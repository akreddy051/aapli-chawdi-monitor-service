package com.example.aapliChawdi.service;

import com.example.aapliChawdi.client.GeminiClient;
import com.example.aapliChawdi.client.TelegramClient;
import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.NoticeReminder;
import com.example.aapliChawdi.entity.Subscription;
import com.example.aapliChawdi.entity.UserSession;
import com.example.aapliChawdi.enums.SessionState;
import com.example.aapliChawdi.repository.NoticeReminderRepository;
import com.example.aapliChawdi.repository.NoticeRepository;
import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService {

    private final TelegramMessageService telegramMessageService;
    private final SubscriptionServiceInterface subscriptionService;
    private final UserSessionService userSessionService;
    private final GeminiClient geminiClient;
    private final VillageRepositoryInterface villageRepository;
    private final TelegramClient telegramClient;
    private final NoticeRepository noticeRepository;
    private final NoticeReminderRepository noticeReminderRepository;

    public void processMessage(Long chatId, String text) {
        try {
            log.info("Received message from {}: {}", chatId, text);
            handleMessage(chatId, text);
        } catch (Exception e) {
            log.error("Failed to process message from {}", chatId, e);
            try {
                telegramMessageService.sendMessage(chatId,
                        "Something went wrong. Please try again or send /start.");
            } catch (Exception ex) {
                log.error("Failed to send error message to {}", chatId, ex);
            }
        }
    }

    public void processCallback(Long chatId, String callbackId, String data) {
        try {
            log.info("Received callback from {}: {}", chatId, data);

            // always acknowledge first — stops spinner on button
            telegramClient.answerCallbackQuery(callbackId);

            String[] parts = data.split(":", 2);
            String type = parts[0];
            String value = parts[1];

            UserSession session = userSessionService.getSession(chatId);

            switch (type) {
                case "DISTRICT" -> {
                    session.setDistrict(value);
                    session.setState(SessionState.WAITING_FOR_TALUKA);
                    userSessionService.save(session);
                    sendTalukaButtons(chatId, value);
                }
                case "TALUKA" -> {
                    session.setTaluka(value);
                    session.setState(SessionState.WAITING_FOR_VILLAGE);
                    userSessionService.save(session);
                    telegramMessageService.sendMessage(chatId,
                            "Which village in " + value +
                                    ", " + session.getDistrict() + "?" +
                                    "\n\n(send /cancel to stop)");
                }
                case "UNSUBSCRIBE" -> {
                    Long subscriptionId = Long.valueOf(value);
                    subscriptionService.unsubscribeById(subscriptionId);
                    telegramMessageService.sendMessage(chatId,
                            "✅ Subscription removed successfully.");
                }
                case "NOTICES_VILLAGE" -> {
                    Long villageId = Long.valueOf(value);
                    sendNoticesList(chatId, villageId);
                }
                case "NOTICE_DETAIL" -> {
                    Long noticeId = Long.valueOf(value);
                    sendNoticeDetail(chatId, noticeId);
                }
                case "REMINDER_YES" -> {
                    Long noticeId = Long.valueOf(value);
                    noticeRepository.findById(noticeId).ifPresent(notice -> {
                        boolean exists = noticeReminderRepository
                                .existsByNoticeAndChatId(notice, chatId);
                        if (!exists) {
                            NoticeReminder reminder = NoticeReminder.builder()
                                    .notice(notice)
                                    .chatId(chatId)
                                    .reminded(false)
                                    .build();
                            noticeReminderRepository.save(reminder);
                            telegramMessageService.sendMessage(chatId,
                                    "✅ Reminder set! I'll notify you 3 days before " +
                                            notice.getObjectionLastDate() + ".");
                        } else {
                            telegramMessageService.sendMessage(chatId,
                                    "You already have a reminder set for this notice.");
                        }
                    });
                }
                case "REMINDER_NO" -> {
                    telegramMessageService.sendMessage(chatId, "Got it, no reminder.");
                }
                case "NOTICES_ACTIVE" -> {
                    Long villageId = Long.valueOf(value);
                    sendFilteredNotices(chatId, villageId, true);
                }
                case "NOTICES_EXPIRED" -> {
                    Long villageId = Long.valueOf(value);
                    sendFilteredNotices(chatId, villageId, false);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process callback from {}", chatId, e);
            try {
                telegramMessageService.sendMessage(chatId,
                        "Something went wrong. Please try again or send /start.");
            } catch (Exception ex) {
                log.error("Failed to send error message to {}", chatId, ex);
            }
        }
    }

    private void handleMessage(Long chatId, String text) {

        // allow cancel anytime regardless of state
        if ("/cancel".equalsIgnoreCase(text.trim()) ||
                "cancel".equalsIgnoreCase(text.trim()) ||
                "🔄 refresh".equalsIgnoreCase(text.trim())) {
            UserSession session = userSessionService.getSession(chatId);
            userSessionService.reset(session);
            telegramClient.sendMessageWithReplyKeyboard(
                    chatId,
                    "✅ Reset done. What would you like to do?",
                    List.of(
                            List.of("Subscribe", "My Subscriptions"),
                            List.of("Unsubscribe", "My Notices"),
                            List.of("🔄 Refresh", "Help")
                    )
            );
            return;
        }

        UserSession session = userSessionService.getSession(chatId);
        SessionState state = session.getState();

        // district and taluka are handled via buttons (callbacks)
        // only village still needs text input
        if (state == SessionState.WAITING_FOR_VILLAGE) {
            String district = session.getDistrict();
            String taluka = session.getTaluka();
            List<String> villages = villageRepository
                    .findVillagesByDistrictAndTaluka(district, taluka);
            if (villages.isEmpty()) {
                telegramMessageService.sendMessage(chatId,
                        "No village data found for " + taluka +
                                ". Please contact the administrator.");
                return;
            }
            String matched = geminiClient.matchFromList(text, villages);
            if ("INVALID".equalsIgnoreCase(matched.trim())) {
                telegramMessageService.sendMessage(chatId,
                        "There is no village named '" + text +
                                "' under " + taluka + " and " + district +
                                ". Please enter a valid village name.");
                return;
            }
            subscriptionService.subscribe(chatId, district, taluka, matched);
            userSessionService.reset(session);
            telegramMessageService.sendMessage(chatId,
                    "✅ Subscribed to " + matched + ", " + taluka +
                            ", " + district);
            return;
        }

        // state is IDLE — handle commands
        handleCommand(chatId, text);
    }

    private void handleCommand(Long chatId, String text) {
        if (text == null) {
            return;
        }
        switch (text.trim().toLowerCase()) {
            case "/start":
                UserSession session = userSessionService.getSession(chatId);
                userSessionService.reset(session);
                telegramClient.sendMessageWithReplyKeyboard(
                        chatId,
                        "Welcome to Aapli Chawdi Monitor.",
                        List.of(
                                List.of("Subscribe", "My Subscriptions"),
                                List.of("Unsubscribe", "My Notices"),
                                List.of("🔄 Refresh", "Help")
                        )
                );
                break;
            case "subscribe":
            case "/subscribe":
                UserSession subSession = userSessionService.getSession(chatId);
                subSession.setState(SessionState.WAITING_FOR_DISTRICT);
                userSessionService.save(subSession);
                sendDistrictButtons(chatId);
                break;
            case "my subscriptions":
            case "/list":
                List<Subscription> subs =
                        subscriptionService.getSubscriptions(chatId);
                if (subs.isEmpty()) {
                    telegramMessageService.sendMessage(chatId,
                            "You have no active subscriptions.");
                } else {
                    StringBuilder sb = new StringBuilder("Your subscriptions:\n\n");
                    for (int i = 0; i < subs.size(); i++) {
                        var v = subs.get(i).getVillage();
                        sb.append(i + 1).append(". ")
                                .append(v.getVillage()).append(", ")
                                .append(v.getTaluka()).append(", ")
                                .append(v.getDistrict()).append("\n");
                    }
                    telegramMessageService.sendMessage(chatId, sb.toString());
                }
                break;
            case "unsubscribe":
            case "/unsubscribe":
                sendUnsubscribeButtons(chatId);
                break;
            case "help":
            case "/help":
                telegramMessageService.sendMessage(chatId,
                        """
                                Commands:
                                
                                /subscribe - Subscribe to a village
                                /list - View your subscriptions
                                /unsubscribe - Remove a subscription
                                /cancel - Cancel current operation
                                """
                );
                break;
            case "cancel":
            case "/cancel":
                UserSession cancelSession = userSessionService.getSession(chatId);
                userSessionService.reset(cancelSession);
                telegramMessageService.sendMessage(chatId,
                        "Cancelled. Send /start to see available commands.");
                break;
            case "my notices":
            case "/notices":
                sendNoticesVillageButtons(chatId);
                break;
            case "🔄 refresh":
                UserSession refreshSession = userSessionService.getSession(chatId);
                userSessionService.reset(refreshSession);
                telegramClient.sendMessageWithReplyKeyboard(
                        chatId,
                        "✅ Reset done. What would you like to do?",
                        List.of(
                                List.of("Subscribe", "My Subscriptions"),
                                List.of("Unsubscribe", "My Notices"),
                                List.of("🔄 Refresh", "Help")
                        )
                );
                break;
            default:
                telegramMessageService.sendMessage(chatId,
                        "Unknown command. Send /start to see available commands.");
        }
    }

    private void sendDistrictButtons(Long chatId) {
        List<String> districts = villageRepository.findDistinctDistricts();

        List<List<Map<String, String>>> rows = new ArrayList<>();
        List<Map<String, String>> currentRow = new ArrayList<>();

        for (String district : districts) {
            currentRow.add(Map.of(
                    "text", district,
                    "callback_data", "DISTRICT:" + district
            ));
            if (currentRow.size() == 3) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        telegramClient.sendMessageWithInlineKeyboard(
                chatId, "Which district?", rows);
    }

    private void sendTalukaButtons(Long chatId, String district) {
        List<String> talukas = villageRepository
                .findDistinctTalukasByDistrict(district);

        List<List<Map<String, String>>> rows = new ArrayList<>();
        List<Map<String, String>> currentRow = new ArrayList<>();

        for (String taluka : talukas) {
            currentRow.add(Map.of(
                    "text", taluka,
                    "callback_data", "TALUKA:" + taluka
            ));
            if (currentRow.size() == 3) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        telegramClient.sendMessageWithInlineKeyboard(
                chatId, "Which taluka in " + district + "?", rows);
    }

    private void sendUnsubscribeButtons(Long chatId) {
        List<com.example.aapliChawdi.entity.Subscription> subs =
                subscriptionService.getSubscriptions(chatId);

        if (subs.isEmpty()) {
            telegramMessageService.sendMessage(chatId,
                    "You have no active subscriptions.");
            return;
        }

        List<List<Map<String, String>>> rows = new ArrayList<>();

        for (var sub : subs) {
            var v = sub.getVillage();
            String label = v.getVillage() + ", " + v.getTaluka()
                    + ", " + v.getDistrict();
            rows.add(List.of(Map.of(
                    "text", label,
                    "callback_data", "UNSUBSCRIBE:" + sub.getId()
            )));
        }

        telegramClient.sendMessageWithInlineKeyboard(
                chatId, "Which subscription do you want to remove?", rows);
    }

    private void sendNoticesVillageButtons(Long chatId) {
        List<com.example.aapliChawdi.entity.Subscription> subs =
                subscriptionService.getSubscriptions(chatId);

        if (subs.isEmpty()) {
            telegramMessageService.sendMessage(chatId,
                    "You have no active subscriptions.");
            return;
        }

        List<List<Map<String, String>>> rows = new ArrayList<>();

        for (var sub : subs) {
            var v = sub.getVillage();
            String label = v.getVillage() + ", " + v.getTaluka();
            rows.add(List.of(Map.of(
                    "text", label,
                    "callback_data", "NOTICES_VILLAGE:" + v.getId()
            )));
        }

        telegramClient.sendMessageWithInlineKeyboard(
                chatId, "Which village?", rows);
    }

    private void sendNoticesList(Long chatId, Long villageId) {
        villageRepository.findById(villageId).ifPresent(village -> {
            List<Notice> notices = noticeRepository
                    .findTop10ByVillageAndProcessedAtIsNotNullOrderByProcessedAtDesc(
                            village);

            if (notices.isEmpty()) {
                telegramMessageService.sendMessage(chatId,
                        "No processed notices found for " +
                                village.getVillage() + ".");
                return;
            }

            telegramClient.sendMessageWithInlineKeyboard(
                    chatId,
                    "What would you like to see for " +
                            village.getVillage() + "?",
                    List.of(List.of(
                            Map.of("text", "📋 Active Notices",
                                    "callback_data", "NOTICES_ACTIVE:" + villageId),
                            Map.of("text", "📁 Expired Notices",
                                    "callback_data", "NOTICES_EXPIRED:" + villageId)
                    ))
            );
        });
    }

    private void sendNoticeDetail(Long chatId, Long noticeId) {
        noticeRepository.findById(noticeId).ifPresent(notice -> {
            // send screenshot if available
            if (notice.getScreenshotData() != null) {
                telegramClient.sendPhoto(
                        chatId,
                        notice.getScreenshotData(),
                        "📋 " + notice.getMutationType() +
                                " | Mutation No: " + notice.getMutationNo()
                );
            }
            // send summary
            telegramMessageService.sendMessage(chatId, notice.getSummary());
        });
    }

    private void sendFilteredNotices(
            Long chatId, Long villageId, boolean active) {
        villageRepository.findById(villageId).ifPresent(village -> {
            List<Notice> notices = noticeRepository
                    .findTop10ByVillageAndProcessedAtIsNotNullOrderByProcessedAtDesc(
                            village);

            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // filter based on active or expired
            List<Notice> filtered = notices.stream()
                    .filter(n -> {
                        if (n.getObjectionLastDate() == null) return false;
                        try {
                            LocalDate deadline = LocalDate.parse(
                                    n.getObjectionLastDate(), formatter);
                            return active
                                    ? !deadline.isBefore(today)   // active: today or future
                                    : deadline.isBefore(today);    // expired: past
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());

            if (filtered.isEmpty()) {
                telegramMessageService.sendMessage(chatId,
                        active
                                ? "No active notices found for " +
                                village.getVillage() + "."
                                : "No expired notices found for " +
                                village.getVillage() + "."
                );
                return;
            }

            List<List<Map<String, String>>> rows = new ArrayList<>();

            for (Notice notice : filtered) {
                String label = notice.getMutationNo() +
                        " | " +notice.getMutationType() +
                        " | " + notice.getMutationDate() +
                        (active ? " ⏳ " + notice.getObjectionLastDate() : "");
                rows.add(List.of(Map.of(
                        "text", label,
                        "callback_data", "NOTICE_DETAIL:" + notice.getId()
                )));
            }

            telegramClient.sendMessageWithInlineKeyboard(
                    chatId,
                    (active ? "📋 Active Notices" : "📁 Expired Notices") +
                            " for " + village.getVillage() + ":",
                    rows
            );
        });
    }
}