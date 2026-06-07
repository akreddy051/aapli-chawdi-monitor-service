package com.example.aapliChawdi.repository;

import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.NoticeReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeReminderRepository
        extends JpaRepository<NoticeReminder, Long> {

    // find pending reminders — not yet sent
    List<NoticeReminder> findByRemindedFalse();

    // check if reminder already exists for this notice + user
    boolean existsByNoticeAndChatId(Notice notice, Long chatId);
}
