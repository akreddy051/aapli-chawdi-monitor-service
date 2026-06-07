package com.example.aapliChawdi.service;

import com.example.aapliChawdi.client.TelegramClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final TelegramClient telegramClient;

    public void sendMessage(
            Long chatId,
            String message) {

        telegramClient.sendMessage(
                chatId,
                message
        );
    }
}
