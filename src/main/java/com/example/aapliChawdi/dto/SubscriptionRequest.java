package com.example.aapliChawdi.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class SubscriptionRequest {

    @Schema(description = "Telegram chat ID", example = "123456789")
    private Long chatId;
    @Schema(description = "Exact district label stored in the village directory", example = "यवतमाळ")
    private String district;
    @Schema(description = "Exact taluka label stored in the village directory", example = "केळापूर")
    private String taluka;
    @Schema(description = "Exact village label stored in the village directory", example = "सुन्ना")
    private String village;
}
