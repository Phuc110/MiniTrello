package com.minitrello.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI setup. Registers the "bearerAuth" scheme so every
 * protected endpoint documented with @SecurityRequirement("bearerAuth")
 * shows the padlock + token field in Swagger UI, letting reviewers test
 * authenticated endpoints directly from the docs.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI miniTrelloOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Trello Enterprise API")
                        .description("Enterprise Project Management Platform")
                        .version("v0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
