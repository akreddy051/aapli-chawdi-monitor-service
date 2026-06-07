package com.example.aapliChawdi.service;

import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.Village;
import com.example.aapliChawdi.repository.SubscriptionRepositoryInterface;
import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import com.example.aapliChawdi.scraper.AapliChawdiScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeOrchestrationService {

    private final AapliChawdiScraper scraper;
    private final NoticeProcessingService noticeProcessingService;
    private final SubscriptionRepositoryInterface subscriptionRepositoryInterface;
    private final NoticeCleanupService noticeCleanupService;

    private final Set<Long> runningVillages = ConcurrentHashMap.newKeySet();

    public void runForVillage(Village village) {
        if (!runningVillages.add(village.getId())) {
            log.warn("Village {} already being processed, skipping",
                    village.getVillage());
            return;
        }
        try {
            List<Notice> notices = scraper.fetchNotices(village);
            noticeProcessingService.processNotices(notices);
            noticeCleanupService.cleanupForVillage(village);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            runningVillages.remove(village.getId());
        }
    }

    public void runForAllSubscribedVillages() {
        List<Village> villages = subscriptionRepositoryInterface.findAllSubscribedVillages();
        log.info("Running notice check for {} village(s)", villages.size());
        for (Village village : villages) {
            try {
                runForVillage(village);
            } catch (Exception e) {
                log.info("Scraping for the village: " + village.getVillage() + "in taluka - " + village.getTaluka() + " and district - " + village.getDistrict() + " got failed");
            }
        }
    }
}