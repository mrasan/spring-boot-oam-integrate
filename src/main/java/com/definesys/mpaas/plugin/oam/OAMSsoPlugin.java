package com.definesys.mpaas.plugin.oam;

import com.definesys.mpaas.common.adapter.IMpaasSSOAuthentication;
import com.definesys.mpaas.common.adapter.UserProfile;
import com.definesys.mpaas.common.exception.MpaasBusinessException;
import com.definesys.mpaas.common.exception.MpaasRuntimeException;
import oracle.security.am.asdk.AccessClient;
import oracle.security.am.asdk.AccessException;
import oracle.security.am.asdk.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

/**
 * @Copyright: Shanghai Definesys Company.All rights reserved.
 * @Description:
 * @author: jianfeng.zheng
 * @since: 2018/10/10 下午2:46
 * @history: 1.2018/10/10 created by jianfeng.zheng
 */
@Component
public class OAMSsoPlugin implements IMpaasSSOAuthentication {
    @Autowired
    private OAMConfig config;
    private static AccessClient accessClient;

    //ObAccessClient.xml模板
    public static final String OB_ACCESS_CLIENT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><CompoundList xmlns=\"http://www.oblix.com\"><SimpleList><NameValPair ParamName=\"id\" Value=\"%s\"/></SimpleList><SimpleList><NameValPair ParamName=\"debug\" Value=\"false\"/></SimpleList><SimpleList><NameValPair ParamName=\"security\" Value=\"open\"/></SimpleList><SimpleList><NameValPair ParamName=\"state\" Value=\"Enabled\"/></SimpleList><SimpleList><NameValPair ParamName=\"primaryCookieDomain\" Value=\"\"/></SimpleList><SimpleList><NameValPair ParamName=\"preferredHost\" Value=\"%s\"/></SimpleList><SimpleList><NameValPair ParamName=\"maxCacheElems\" Value=\"100000\"/></SimpleList><SimpleList><NameValPair ParamName=\"cacheTimeout\" Value=\"1800\"/></SimpleList><SimpleList><NameValPair ParamName=\"cookieSessionTime\" Value=\"0\"/></SimpleList><SimpleList><NameValPair ParamName=\"maxSessionTime\" Value=\"36000\"/></SimpleList><SimpleList><NameValPair ParamName=\"maxConnections\" Value=\"1\"/></SimpleList><SimpleList><NameValPair ParamName=\"idleSessionTimeout\" Value=\"0\"/></SimpleList><SimpleList><NameValPair ParamName=\"failoverThreshold\" Value=\"1\"/></SimpleList><SimpleList><NameValPair ParamName=\"aaaTimeoutThreshold\" Value=\"-1\"/></SimpleList><SimpleList><NameValPair ParamName=\"sleepFor\" Value=\"60\"/></SimpleList><SimpleList><NameValPair ParamName=\"denyOnNotProtected\" Value=\"true\"/></SimpleList><SimpleList><NameValPair ParamName=\"cachePragmaHeader\" Value=\"no-cache\"/></SimpleList><SimpleList><NameValPair ParamName=\"cacheControlHeader\" Value=\"no-cache\"/></SimpleList><SimpleList><NameValPair ParamName=\"ipValidation\" Value=\"false\"/></SimpleList><SimpleList><NameValPair ParamName=\"accessClientPasswd\" Value=\"\"/></SimpleList><ValList ListName=\"primary_server_list\"><ValListMember Value=\"primaryServer1\"/></ValList><ValNameList ListName=\"primaryServer1\"><NameValPair ParamName=\"host\" Value=\"%s\"/><NameValPair ParamName=\"port\" Value=\"%s\"/><NameValPair ParamName=\"numOfConnections\" Value=\"1\"/></ValNameList><ValList ListName=\"proxySSLHeaderVar\"><ValListMember Value=\"IS_SSL\"/></ValList><ValList ListName=\"URLInUTF8Format\"><ValListMember Value=\"true\"/></ValList><ValList ListName=\"client_request_retry_attempts\"><ValListMember Value=\"1\"/></ValList><ValList ListName=\"inactiveReconfigPeriod\"><ValListMember Value=\"10\"/></ValList></CompoundList>";

    /**
     * oam验证
     *
     * @param header
     * @param cookies
     * @return
     * @throws MpaasBusinessException
     */
    @Override
    public UserProfile ssoAuth(Map<String, String> header, Map<String, String> cookies) throws MpaasBusinessException {
        String path = configPath();
        //AccessClient只要初始化一次就行
        if (accessClient == null) {
            try {
                accessClient =
                        AccessClient.createDefaultInstance(path, AccessClient.CompatibilityMode.OAM_10G);
            } catch (AccessException e) {
                throw new MpaasRuntimeException(e);
            }
        }
        String cookie = parseCookie(cookies);
        String name =
                getUserNameFromToken(cookie);
        UserProfile user = new UserProfile();
        user.setUid(name);
        user.setToken(cookie);
        return user;
    }

    /**
     * 生成配置文件
     *
     * @return
     */
    private String configPath() {
        String tmp = System.getProperty("java.io.tmpdir");
        String path = tmp + "/mpaas-sso-oam-config";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File ob = new File(dir + "/ObAccessClient.xml");
        String xml = null;
        if (!ob.exists()) {
            xml = String.format(OB_ACCESS_CLIENT_TEMPLATE, config.getHostId(), config.getName(), config.getHost(), String.valueOf(config.getPort()));
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(ob);
                fout.write(xml.getBytes("utf-8"));
                fout.close();
            } catch (Exception e) {
                throw new MpaasRuntimeException(e);
            }
        }
        return path;
    }

    /**
     * 从cookie中获取用户名(uid)
     *
     * @param sessionToken
     * @return
     */
    public static String getUserNameFromToken(String sessionToken) {
        UserSession session = null;
        String userName = null;
        try {
            session = new UserSession(sessionToken);
            userName = session.getUserIdentity();
        } catch (AccessException e) {
            throw new MpaasRuntimeException(e);
        }

        if (userName != null) {
            userName =
                    userName.substring(userName.indexOf("uid=") + 4, userName.indexOf(","));
        }
        return userName;
    }

    /**
     * 解析cookie
     * 需要设置mpaas.sso.oam.cookieName
     * 如果没有设置默认获取包含OAMAUTHNCOOKIE的cookie
     *
     * @param cookies
     * @return
     */
    private String parseCookie(Map<String, String> cookies) {
        if (config.getCookieName() != null) {
            return cookies.get(config.getCookieName());
        }
        for (String k : cookies.keySet()) {
            if (k.toUpperCase().contains("OAMAUTHNCOOKIE")) {
                return cookies.get(k);
            }
        }
        throw new MpaasRuntimeException("no config cookie name");
    }
}
