package com.telecom.ocs.provisioning.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for OCS Provisioning Service.
 * Customizes Springdoc OpenAPI documentation generation.
 * Implements Constitution Principle I: API Contract First.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:ocs-provisioning-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${openapi.prod-url:}")
    private String prodUrl;

    /**
     * Configures OpenAPI documentation metadata.
     * This supplements the existing OpenAPI spec at app-spec/ocs-provisioing-api.yml
     * with runtime server information and versioning.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("OCS Provisioning Service API")
                        .version("1.0.0")
                        .description("RESTful API for telecom subscriber provisioning in online charging systems. " +
                                "Manages subscribers, subscriptions, balances, groups, notifications, timers, and account history.")
                        .contact(new Contact()
                                .name("OCS Provisioning Team")
                                .email("ocs-provisioning@telecom.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://telecom.com/licenses")));

        // Add development server
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Development environment");
        openAPI.addServersItem(devServer);

        // Add production server if configured
        if (prodUrl != null && !prodUrl.isEmpty()) {
            Server prodServer = new Server();
            prodServer.setUrl(prodUrl);
            prodServer.setDescription("Production environment");
            openAPI.addServersItem(prodServer);
        }

        return openAPI;
    }
}
