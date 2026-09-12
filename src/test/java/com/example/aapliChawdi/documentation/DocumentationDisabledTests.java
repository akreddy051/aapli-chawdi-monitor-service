package com.example.aapliChawdi.documentation;

import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import com.example.aapliChawdi.scraper.VillageDataScraper;
import com.example.aapliChawdi.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OpenApiDocumentationTests.DocumentationApplication.class, properties = {
        "security.admin-api-key=" + OpenApiDocumentationTests.ADMIN_KEY,
        "security.telegram-webhook-secret=" + OpenApiDocumentationTests.WEBHOOK_SECRET,
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class DocumentationDisabledTests {
    @MockitoBean NoticeOrchestrationService orchestrationService;
    @MockitoBean VillageRepositoryInterface villageRepository;
    @MockitoBean SubscriptionServiceInterface subscriptionService;
    @MockitoBean VillageDataScraper villageDataScraper;
    @MockitoBean TelegramBotService telegramBotService;
    @Autowired MockMvc mvc;

    @Test
    void documentationIsNotServedEvenToAdminByDefault() throws Exception {
        for (String path : new String[]{"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs.yaml"}) {
            mvc.perform(get(path).header("X-API-Key", OpenApiDocumentationTests.ADMIN_KEY))
                    .andExpect(status().isNotFound());
        }
    }
}
