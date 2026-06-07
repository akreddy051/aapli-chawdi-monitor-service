package com.example.aapliChawdi.scraper;

import com.example.aapliChawdi.entity.Village;
import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.SelectOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VillageDataScraper {

    private final VillageRepositoryInterface villageRepository;

    @Value("${playwright.headless}")
    private boolean headless;

    @Value("${playwright.slowMo}")
    private int slowMo;

    public void scrapeAndStore() {
        List<String> districts = fetchDistrictsFromBrowser();

        boolean completed = false;
        int browserSessions = 0;

        while (!completed && browserSessions < 10) {
            browserSessions++;
            log.info("Starting browser session {}", browserSessions);

            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions()
                                .setHeadless(headless)
                                .setSlowMo(slowMo)
                );
                Page page = browser.newPage();
                page.setDefaultTimeout(60000);
                page.navigate(
                        "https://digitalsatbara.mahabhumi.gov.in/aaplichawdi"
                );
                page.locator("input[value='satbara']").click();

                completed = scrapeAllDistricts(page, districts);
                browser.close();

            } catch (Exception e) {
                log.warn(
                        "Browser session {} failed: {}. " +
                                "Waiting 30 seconds before retry...",
                        browserSessions, e.getMessage()
                );
                try {
                    Thread.sleep(30000); // wait 30 seconds before new session
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (completed) {
            log.info("Village data scraping completed successfully");
        } else {
            log.error("Village data scraping failed after {} browser sessions",
                    browserSessions);
        }
    }

    private List<String> fetchDistrictsFromBrowser() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(headless)
                            .setSlowMo(slowMo)
            );
            Page page = browser.newPage();
            page.setDefaultTimeout(60000);
            page.navigate(
                    "https://digitalsatbara.mahabhumi.gov.in/aaplichawdi"
            );
            page.locator("input[value='satbara']").click();
            page.waitForFunction(
                    "document.querySelector('#ddlDist1').options.length > 1"
            );
            List<String> districts = getOptions(page, "#ddlDist1");
            browser.close();
            log.info("Fetched {} districts", districts.size());
            return districts;
        }
    }

    private boolean scrapeAllDistricts(Page page, List<String> districts) {
        for (String district : districts) {
            page.locator("#ddlDist1")
                    .selectOption(new SelectOption().setLabel(district));
            page.waitForFunction(
                    "document.querySelector('#ddlTahsil').options.length > 1"
            );
            page.waitForTimeout(2000);

            List<String> talukas = getOptions(page, "#ddlTahsil");

            for (String taluka : talukas) {
                long talukaCount = villageRepository
                        .countByDistrictAndTaluka(district, taluka);
                if (talukaCount > 0) {
                    log.info("Skipping taluka {} - already scraped", taluka);
                    continue;
                }

                // this line can throw if portal rate limits us
                page.locator("#ddlTahsil")
                        .selectOption(new SelectOption().setLabel(taluka));
                page.waitForFunction(
                        "document.querySelector('#ddlVillage').options.length > 1"
                );
                page.waitForTimeout(3000);

                List<String> villages = getOptions(page, "#ddlVillage");
                for (String village : villages) {
                    saveIfNotExists(district, taluka, village);
                }

                log.info("Saved {} villages for {}, {}",
                        villages.size(), taluka, district);
            }
        }
        return true; // only reaches here if all districts completed
    }

    private List<String> getOptions(Page page, String selectId) {
        return (List<String>) page.evaluate(
                "Array.from(document.querySelector('" + selectId + "').options)" +
                        ".slice(1).map(o => o.text.trim())"
        );
    }

    private void saveIfNotExists(
            String district, String taluka, String village) {
        boolean exists = villageRepository
                .existsByDistrictAndTalukaAndVillage(district, taluka, village);
        if (!exists) {
            Village v = Village.builder()
                    .district(district)
                    .taluka(taluka)
                    .village(village)
                    .build();
            villageRepository.save(v);
        }
    }
}