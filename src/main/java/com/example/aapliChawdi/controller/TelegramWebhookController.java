package com.example.aapliChawdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.example.aapliChawdi.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Telegram", description = "Receive updates delivered by Telegram.")
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;

    @Operation(security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "TelegramWebhookSecret"), summary = "Receive a Telegram update", description = "Processes a message or callback before acknowledging it. Sending a real update may change bot state and send Telegram replies.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "Empty probe", value = "{}", description = "Acknowledges without invoking the bot."),
                    @ExampleObject(name = "Text message", value = """
                            {"update_id": 10001, "message": {"message_id": 1, "chat": {"id": 123456789, "type": "private"}, "text": "/help"}}
                            """),
                    @ExampleObject(name = "Callback", value = """
                            {"update_id": 10002, "callback_query": {"id": "example-callback-id", "from": {"id": 123456789}, "message": {"chat": {"id": 123456789, "type": "private"}}, "data": "NOTICES_VILLAGE:1"}}
                            """, description = "Use a valid callback ID and village ID when testing against Telegram.")
            }))
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