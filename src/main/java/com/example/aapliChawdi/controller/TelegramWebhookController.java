package com.example.aapliChawdi.controller;

import com.example.aapliChawdi.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveUpdate(
            @RequestBody Map<String, Object> update) {

        log.info("Received Telegram update");

        // handle regular text message
        if (update.containsKey("message")) {
            Map<String, Object> message =
                    (Map<String, Object>) update.get("message");
            Map<String, Object> chat =
                    (Map<String, Object>) message.get("chat");
            Long chatId = Long.valueOf(chat.get("id").toString());
            String text = String.valueOf(message.get("text"));
            telegramBotService.processMessage(chatId, text);
        }

        // handle inline button tap
        if (update.containsKey("callback_query")) {
            Map<String, Object> callbackQuery =
                    (Map<String, Object>) update.get("callback_query");
            String callbackId = callbackQuery.get("id").toString();
            Map<String, Object> from =
                    (Map<String, Object>) callbackQuery.get("from");
            Long chatId = Long.valueOf(from.get("id").toString());
            String data = callbackQuery.get("data").toString();
            telegramBotService.processCallback(chatId, callbackId, data);
        }

        return ResponseEntity.ok().build();
    }
}