package com.example.aapliChawdi.dto;

import lombok.Data;

@Data
public class SubscriptionRequest {

    private Long chatId;
    private String district;
    private String taluka;
    private String village;
}
