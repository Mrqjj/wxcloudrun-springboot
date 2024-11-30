package com.tencent.wxcloudrun.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>ProjectName: baiduSEOProject</p>
 * <p>PackageName: com.seo.utils</p>
 * <p>Description: 网络请求工具类</p>
 * <p>Copyright: Copyright (c) 2023 by Ts</p>
 * <p>Contacts: Ts vx: Q_Q-1992</p>
 *
 * @Author: Ts
 * @Version: 1.0
 * @Date: 2023-07-24 21:36
 **/
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    private static final Object lock = new Object();
    //重尝试次数
    private static final int requestTryAgain = 3;

    /**
     * 发送get 请求
     *
     * @param url       请求地址
     * @param headerMap 请求头
     * @return
     */
    public static byte[] sendGetRequest(String url, Map<String, String> headerMap) {
        return sendGetRequest(url, headerMap, null, null);
    }


    /**
     * 发送get 请求
     *
     * @param url       请求地址
     * @param headerMap 请求头
     * @param proxy     代理信息
     * @return
     */
    public static byte[] sendGetRequest(String url, Map<String, String> headerMap, InetSocketAddress proxy) {
        return sendGetRequest(url, headerMap, null, proxy);
    }

    /**
     * 发送get 请求
     *
     * @param url       请求地址
     * @param dnsMap    DNS存储对象
     * @param headerMap 请求头
     * @param proxy     代理信息
     * @return 返回服务器响应数据
     */
    public static byte[] sendGetRequest(String url, Map<String, String> headerMap, Map<String, Object> dnsMap, InetSocketAddress proxy) {
        HttpGet httpGet = new HttpGet(url);
        if (headerMap != null) {
            // 写入headers
            for (Map.Entry entry : headerMap.entrySet()) {
                httpGet.setHeader(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        CloseableHttpClient httpClient;
        HttpResponse response;
        //重尝试请求
        for (int i = 0; i < requestTryAgain; i++) {
            try {
                //获取dns的ip配置信息
                String targetServerIP = null;
                if (dnsMap != null) {
                    JSONObject o = (JSONObject) dnsMap.get(new URL(url).getHost());
                    targetServerIP = o != null ? o.getJSONArray("ip").get(0).toString() : null;
                }
                httpClient = getSslHttpClient(new URL(url).getHost(), targetServerIP);
                //超时时间设置
                RequestConfig.Builder requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(10000).setConnectionRequestTimeout(10000);

                if (proxy != null) {
                    HttpClientContext context = HttpClientContext.create();
//                    context.setAttribute("socks.address", proxy);
                    HttpHost httpHost = new HttpHost(proxy.getHostName(), proxy.getPort());
                    requestConfig.setProxy(httpHost);
                    httpGet.setConfig(requestConfig.build());
                    // 发送GET请求，获取响应
                    response = httpClient.execute(httpGet, context);
                } else {
                    httpGet.setConfig(requestConfig.build());
                    HttpClientContext context = HttpClientContext.create();
                    response = httpClient.execute(httpGet, context);
                }
                // 从响应中获取响应体
                HttpEntity entity = response.getEntity();
                byte[] result = EntityUtils.toByteArray(entity);
//                logger.info(String.format("发送GET请求:[%s],响应内容:[%s]", url, new String(result)));
                return result;
            } catch (IOException exception) {
                return null;
            }
        }
        return null;
    }

    /**
     * 发送 head 请求
     *
     * @param url       请求地址
     * @param dnsMap    dns信息
     * @param headerMap 头信息
     * @param proxy     代理信息
     * @return 返回响应头标识
     * @throws Exception 抛出异常
     */
    public static JSONObject sendHeadRequest(String url, Map<String, String> headerMap, Map<String, Object> dnsMap, InetSocketAddress proxy) {
        HttpHead httpHead = new HttpHead(url);
        if (headerMap != null) {
            // 写入headers
            for (Map.Entry entry : headerMap.entrySet()) {
                httpHead.setHeader(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        CloseableHttpClient httpClient;
        HttpResponse response;
        //获取dns的ip配置信息
        String targetServerIP = null;
        //超时时间设置
        httpHead.setConfig(RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(3000).setConnectionRequestTimeout(3000).build());
        //重尝试请求
        for (int i = 0; i < requestTryAgain; i++) {
            try {
                if (dnsMap != null) {
                    JSONObject o = (JSONObject) dnsMap.get(new URL(url).getHost());
                    targetServerIP = o != null ? o.getJSONArray("ip").get(0).toString() : null;
                }
                httpClient = getSslHttpClient(new URL(url).getHost(), targetServerIP);
                if (proxy != null) {
                    HttpClientContext context = HttpClientContext.create();
                    context.setAttribute("socks.address", proxy);
                    // 发送GET请求，获取响应
                    response = httpClient.execute(httpHead, context);
                } else {
                    HttpClientContext context = HttpClientContext.create();
                    response = httpClient.execute(httpHead, context);
                }
                JSONObject jsonObject = new JSONObject();
                for (Header header : response.getAllHeaders()) {
                    jsonObject.put(header.getName(), header.getValue());
                }
                return jsonObject;
            } catch (IOException ignored) {
                ignored.printStackTrace();
                logger.info(String.format("发送HEAD请求:[%s], 出现异常IOException, 异常信息:[%s]", url, ignored.getMessage()));
            }
        }
        return null;
    }

    /**
     * 发送post 请求
     *
     * @param url       地址
     * @param headerMap 头部信息
     * @param body      请求体
     * @return 返回响应体
     */
    public static byte[] sendPostRequest(String url, Object body, Map<String, String> headerMap) {
        return sendPostRequest(url, body, headerMap, null);
    }


    /**
     * 发送post 请求
     *
     * @param url       地址
     * @param headerMap 头部信息
     * @param body      请求体
     * @param proxy     代理信息
     * @return
     */
    public static byte[] sendPostRequest(String url, Object body, Map<String, String> headerMap, InetSocketAddress proxy) {
        return sendPostRequest(url, body, headerMap, null, proxy);
    }

    /**
     * 发送post 请求
     *
     * @param url       请求地址
     * @param dnsMap    dns配置map
     * @param headerMap 请求头
     * @param body      请求体
     * @param proxy     代理信息
     * @return 返回目标响应体
     */
    public static byte[] sendPostRequest(String url, Object body, Map<String, String> headerMap, Map<String, Object> dnsMap, InetSocketAddress proxy) {
        HttpPost httpPost = new HttpPost(url);
        if (headerMap != null) {
            for (Map.Entry entry : headerMap.entrySet()) {
                httpPost.setHeader(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        //超时时间设置
        httpPost.setConfig(RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(3000).setConnectionRequestTimeout(3000).build());
        //重尝试请求
        for (int i = 0; i < requestTryAgain; i++) {
            try {
                // 根数参数的数据类型来构建不同的body对象
                if (body != null) {
                    if (body instanceof byte[]) {
                        HttpEntity entity = new ByteArrayEntity((byte[]) body);
                        httpPost.setEntity(entity);

                    } else if (body instanceof String) {
                        HttpEntity entity = new StringEntity(body.toString());
                        httpPost.setEntity(entity);
                    }else if(body instanceof JSONObject){
                        HttpEntity entity = new StringEntity(((JSONObject) body).toJSONString());
                        httpPost.setEntity(entity);
                    }
                }

                //获取dns的ip配置信息
                String targetServerIP = null;
                if (dnsMap != null) {
                    JSONObject o = (JSONObject) dnsMap.get(new URL(url).getHost());
                    targetServerIP = o != null ? o.getJSONArray("ip").get(0).toString() : null;
                }

                HttpResponse response;
                CloseableHttpClient httpClient = getSslHttpClient(new URL(url).getHost(), targetServerIP);
                //超时时间设置
                RequestConfig.Builder requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(10000).setConnectionRequestTimeout(10000);
                if (proxy != null) {
                    HttpClientContext context = HttpClientContext.create();
//                    context.setAttribute("socks.address", proxy);
                    HttpHost httpHost = new HttpHost(proxy.getHostName(), proxy.getPort());
                    requestConfig.setProxy(httpHost);
                    httpPost.setConfig(requestConfig.build());
                    // 发送POST请求，获取响应
                    response = httpClient.execute(httpPost, context);
                } else {
                    httpPost.setConfig(requestConfig.build());
                    HttpClientContext context = HttpClientContext.create();
                    response = httpClient.execute(httpPost, context);
                }

                // 从响应中获取响应体
                HttpEntity entity = response.getEntity();
                byte[] result = EntityUtils.toByteArray(entity);
                if (result == null) {
                    System.out.println(result);
                }
                // 将响应体原始byte数组返回
                return result;
            } catch (IOException ignored) {
                ignored.printStackTrace();
                logger.info(String.format("发送POST请求:[%s], 出现异常IOException, 异常信息:[%s]", url, ignored.getMessage()));
            }
        }
        return null;
    }

    /**
     * 设置DNS核心方法
     *
     * @param host 请求域名
     * @param ip   设置ip地址
     * @return SSLHTTPClient 对象
     */
    public static CloseableHttpClient getSslHttpClient(String host, String ip) {
        CloseableHttpClient httpClient = null;
        synchronized (lock) {
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().
                    register("http",
                            new MyConnectionSocketFactory()).
                    register("https", new MySSLConnectionSocketFactory(SSLContexts.createDefault())).build();
            BasicHttpClientConnectionManager connectionManager;
            if (ip != null) {
                //核心：1. 创建MyDnsResolver对象
                DnsResolverHost myDnsResolver = new DnsResolverHost(new HashMap<>());
                //核心：2. 设置DNS
                myDnsResolver.addResolve(host, ip);
                //核心：3. 创建BasicHttpClientConnectionManager对象，指定MyDnsResolver
                connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry, null, null, myDnsResolver);
            } else {
                connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);
            }
            HttpClientBuilder httpClientBuilder = HttpClients.custom();
//            if (cookieStore != null) {
//                httpClientBuilder.setDefaultCookieStore(cookieStore);
//            }
            httpClientBuilder.setConnectionManager(connectionManager);
//            if (false) {
//                InetSocketAddress proxy = ProxyUtils.getTunnelProxy();
                //代理需要鉴权
//                HttpHost httpHost = new HttpHost(proxy.getHostName(), proxy.getPort());
//                String namePass = proxy.getHostName().split("@")[0];
//                String userName = namePass.split(":")[0];
//                String passWord = namePass.split(":")[1];
                // 创建凭据提供程序并设置用户名和密码
//                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//                credentialsProvider.setCredentials(new AuthScope(httpHost),
//                        new UsernamePasswordCredentials(userName, passWord));
//                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//            }
            httpClient = httpClientBuilder.build();
        }

        return httpClient;
    }

    /**
     * 自定义 socketFactory
     */
    static class MyConnectionSocketFactory extends PlainConnectionSocketFactory {
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            if (context.getAttribute("socks.address") != null) {
                InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr); //Proxy.Type.SOCKS  Proxy.Type.HTTP
                return new Socket(proxy);
            }
            return new Socket();
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                    InetSocketAddress localAddress, HttpContext context) throws IOException {
            return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
    }

    /**
     * 自定义sslSocketFactory
     */
    static class MySSLConnectionSocketFactory extends SSLConnectionSocketFactory {

        public MySSLConnectionSocketFactory(final SSLContext sslContext) {
            // You may need this verifier if target site's certificate is not secure
            super(sslContext, ALLOW_ALL_HOSTNAME_VERIFIER);
            //忽略证书校验 抓包使用
            try {
                final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }};
                sslContext.init(null, trustAllCerts, new SecureRandom());
            } catch (Exception e) {

            }
        }

        @Override
        public Socket createSocket(final HttpContext context) throws IOException {


            if (context.getAttribute("socks.address") != null) {
                InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
                Proxy proxy = new Proxy(Proxy.Type.HTTP, socksaddr); //Proxy.Type.SOCKS
                return new Socket(proxy);
            }
            return new Socket();
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
                                    InetSocketAddress localAddress, HttpContext context) throws IOException {
            return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
    }
}
