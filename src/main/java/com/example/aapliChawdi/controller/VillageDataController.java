package com.example.aapliChawdi.controller;

import com.example.aapliChawdi.scraper.VillageDataScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/villages")
@RequiredArgsConstructor
@Slf4j
public class VillageDataController {

    private final VillageDataScraper villageDataScraper;

    @PostMapping("/scrape")
    public ResponseEntity<String> scrapeVillageData() {
        log.info("Village data scrape triggered manually");
        villageDataScraper.scrapeAndStore();
        return ResponseEntity.ok("Village data scrape complete");
    }
}
