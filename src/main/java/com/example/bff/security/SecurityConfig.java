package com.example.bff.security;

import com.example.bff.auth.service.SessionService;
import com.example.bff.config.SecurityProperties;
import com.example.bff.infrastructure.keycloak.KeycloakAuthClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public SessionAuthenticationFilter sessionAuthenticationFilter(
            SessionService sessionService,
            KeycloakAuthClient keycloakAuthClient
    ) {
        return new SessionAuthenticationFilter(
                sessionService,
                keycloakAuthClient,
                securityProperties.getSessionCookieName(),
                securityProperties.getPublicPaths()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionAuthenticationFilter sessionAuthenticationFilter) {
        try {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(securityProperties.getPublicPaths().toArray(new String[0])).permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build security filter chain", e);
        }
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return _ -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Not used");
        };
    }
}
