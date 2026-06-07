package com.example.aapliChawdi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private Village village;

    private String registrationOffice;
    private String registrationNo;
    @Column(unique = true)
    private String mutationNo;
    private String mutationType;
    private String mutationDate;
    private String objectionLastDate;
    private String surveyNo;
    private String noticeUrl;

    // filled after processing
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summary;
    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] screenshotData;
    private LocalDateTime processedAt;
    @Transient
    private String bodyText;
}
