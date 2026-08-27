package com.kleos.transfers.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SpringDoc OpenAPI metadata for the versioned public API.
 */
@Configuration
@Profile("!prod")
public class OpenApiConfig {

    @Bean
    public OpenAPI kleosOpenApi(
            @Value("${kleos.application.name}") String applicationName,
            @Value("${kleos.application.version}") String applicationVersion
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .description("Versioned HTTP API for identity, historical, and prediction resources.")
                        .version(applicationVersion)
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
