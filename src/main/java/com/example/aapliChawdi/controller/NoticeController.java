package com.example.aapliChawdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import com.example.aapliChawdi.service.NoticeOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notices", description = "Collect notices for subscribed villages.")
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeOrchestrationService orchestrationService;
    private final VillageRepositoryInterface villageRepository;

    // Trigger for all subscribed villages
    @Operation(summary = "Check all subscribed villages", description = "Runs notice collection synchronously and may send Telegram alerts to subscribers.")
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerAll() {
        orchestrationService.runForAllSubscribedVillages();
        return ResponseEntity.ok("Triggered for all villages");
    }

    // Trigger for a specific village
    @Operation(summary = "Check one village", description = "Runs notice collection synchronously for a stored village and may send Telegram alerts.")
    @ApiResponse(responseCode = "200", description = "Village processing returned")
    @ApiResponse(responseCode = "404", description = "Village not found", content = @Content)
    @PostMapping("/trigger/{villageId}")
    public ResponseEntity<String> triggerForVillage(
            @PathVariable Long villageId) {
        return villageRepository.findById(villageId)
                .map(village -> {
                    orchestrationService.runForVillage(village);
                    return ResponseEntity.ok(
                            "Triggered for village: " + village.getVillage());
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
