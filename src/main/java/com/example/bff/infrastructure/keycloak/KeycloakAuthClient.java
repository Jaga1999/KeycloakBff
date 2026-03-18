package com.example.bff.infrastructure.keycloak;

import com.example.bff.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAuthClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Object>> LIST_TYPE = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public Mono<Map<String, Object>> login(String username, String password) {
        log.debug("Keycloak login request for user: {}", username);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("username", username);
        formData.add("password", password);

        return Mono.fromCallable(() -> restClient.post()
                .uri(properties.getServerUrl() + properties.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Mono<Map<String, Object>> refreshToken(String refreshToken) {
        log.debug("Keycloak refresh token request");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("refresh_token", refreshToken);

        return Mono.fromCallable(() -> {
            try {
                return restClient.post()
                        .uri(properties.getServerUrl() + properties.getTokenEndpoint())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formData)
                        .retrieve()
                        .body(MAP_TYPE);
            } catch (Exception e) {
                log.error("Error refreshing token from Keycloak: {}", e.getMessage());
                return null;
            }
        });
    }

    public Mono<Map<String, Object>> exchangeCode(String code) {
        log.debug("Keycloak exchange code request");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("code", code);
        formData.add("redirect_uri", properties.getRedirectUri());

        return Mono.fromCallable(() -> restClient.post()
                .uri(properties.getServerUrl() + properties.getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Mono<Void> logout(String refreshToken) {
        log.debug("Keycloak logout request");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("refresh_token", refreshToken);

        return Mono.fromRunnable(() -> restClient.post()
                .uri(properties.getServerUrl() + properties.getLogoutEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity());
    }

    public Mono<String> obtainAdminAccessToken() {
        log.trace("Obtaining Keycloak admin access token");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", properties.getAdmin().getClientId());
        formData.add("username", properties.getAdmin().getUsername());
        formData.add("password", properties.getAdmin().getPassword());

        return Mono.fromCallable(() -> {
            Map<String, Object> body = restClient.post()
                    .uri(properties.getServerUrl() + "/realms/master/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(MAP_TYPE);
            return body != null ? (String) body.get("access_token") : null;
        });
    }

    public Mono<Boolean> userExistsByUsernameOrEmail(String username, String email, String adminToken) {
        log.debug("Checking if user exists: {} or {}", username, email);
        return Mono.fromCallable(() -> {
            String url = UriComponentsBuilder.fromUriString(properties.getServerUrl())
                    .path("/admin/realms/{realm}/users")
                    .queryParam("username", username)
                    .queryParam("exact", true)
                    .buildAndExpand(properties.getRealm())
                    .toUriString();

            List<Object> list = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(LIST_TYPE);
            
            if (list != null && !list.isEmpty()) return true;

            String emailUrl = UriComponentsBuilder.fromUriString(properties.getServerUrl())
                    .path("/admin/realms/{realm}/users")
                    .queryParam("email", email)
                    .queryParam("exact", true)
                    .buildAndExpand(properties.getRealm())
                    .toUriString();

            List<Object> emailList = restClient.get()
                    .uri(emailUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(LIST_TYPE);
            
            return emailList != null && !emailList.isEmpty();
        }).onErrorReturn(false);
    }

    public Mono<String> createUser(String username, String email, String firstName, String lastName, String password, String adminToken) {
        log.info("Creating user in Keycloak: {}", username);
        Map<String, Object> payload = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", Boolean.TRUE,
                "emailVerified", Boolean.FALSE,
                "credentials", List.of(
                        Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", Boolean.FALSE
                        )
                )
        );

        return Mono.fromCallable(() -> restClient.post()
                .uri(properties.getServerUrl() + "/admin/realms/{realm}/users", properties.getRealm())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange((_, response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
                        log.debug("User created successfully, location: {}", location);
                        return Optional.ofNullable(location)
                                .map(URI::create)
                                .map(uri -> {
                                    String path = uri.getPath();
                                    int lastSlash = path.lastIndexOf('/');
                                    return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
                                }).orElse(null);
                    }
                    log.error("Failed to create user in Keycloak, status: {}", response.getStatusCode());
                    throw new RuntimeException("Failed to create user: " + response.getStatusCode());
                }));
    }

    public Mono<String> findUserIdByEmail(String email, String adminToken) {
        log.debug("Finding Keycloak user ID for email: {}", email);
        return Mono.fromCallable(() -> {
            String url = UriComponentsBuilder.fromUriString(properties.getServerUrl())
                    .path("/admin/realms/{realm}/users")
                    .queryParam("email", email)
                    .queryParam("exact", true)
                    .buildAndExpand(properties.getRealm())
                    .toUriString();

            List<Object> list = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(LIST_TYPE);

            if (list == null || list.isEmpty()) {
                return null;
            }
            Object first = list.getFirst();
            if (first instanceof Map<?, ?> map && map.get("id") instanceof String id) {
                return id;
            }
            return null;
        });
    }

    public Mono<Void> triggerResetPasswordEmail(String userId, String adminToken) {
        log.info("Triggering reset password email for Keycloak user: {}", userId);
        return Mono.fromRunnable(() -> restClient.put()
                .uri(properties.getServerUrl() + "/admin/realms/{realm}/users/{id}/execute-actions-email", properties.getRealm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of("UPDATE_PASSWORD"))
                .retrieve()
                .toBodilessEntity());
    }
}
