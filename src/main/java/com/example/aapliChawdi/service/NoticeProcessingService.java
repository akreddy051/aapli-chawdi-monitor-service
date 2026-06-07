package com.example.aapliChawdi.service;

import com.example.aapliChawdi.client.GeminiClient;
import com.example.aapliChawdi.client.TelegramClient;
import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.Subscription;
import com.example.aapliChawdi.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeProcessingService {

    private final NoticeRepository noticeRepository;
    private final GeminiClient geminiClient;
    private final TelegramClient telegramClient;
    private final SubscriptionServiceInterface subscriptionService;

    public void processNotices(List<Notice> notices) {
        for (Notice notice : notices) {

            // bodyText is null for already processed notices — skip them
            if (notice.getBodyText() == null) {
                log.info("Skipping already processed notice: {}", notice.getMutationNo());
                continue;
            }

            String summary = geminiClient.summarizeNotice(notice.getBodyText());

            // Gemini returns DATA_NOT_FOUND for error pages or invalid content
            if ("DATA_NOT_FOUND".equalsIgnoreCase(summary.trim())) {
                log.warn("Invalid notice page for mutation: {}", notice.getMutationNo());
                continue;
            }

            // Save processed notice to database
            notice.setSummary(summary);
            notice.setProcessedAt(LocalDateTime.now());
            try {
                noticeRepository.save(notice);
            } catch (DataIntegrityViolationException e) {
                log.warn("Notice {} already saved by another thread, skipping",
                        notice.getMutationNo());
                continue;
            }

            // Notify all subscribers of this village
            List<Subscription> subscribers =
                    subscriptionService.getSubscribers(notice.getVillage());

            for (Subscription subscriber : subscribers) {
                telegramClient.sendPhoto(
                        subscriber.getChatId(),
                        notice.getScreenshotData(),
                        "📢 New Mutation Notice\nMutation No: " + notice.getMutationNo()
                );
                telegramClient.sendMessage(subscriber.getChatId(), summary);
                // ask if they want a reminder
                telegramClient.sendMessageWithInlineKeyboard(
                        subscriber.getChatId(),
                        "⏰ Objection deadline: " + notice.getObjectionLastDate() +
                                "\nWant a reminder 3 days before the deadline?",
                        List.of(List.of(
                                Map.of("text", "✅ Yes, remind me",
                                        "callback_data", "REMINDER_YES:" + notice.getId()),
                                Map.of("text", "❌ No thanks",
                                        "callback_data", "REMINDER_NO:" + notice.getId())
                        ))
                );
            }

            log.info("Notified {} subscriber(s) for mutation {}.",
                    subscribers.size(), notice.getMutationNo());
        }
    }
}