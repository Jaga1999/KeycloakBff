package com.example.bff.common.web;

import com.example.bff.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class CookieUtils {

    private final SecurityProperties securityProperties;

    public CookieUtils(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void addSessionCookie(HttpServletResponse response, UUID sessionId, boolean rememberMe) {
        Cookie cookie = new Cookie(securityProperties.getSessionCookieName(), sessionId.toString());
        cookie.setHttpOnly(true);
        cookie.setSecure(securityProperties.isSessionCookieSecure());
        cookie.setPath("/");
        cookie.setAttribute("SameSite", securityProperties.getSessionCookieSameSite());

        if (rememberMe) {
            cookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        } else {
            cookie.setMaxAge(-1); // session cookie
        }

        response.addCookie(cookie);
        log.debug("Set-Cookie header added for session: {}", sessionId);
    }

    public void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(securityProperties.getSessionCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(securityProperties.isSessionCookieSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", securityProperties.getSessionCookieSameSite());
        
        response.addCookie(cookie);
        log.debug("Session cookie cleared");
    }
}
