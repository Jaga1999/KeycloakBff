package com.example.bff.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_BFF_SESSION = "BffSession";

    @Bean
    public OpenAPI bffOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Keycloak BFF API")
                        .description("Backend-for-Frontend API secured via Keycloak and HTTP-only session cookie.")
                        .version("v1")
                        .contact(new Contact().name("BFF").email("noreply@example.com"))
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Keycloak Documentation")
                        .url("https://www.keycloak.org/documentation"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_BFF_SESSION))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_BFF_SESSION, new SecurityScheme()
                                .name("SESSION_ID")
                                .description("BFF session cookie (HttpOnly)")
                                .in(SecurityScheme.In.COOKIE)
                                .type(SecurityScheme.Type.APIKEY)));
    }
}

