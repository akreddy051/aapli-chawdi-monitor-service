package com.example.aapliChawdi.scheduler;

import com.example.aapliChawdi.service.NoticeOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NoticeScheduler {

    private final NoticeOrchestrationService orchestrationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void run() {
        log.info("Scheduled daily run triggered");
        orchestrationService.runForAllSubscribedVillages();
    }
}
