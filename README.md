## 1.介绍
[Oracle Access Manager(OAM)](https://www.oracle.com/technetwork/middleware/id-mgmt/overview/index-090417.html)是oracle公司开发的身份认证和资源管理解决方案。结合```WebGate```和```OHS```可实现系统间单点登录集成。

oracle中间件产品可以非常方便的与OAM进行集成,通过配置```weblogic```安全域(Security Realms)，应用几乎不用做任何改动即可实现单点登录。

后台接口开发中获取当前登录用户是不可避免的一个步骤，spring boot也是如此，本文介绍spring boot与OAM集成的一个可行并且认为是简单的方案。

## 2.实现
### 2.1 问题
要实现两者之间的集成需要解决两个问题:
1. spring boot是无状态应用，如何获取当前用户信息
2. spring boot不是部署weblogic上，无法通过weblogic 安全域进行单点登录集成(当然spring boot也可以部署到weglogic上，这个不是本文讨论的方向)

### 2.2 思路
1. 通过ohs代理，spring boot可以获取cookie等信息
2. 调用OAM SDK通过cookie获取当前用户名

只要实现以上两点就能实现spring boot 在脱离weblogic的情况下也能获取用户信息，如果可行，该方案适用于任何后端程序。
### 2.3 准备工作
#### 2.3.1 SDK下载安装
在[这里](https://www.oracle.com/technetwork/middleware/downloads/oid-11g-161194.html)可以下载sdk，下载时请选择对应的OAM版本，本文使用```11.1.1.5.0```版本。

为了使用方便，可以将jar包安装到本地maven仓库
```bash
mvn install:install-file -Dfile="/Users/asan/workspace/mpaas/lib/oamasdk-api.jar" -DgroupId=com.oracle -DartifactId=oamasdk-api -Dversion=11.1.1.5.0 -Dpackaging=jar
```
这样可以通过以下方式在pom中引用
```xml
<dependency>
	<groupId>com.oracle</groupId>
	<artifactId>oamasdk-api</artifactId>
	<version>11.1.1.5.0</version>
</dependency>
```
#### 2.3.2 配置webgate
登录oamconsole(一般是http://host:7001/oamconsole),在欢迎界面创建一个新的```OAM 11g Webgate```输入```Name```和```Host Identifier```，这两个默认一样，一定要将```Access Client Password```设置为空，可能是sdk的bug，sdk无法解析密码，错误如下
```txt
2018-10-10 22:46:18 oracle.security.am.asdk.AccessClient initialize
严重: Oracle Access SDK 初始化失败。
oracle.security.am.asdk.AccessException: OAMAGENT-02072: 无法执行encrypt password操作。
	at oracle.security.am.asdk.impl.Configuration.setEncryptedPassword(Configuration.java:321)
	at oracle.security.am.asdk.impl.ConfigXMLHandler.processConfig(ConfigXMLHandler.java:580)
	at oracle.security.am.asdk.impl.ConfigXMLHandler.readConfigurationFromFile(ConfigXMLHandler.java:126)
	at oracle.security.am.asdk.AccessClient.initialize(AccessClient.java:634)
	at oracle.security.am.asdk.AccessClient.<init>(AccessClient.java:553)
	at oracle.security.am.asdk.AccessClient.createDefaultInstance(AccessClient.java:242)
...
```
[官方](https://support.oracle.com/epmos/faces/DocumentDisplay?id=1394989.1)说是sdk版本问题，但笔者试过所有的sdk版本还是无法解决。

![spring-boot-oam-01.png](http://oxuwc5kol.bkt.clouddn.com/spring-boot-oam-01.png)

![spring-boot-oam-01.png](http://oxuwc5kol.bkt.clouddn.com/spring-boot-oam-02.png)

点击右上角```Apply```后可以有系统提示，在提示路径下可以找到```ObAccessClient.xml```文件，这个就是webgate的配置文件，在实际环境配置中，需要将该文件放到```$OHS_HOME/config/webgate/config```目录下，才能实现ohs资源拦截认证。
将```ObAccessClient.xml```文件复制到本地，如放到目录```/you/path/config/ObAccessClient.xml```下。
![spring-boot-oam-01.png](http://oxuwc5kol.bkt.clouddn.com/spring-boot-oam-03.png)
#### 2.3.3 代码实现
直接上代码
```java
import oracle.security.am.asdk.AccessClient;
import oracle.security.am.asdk.AccessException;
import oracle.security.am.asdk.UserSession;

public class OAM {
    //保存ObAccessClient.xml文件目录
    public static final String OAM_CONFIG_PATH = "/you/path/config/";

    public static void main(String[] args) throws AccessException {
        String cookie =
            "Ad/NbCxwjXyRvO15xxJiueMY/utzybeifo6bIhhKOcsvQqQM73fIoE/fKm7zdUeo7FqbDYhP6ncFC/Ntq4yeSAXQeIum9EcaCqAXrUVyvO99ACxDv/2OdX5D/R10a7nuc6nlE54zPVbpm5xkp5H8TjlaM0oeqjDZXOalLcAsV9RnQjLrd9rW3cQi05PVuPalf0jRj+pqCILCHf+sO+cCdcYE2jEybbX3oXVwVGYJQSDDIEm/ne56gZlBKOg56zne2zEp60TkyXgkBG8vL0mNqVGdxd9DmNDeGaAYDoif5EzKfXp7K9QtWbGOjwRSGcTZcLvWcBPJjBhTOBGCLJ3yEg==";
        AccessClient client = null;
        client =
                AccessClient.createDefaultInstance(OAM_CONFIG_PATH, AccessClient.CompatibilityMode.OAM_10G);
        UserSession session = new UserSession(cookie);
        String userName = session.getUserIdentity();
        if (userName != null) {
            userName =
                    userName.substring(userName.indexOf("uid=") + 4, userName.indexOf(","));
        }
        System.out.println("current user==>" + userName);
    }
}
```
```cookie```可以在浏览器上登录系统，F12进入开发者面板，在网络请求里找到cookie，OAM cookie名称为```OAMAuthnCookie```
## 3. spring boot实现
上面代码只是一个demo，跟spring boot也没什么关系，在开发中，我们一般都有个最佳实践，下面介绍一个我认为是较佳的实践方案。在开始之前，建议你阅读下[这篇](https://segmentfault.com/a/1190000016168393)文章，了解一些规范，当然，你也可以跳过，这个不是核心内容。

在demo里有个稍微比较复杂的操作，就是从服务器下载```ObAccessClient.xml```文件，并且保存到本地路径，在代码里引用，而且查看```ObAccessClient.xml```文件可以知道里面的内容大部分可以写死，只有在创建webgate那个界面填的内容是动态的。因此可以考虑将配置文件放入到spring boot的```application.properties```中，然后在程序里动态生成配置文件，避免引入新的配置文件。

***application.properties***
```properties
mpaas.sso.oam.host=xxxx.example.com
mpaas.sso.oam.port=5575
mpaas.sso.oam.hostId=my_webgate_11g
mpaas.sso.oam.id=my_webgate_11g
mpaas.sso.oam.cookieName=OAMAuthnCookie
```
创建配置类保存配置信息

***OAMConfig.java***
```java
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
    //getter and setter method
    ....
}
```
主程序

***OAMSsoPlugin.java***
```java
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
```
测试类
***TestController.java***
```java
import com.definesys.mpaas.common.adapter.IMpaasSSOAuthentication;
import com.definesys.mpaas.common.adapter.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @Copyright: Shanghai Definesys Company.All rights reserved.
 * @Description:
 * @author: jianfeng.zheng
 * @since: 2018/10/10 下午7:23
 * @history: 1.2018/10/10 created by jianfeng.zheng
 */
@RestController
public class TestController {

    @Autowired
    private IMpaasSSOAuthentication auth;

    @RequestMapping(value = "/test")
    public UserProfile test(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        Map<String, String> ck = new HashMap<>();
        for (Cookie k : cookies) {
            ck.put(k.getName(), k.getValue());
        }
        return auth.ssoAuth(null, ck);
    }
}
```
代码可以在[github](https://github.com/mrasan/spring-boot-oam-integrate)上下载查看

#### 配置ohs
需要配置ohs反向代理，在ohs配置文件```mod_wl_ohs.conf```最后添加以下配置项，并重启ohs
```conf
...

ProxyPass /test http://ip:8080/test
```
ip指向运行spring boot的机器ip

通过单点登录地址访问，会先跳转到登录页面，登录完后可访问接口并且返回当前用户信息

### 写在最后
更好的做法是在filter中拦截请求做登录认证，未通过认证的返回```401```通过认证的将用户信息保存在```ThreadLocal```里，这样可以在任意位置获取用户信息。留给读者自行实现。