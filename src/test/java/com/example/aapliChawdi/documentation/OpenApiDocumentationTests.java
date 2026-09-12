package com.example.aapliChawdi.documentation;

import com.example.aapliChawdi.config.OpenApiConfiguration;
import com.example.aapliChawdi.security.AuthenticationSecrets;
import com.example.aapliChawdi.security.SecurityConfiguration;
import com.example.aapliChawdi.controller.*;
import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import com.example.aapliChawdi.scraper.VillageDataScraper;
import com.example.aapliChawdi.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = OpenApiDocumentationTests.DocumentationApplication.class,
        properties = {"security.admin-api-key=admin-test-key-012345678901234567890123456789",
                "security.telegram-webhook-secret=webhook-test-key-012345678901234567890123456789",
                "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true",
                "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class OpenApiDocumentationTests {
    static final String ADMIN_KEY = "admin-test-key-012345678901234567890123456789";
    static final String WEBHOOK_SECRET = "webhook-test-key-012345678901234567890123456789";
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({SecurityConfiguration.class, AuthenticationSecrets.class, OpenApiConfiguration.class, NoticeController.class, SubscriptionController.class,
            VillageDataController.class, TelegramWebhookController.class})
    static class DocumentationApplication { }

    @MockitoBean NoticeOrchestrationService orchestrationService;
    @MockitoBean VillageRepositoryInterface villageRepository;
    @MockitoBean SubscriptionServiceInterface subscriptionService;
    @MockitoBean VillageDataScraper villageDataScraper;
    @MockitoBean TelegramBotService telegramBotService;
    @Autowired MockMvc mvc;

    @Test
    void documentsAllOperationsAndRequestExamples() throws Exception {
        mvc.perform(get("/v3/api-docs").header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Aapli Chawdi API"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.security[0].AdminApiKey").exists())
                .andExpect(jsonPath("$.paths['/api/telegram/webhook'].post.security[0].TelegramWebhookSecret").exists())
                .andExpect(jsonPath("$.paths.*", hasSize(6)))
                .andExpect(jsonPath("$.paths['/notices/trigger'].post").exists())
                .andExpect(jsonPath("$.paths['/notices/trigger/{villageId}'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/villages/scrape'].post").exists())
                .andExpect(jsonPath("$.paths['/api/subscriptions'].post").exists())
                .andExpect(jsonPath("$.paths['/api/subscriptions'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/subscriptions/{chatId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/telegram/webhook'].post.requestBody.content['application/json'].examples['Text message']").exists())
                .andExpect(jsonPath("$.components.schemas.SubscriptionRequest.properties.chatId.example").value(123456789));
    }

    @Test
    void servesSwaggerUiAndDownloadableYaml() throws Exception {
        mvc.perform(get("/swagger-ui.html").header("X-API-Key", ADMIN_KEY))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html").header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
        mvc.perform(get("/v3/api-docs/swagger-config").header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
        mvc.perform(get("/v3/api-docs.yaml").header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aapli Chawdi API")));
    }
}
