package com.example.aapliChawdi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public final class AuthenticationSecrets {
    private final byte[] adminKey;
    private final byte[] webhookSecret;

    public AuthenticationSecrets(@Value("${security.admin-api-key:}") String adminKey,
                                 @Value("${security.telegram-webhook-secret:}") String webhookSecret) {
        this.adminKey = validate("ADMIN_API_KEY", adminKey);
        this.webhookSecret = validate("TELEGRAM_WEBHOOK_SECRET", webhookSecret);
        if (MessageDigest.isEqual(this.adminKey, this.webhookSecret)) {
            throw new IllegalStateException("ADMIN_API_KEY and TELEGRAM_WEBHOOK_SECRET must be different");
        }
    }

    private static byte[] validate(String name, String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{32,256}")) {
            throw new IllegalStateException(name + " must contain 32–256 letters, digits, underscores or hyphens");
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public boolean matchesAdmin(String value) {
        return matches(adminKey, value);
    }

    public boolean matchesWebhook(String value) {
        return matches(webhookSecret, value);
    }

    private static boolean matches(byte[] expected, String supplied) {
        return supplied != null && supplied.length() <= 256
                && MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8));
    }
}
