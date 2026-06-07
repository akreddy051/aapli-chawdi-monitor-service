package com.example.aapliChawdi.entity;

import com.example.aapliChawdi.enums.SessionState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class UserSession {

    @Id
    private Long chatId;
    @Enumerated(EnumType.STRING)
    private SessionState state;
    private String district;
    private String taluka;
}