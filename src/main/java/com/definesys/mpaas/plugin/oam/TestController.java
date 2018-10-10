package com.definesys.mpaas.plugin.oam;

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
