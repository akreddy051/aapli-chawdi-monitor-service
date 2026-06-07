package com.example.aapliChawdi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class TelegramClient {

    @Value("${telegram.bot.token}")
    private String BOT_TOKEN;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // base URL builder
    private String url(String method) {
        return "https://api.telegram.org/bot" + BOT_TOKEN + "/" + method;
    }

    // core POST method — all API calls go through here
    private void post(String method, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url(method)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            log.info("Telegram [{}] response: {}", method, response.body());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to call Telegram API: " + method, e);
        }
    }

    // plain text message
    public void sendMessage(Long chatId, String text) {
        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text
        ));
    }

    // message with persistent bottom keyboard (main menu)
    public void sendMessageWithReplyKeyboard(
            Long chatId, String text, List<List<String>> buttons) {
        List<List<Map<String, String>>> keyboard = buttons.stream()
                .map(row -> row.stream()
                        .map(label -> Map.of("text", label))
                        .toList())
                .toList();

        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text,
                "reply_markup", Map.of(
                        "keyboard", keyboard,
                        "resize_keyboard", true,
                        "one_time_keyboard", false
                )
        ));
    }

    // message with inline buttons (district/taluka selection)
    public void sendMessageWithInlineKeyboard(
            Long chatId, String text, List<List<Map<String, String>>> buttons) {
        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text,
                "reply_markup", Map.of("inline_keyboard", buttons)
        ));
    }

    // answer a callback query (removes loading spinner after button tap)
    public void answerCallbackQuery(String callbackQueryId) {
        post("answerCallbackQuery", Map.of(
                "callback_query_id", callbackQueryId
        ));
    }

    // send photo with caption
    public void sendPhoto(Long chatId, byte[] imageBytes, String caption) {
        try {
            String boundary = "Boundary-" + UUID.randomUUID();
            String part1 =
                    "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n" +
                            chatId + "\r\n";
            String part2 =
                    "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"caption\"\r\n\r\n" +
                            caption + "\r\n";
            String part3Header =
                    "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"photo\";" +
                            " filename=\"notice.png\"\r\n" +
                            "Content-Type: image/png\r\n\r\n";
            String ending = "\r\n--" + boundary + "--\r\n";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url("sendPhoto")))
                    .header("Content-Type",
                            "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(
                            Arrays.asList(
                                    part1.getBytes(),
                                    part2.getBytes(),
                                    part3Header.getBytes(),
                                    imageBytes,
                                    ending.getBytes()
                            )
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            log.info("Telegram photo response: {}", response.body());

        } catch (Exception e) {
            throw new RuntimeException("Failed to send Telegram photo", e);
        }
    }
}