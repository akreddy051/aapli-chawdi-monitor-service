package com.example.aapliChawdi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI aapliChawdiOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("AdminApiKey", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-API-Key"))
                        .addSecuritySchemes("TelegramWebhookSecret", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-Telegram-Bot-Api-Secret-Token")))
                .addSecurityItem(new SecurityRequirement().addList("AdminApiKey"))
                .info(new Info().title("Aapli Chawdi API").version("0.0.1-SNAPSHOT")
                        .description("Manage village subscriptions, collect land mutation notices, "
                                + "and receive Telegram updates. Scraping operations run synchronously "
                                + "and may take several minutes. Notice triggers can send Telegram alerts."))
                .servers(List.of(new Server().url("/").description("Current service")));
    }
}
