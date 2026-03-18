package com.example.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.keycloak")
@Getter
@Setter
public class KeycloakProperties {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String logoutEndpoint;
    private String authorizeEndpoint;
    private String redirectUri;
    private String dashboardUrl;
    private Admin admin = new Admin();

    @Getter
    @Setter
    public static class Admin {
        private String clientId;
        private String username;
        private String password;
    }
}
