package com.example.aapliChawdi.controller;

import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import com.example.aapliChawdi.service.NoticeOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeOrchestrationService orchestrationService;
    private final VillageRepositoryInterface villageRepository;

    // Trigger for all subscribed villages
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerAll() {
        orchestrationService.runForAllSubscribedVillages();
        return ResponseEntity.ok("Triggered for all villages");
    }

    // Trigger for a specific village
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
