package com.definesys.mpaas.plugin.oam;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Copyright: Shanghai Definesys Company.All rights reserved.
 * @Description:
 * @author: jianfeng.zheng
 * @since: 2018/10/10 下午5:45
 * @history: 1.2018/10/10 created by jianfeng.zheng
 */
@Component
@ConfigurationProperties(prefix = "mpaas.sso.oam")
public class OAMConfig {
    private String host;     //OAM服务器地址
    private Integer port;    //OAM服务端口(默认是5575)
    private String name;     //webgate名称
    private String hostId;   //webgate唯一标识符
    private String cookieName;  //cookie名称 默认OAMAuthnCookie

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
