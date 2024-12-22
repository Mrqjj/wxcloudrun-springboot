package com.tencent.wxcloudrun.controller;

import com.alibaba.fastjson.JSONObject;
import com.tencent.wxcloudrun.dto.BodyRequest;
import com.tencent.wxcloudrun.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.CounterRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * counter控制器
 */
@RestController

public class CounterController {

    public static String accessToken = "";
    private static long expiresTime = 0;


    final CounterService counterService;
    final Logger logger;

    public CounterController(@Autowired CounterService counterService) {
        this.counterService = counterService;
        this.logger = LoggerFactory.getLogger(CounterController.class);
    }


    /**
     * 获取当前计数
     *
     * @return API response json
     */
    @GetMapping(value = "/api/count")
    ApiResponse get() {
        logger.info("/api/count get request");
        Optional<Counter> counter = counterService.getCounter(1);
        Integer count = 0;
        if (counter.isPresent()) {
            count = counter.get().getCount();
        }

        return ApiResponse.ok(count);
    }


    /**
     * 更新计数，自增或者清零
     *
     * @param request {@link CounterRequest}
     * @return API response json
     */
    @PostMapping(value = "/api/count")
    ApiResponse create(HttpServletRequest httpReq, @RequestBody CounterRequest request) {
        logger.info("/api/count post request, action: {}", request.getAction());
        Enumeration<String> headerNames = httpReq.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            logger.info("requestHeader key: {}, value: {}", headerName, httpReq.getHeader(headerName));
        }

        Optional<Counter> curCounter = counterService.getCounter(1);
        if (request.getAction().equals("inc")) {
            Integer count = 1;
            if (curCounter.isPresent()) {
                count += curCounter.get().getCount();
            }
            Counter counter = new Counter();
            counter.setId(1);
            counter.setCount(count);
            counterService.upsertCount(counter);
            return ApiResponse.ok(count);
        } else if (request.getAction().equals("clear")) {
            if (!curCounter.isPresent()) {
                return ApiResponse.ok(0);
            }
            counterService.clearCount(1);
            return ApiResponse.ok(0);
        } else {
            return ApiResponse.error("参数action错误");
        }
    }


    @PostMapping(value = "/api/getSessionKey")
    ApiResponse getSessionKey(HttpServletRequest httpReq, @RequestBody BodyRequest request) {
        String jsCode = request.getJsCode();
        String appId = "wx8b4b0fa894795915";
        String secret = "c4aea564db0adf169bff47e1f52d2eec";
        String grantType = "authorization_code";
        String url = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=%s", appId, secret, jsCode, grantType);
        byte[] res = HttpUtils.sendGetRequest(url, null);
        return ApiResponse.ok(new String(res));
    }


    @PostMapping(value = "/api/decryptInfo")
    ApiResponse decryptInfo(HttpServletRequest httpReq, @RequestBody BodyRequest request) {
        String iv = request.getIv();
        String key = request.getSessionKey();
        String data = request.getEncryptedData();

        return decrypt(data, key, iv);
    }


    @PostMapping(value = "/api/getUserPhoneInfo")
    ApiResponse getUserPhoneInfo(HttpServletRequest httpReq, @RequestBody BodyRequest request) {
        String code = request.getJsCode();
        if (expiresTime < System.currentTimeMillis()) {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=wx8b4b0fa894795915&secret=c4aea564db0adf169bff47e1f52d2eec";
            byte[] res = HttpUtils.sendGetRequest(url, null);
            String result = new String(res, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONObject.parseObject(result);
            accessToken = jsonObject.getString("access_token");
            expiresTime = System.currentTimeMillis() + (jsonObject.getLong("expires_in") * 1000) - 60 * 1000 * 5;
        }
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
        JSONObject body = new JSONObject();
        body.put("code", code);
        HashMap headerMap = new HashMap();
        headerMap.put("Content-Type", "application/json");
        byte[] res = HttpUtils.sendPostRequest(url, body.toJSONString(), headerMap);
        String result = new String(res, StandardCharsets.UTF_8);
        return ApiResponse.ok(JSONObject.parseObject(result));
    }


    @PostMapping(value = "/api/getUserRiskRank")
    ApiResponse getUserRiskRank(HttpServletRequest httpReq, @RequestBody BodyRequest request) {
        if (expiresTime < System.currentTimeMillis()) {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=wx8b4b0fa894795915&secret=c4aea564db0adf169bff47e1f52d2eec";
            byte[] res = HttpUtils.sendGetRequest(url, null);
            String result = new String(res, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONObject.parseObject(result);
            accessToken = jsonObject.getString("access_token");
            expiresTime = System.currentTimeMillis() + (jsonObject.getLong("expires_in") * 1000) - 60 * 1000 * 5;
        }
        String url = "https://api.weixin.qq.com/wxa/getuserriskrank?access_token=" + accessToken;
        JSONObject body = new JSONObject();
        body.put("appid", "wx8b4b0fa894795915");
        body.put("openid", httpReq.getHeader("x-wx-openid"));
        body.put("scene", 0);
        body.put("client_ip", httpReq.getHeader("x-original-forwarded-for"));
        HashMap<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("Content-Type", "application/json");
        logger.info("getUserRiskRank requestBody : {}", body.toJSONString());
        byte[] res = HttpUtils.sendPostRequest(url, body, headerMap);
        String result = new String(res, StandardCharsets.UTF_8);
        return ApiResponse.ok(JSONObject.parseObject(result));
    }

    public static ApiResponse decrypt(String encryptedData, String key, String iv) {
        // 转换密钥和IV为字节数组
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8));
            byte[] ivBytes = Base64.getDecoder().decode(iv.getBytes(StandardCharsets.UTF_8));
            // Base64 解码密文
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedData);
            // 初始化 AES 密钥和 IV
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            // 配置解密器
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // PKCS5 等效于 PKCS7
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            // 执行解密
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            // 返回解密后的明文字符串
            return new ApiResponse(0, "", new String(plainBytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new ApiResponse(500, e.getMessage(), null);
        }
    }

}