package com.example.aapliChawdi.documentation;

import com.example.aapliChawdi.security.AuthenticationSecrets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationTests extends OpenApiDocumentationTests {
    @Test
    void everyAdminOperationAndDocumentationResourceRequiresCredentials() throws Exception {
        String[][] routes = {{"POST", "/notices/trigger"}, {"POST", "/notices/trigger/1"},
                {"POST", "/villages/scrape"}, {"POST", "/api/subscriptions"},
                {"DELETE", "/api/subscriptions"}, {"GET", "/api/subscriptions/1"},
                {"GET", "/swagger-ui.html"}, {"GET", "/swagger-ui/index.html"},
                {"GET", "/v3/api-docs"}, {"GET", "/v3/api-docs.yaml"},
                {"GET", "/v3/api-docs/swagger-config"}};
        for (String[] route : routes) {
            mvc.perform(request(HttpMethod.valueOf(route[0]), route[1]))
                    .andExpect(status().isUnauthorized());
            mvc.perform(request(HttpMethod.valueOf(route[0]), route[1]).header("X-API-Key", "wrong"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(orchestrationService, villageRepository, subscriptionService,
                villageDataScraper, telegramBotService);
    }

    @Test
    void validAdminCanTriggerButCannotAuthenticateWebhook() throws Exception {
        mvc.perform(post("/notices/trigger").header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID"));
        verify(orchestrationService).runForAllSubscribedVillages();
        mvc.perform(post("/api/telegram/webhook").header("X-API-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(telegramBotService);
    }

    @Test
    void webhookRequiresItsOwnSecretBeforeProcessingBody() throws Exception {
        String body = "{\"update_id\":42,\"message\":{\"chat\":{\"id\":123},\"text\":\"/help\"}}";
        for (String secret : new String[]{"", "wrong", ADMIN_KEY}) {
            mvc.perform(post("/api/telegram/webhook").header("X-Telegram-Bot-Api-Secret-Token", secret)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(telegramBotService);
        mvc.perform(post("/api/telegram/webhook").header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(telegramBotService).processMessage(123L, "/help");
        mvc.perform(post("/notices/trigger").header("X-Telegram-Bot-Api-Secret-Token", WEBHOOK_SECRET))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/notices/trigger").header("X-API-Key", WEBHOOK_SECRET))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orchestrationService);
    }

    @Test
    void rejectsQueryCookieDuplicateHeadersAndUnknownRoutes() throws Exception {
        mvc.perform(post("/notices/trigger").param("X-API-Key", ADMIN_KEY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/notices/trigger").cookie(new jakarta.servlet.http.Cookie("X-API-Key", ADMIN_KEY)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/notices/trigger").header("X-API-Key", ADMIN_KEY, ADMIN_KEY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/unknown").header("X-API-Key", ADMIN_KEY)).andExpect(status().isForbidden());
        mvc.perform(get("/api/telegram/webhook").header("X-API-Key", ADMIN_KEY)).andExpect(status().isForbidden());
        mvc.perform(post("/notices/trigger").header("X-API-Key", ADMIN_KEY)).andExpect(status().isOk());
        mvc.perform(post("/notices/trigger")).andExpect(status().isUnauthorized());
        verify(orchestrationService, times(1)).runForAllSubscribedVillages();
    }

    @Test
    void invalidSecretConfigurationFailsStartupWithoutPrintingSecret() {
        for (String invalid : new String[]{"", "too-short", "invalid secret with spaces 012345678901234567890", "a".repeat(257)}) {
            new ApplicationContextRunner().withBean(AuthenticationSecrets.class)
                    .withPropertyValues("security.admin-api-key=" + invalid,
                            "security.telegram-webhook-secret=" + WEBHOOK_SECRET)
                    .run(context -> assertNotNull(context.getStartupFailure()));
            assertThrows(IllegalStateException.class, () -> new AuthenticationSecrets(ADMIN_KEY, invalid));
        }
        new ApplicationContextRunner().withBean(AuthenticationSecrets.class)
                .run(context -> assertNotNull(context.getStartupFailure()));
        assertThrows(IllegalStateException.class, () -> new AuthenticationSecrets(ADMIN_KEY, ADMIN_KEY));
        var error = assertThrows(IllegalStateException.class,
                () -> new AuthenticationSecrets("sensitive invalid value", WEBHOOK_SECRET));
        assertFalse(error.getMessage().contains("sensitive invalid value"));
    }
}
