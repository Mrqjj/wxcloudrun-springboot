package com.tencent.wxcloudrun.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * <p>ProjectName: wxcloudrun-springboot</p>
 * <p>PackageName: com.tencent.wxcloudrun.dto</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2023 by Ts</p>
 * <p>Contacts: Ts vx: Q_Q-1992</p>
 *
 * @Author: Ts
 * @Version: 1.0
 * @Date: 2024-11-28 05:18
 **/
@Data
public class BodyRequest {

    private String jsCode;

    private String cloudID;

    private String iv;

    private String rawData;

    private String encryptedData;

    private String sessionKey;

    private JSONObject userInfo;
}
