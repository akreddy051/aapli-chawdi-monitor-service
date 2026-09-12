package com.example.aapliChawdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.aapliChawdi.scraper.VillageDataScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Villages", description = "Populate the village directory from the Aapli Chawdi portal.")
@RestController
@RequestMapping("/villages")
@RequiredArgsConstructor
@Slf4j
public class VillageDataController {

    private final VillageDataScraper villageDataScraper;

    @Operation(summary = "Scrape the village directory", description = "Runs browser scraping synchronously and writes locations to the database. May take several minutes. Inspect logs for partial failures.")
    @PostMapping("/scrape")
    public ResponseEntity<String> scrapeVillageData() {
        log.info("Village data scrape triggered manually");
        villageDataScraper.scrapeAndStore();
        return ResponseEntity.ok("Village data scrape complete");
    }
}
