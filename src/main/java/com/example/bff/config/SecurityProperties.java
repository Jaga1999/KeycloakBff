package com.example.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    private String sessionCookieName = "SESSION_ID";
    private boolean sessionCookieSecure = true;
    private String sessionCookieSameSite = "Strict";
    private List<String> publicPaths = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html"
    );
}
