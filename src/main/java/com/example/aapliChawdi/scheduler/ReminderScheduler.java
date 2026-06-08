package com.example.aapliChawdi.scheduler;

import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.NoticeReminder;
import com.example.aapliChawdi.repository.NoticeReminderRepository;
import com.example.aapliChawdi.service.TelegramMessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final NoticeReminderRepository noticeReminderRepository;
    private final TelegramMessageService telegramMessageService;

    private static final DateTimeFormatter PORTAL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    @Scheduled(cron = "0 0 8 * * *") // 8 AM daily
    public void sendDeadlineReminders() {
        log.info("Running deadline reminder check");

        List<NoticeReminder> pending =
                noticeReminderRepository.findByRemindedFalse();

        LocalDate today = LocalDate.now();

        for (NoticeReminder reminder : pending) {
            Notice notice = reminder.getNotice();

            if (notice.getObjectionLastDate() == null) {
                continue;
            }

            try {
                LocalDate deadline = LocalDate.parse(
                        notice.getObjectionLastDate(),
                        PORTAL_DATE_FORMAT
                );

                long daysUntilDeadline = ChronoUnit.DAYS
                        .between(today, deadline);

                if (daysUntilDeadline == 3) {
                    telegramMessageService.sendMessage(
                            reminder.getChatId(),
                            "⚠️ Reminder: The objection deadline for " +
                                    "mutation " + notice.getMutationNo() +
                                    " in " + notice.getVillage().getVillage() +
                                    " is on " + notice.getObjectionLastDate() +
                                    " — only 3 days left to file an objection!"
                    );
                    reminder.setReminded(true);
                    noticeReminderRepository.save(reminder);
                    log.info("Reminder sent for notice {} to chatId {}",
                            notice.getMutationNo(), reminder.getChatId());
                }

                // deadline already passed — mark as reminded
                // so it doesn't keep checking
                if (daysUntilDeadline < 0) {
                    reminder.setReminded(true);
                    noticeReminderRepository.save(reminder);
                }

            } catch (Exception e) {
                log.error("Failed to process reminder for notice {}",
                        notice.getMutationNo(), e);
            }
        }
    }
}
