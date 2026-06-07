package com.example.aapliChawdi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    private Long chatId;

    private boolean reminded;
}
