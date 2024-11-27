package com.tencent.wxcloudrun.utils;

import lombok.Data;
import org.apache.http.conn.DnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * <p>ProjectName: baiduSEOProject</p>
 * <p>PackageName: com.seo.utils</p>
 * <p>Description: HTTPClient 自定义DNS重写类</p>
 * <p>Copyright: Copyright (c) 2023 by Ts</p>
 * <p>Contacts: Ts vx: Q_Q-1992</p>
 *
 * @Author: Ts
 * @Version: 1.0
 * @Date: 2023-08-03 10:02
 **/
@Data
public class DnsResolverHost implements DnsResolver {
    private Map<String, InetAddress[]> MAPPINGS;



    public DnsResolverHost(Map<String, InetAddress[]> mappings) {
        this.MAPPINGS = mappings;
    }

    public void addResolve(String host, String ip) {
        try {
            MAPPINGS.put(host, new InetAddress[]{InetAddress.getByName(ip)});
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        return (MAPPINGS.containsKey(host) && MAPPINGS.get(host) != null) ? MAPPINGS.get(host) : new InetAddress[0];
    }

}
