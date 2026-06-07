package com.example.aapliChawdi.scraper;

import com.example.aapliChawdi.client.GeminiClient;
import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.Village;
import com.example.aapliChawdi.repository.NoticeRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.SelectOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Component
@RequiredArgsConstructor
@Slf4j
public class AapliChawdiScraper {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private final GeminiClient geminiClient;
    private final NoticeRepository noticeRepository;

    private static final int MAX_CAPTCHA_ATTEMPTS = 1;

    private static final Logger logger =
            LoggerFactory.getLogger(
                    AapliChawdiScraper.class);

    @Value("${playwright.headless}")
    private boolean headless;

    @Value("${playwright.slowMo}")
    private int slowMo;

    private void initBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo)
        );
        context = browser.newContext();
        page = context.newPage();
    }

    private void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    public List<Notice> fetchNotices(Village village) throws IOException {
        Files.createDirectories(Paths.get("screenshots"));
        List<Notice> notices = new ArrayList<>();
        try {
            // 1. Setup browser
            initBrowser();
            // 2. Open portal
            page.navigate("https://digitalsatbara.mahabhumi.gov.in/aaplichawdi");
            // 3. Select village + handle captcha
            selectVillage(page, village);
            // 4. Verify dashboard
            verifyVillageDashboard(page, village);
            // 5. Wait for table
            page.locator("#data").waitFor();
            // 6. Scrape data
            Locator rows = page.locator("#data tr");
            int rowCount = rows.count();
            // Skip header row (index 0)
            for (int i = 1; i < rowCount; i++) {
                Locator row = rows.nth(i);
                Locator cells = row.locator("td");
                if (cells.count() < 8) {
                    continue;
                }
                Notice notice = Notice.builder()
                        .village(village)
                        .registrationOffice(getText(cells, 0))
                        .registrationNo(getText(cells, 1))
                        .mutationNo(getText(cells, 2))
                        .mutationType(getText(cells, 3))
                        .mutationDate(getText(cells, 4))
                        .objectionLastDate(getText(cells, 5))
                        .surveyNo(getText(cells, 6))
                        .noticeUrl(getNoticeUrl(cells.nth(7)))
                        .build();
                notices.add(notice);
            }
            for (Notice notice : notices) {
                if (noticeRepository.existsByMutationNo(notice.getMutationNo())) {
                    logger.info("Already processed: {}", notice.getMutationNo());
                    continue;
                }
                try (Page noticePage = page.context().newPage()) {
                    noticePage.navigate(notice.getNoticeUrl());
                    noticePage.waitForLoadState();
                    byte[] screenshotBytes = noticePage.screenshot(
                            new Page.ScreenshotOptions()
                                    .setFullPage(true)
                    );
                    notice.setScreenshotData(screenshotBytes);
                    notice.setBodyText(noticePage.locator("body").innerText());
                }
            }
            return notices;
        } catch (Exception e) {
            throw new RuntimeException("Error while fetching notices", e);
        } finally {
            // 7. Always close browser
            closeBrowser();
        }
    }

    private void selectVillage(Page page, Village village) {
        page.locator("input[value='satbara']").click();
        page.locator("#ddlDist1")
                .selectOption(new SelectOption().setLabel(village.getDistrict()));
        String talukaName = village.getTaluka();
        log.info("Taluka bytes: {}",
                talukaName.chars()
                        .mapToObj(c -> String.format("U+%04X", c))
                        .collect(java.util.stream.Collectors.joining(" "))
        );
        System.out.println("taluka to enter: "+village.getTaluka());
        String optionsText = (String) page.evaluate(
                "Array.from(document.querySelector('#ddlTahsil').options)" +
                        ".map(o => o.text + ' [' + Array.from(o.text).map(c => c.codePointAt(0).toString(16)).join(',') + ']')" +
                        ".join('\\n')"
        );
        log.info("Dropdown options:\n{}", optionsText);
        page.locator("#ddlTahsil")
                .selectOption(new SelectOption().setLabel(village.getTaluka()));
        page.locator("#ddlVillage")
                .selectOption(new SelectOption().setLabel(village.getVillage()));
        page.locator("#myimg")
                .screenshot(new Locator.ScreenshotOptions()
                        .setPath(Paths.get("screenshots/puzzle.png")));
        solveCaptcha(page);
    }

    private void solveCaptcha(Page page) {
        for (int attempt = 1; attempt <= MAX_CAPTCHA_ATTEMPTS; attempt++) {
            try {
                logger.info("Captcha attempt {}", attempt);
                String captchaImagePath =
                        "screenshots/captcha-" + attempt + ".png";
                page.locator("#myimg")
                        .screenshot(
                                new Locator.ScreenshotOptions()
                                        .setPath(Paths.get(captchaImagePath))
                        );
                String captchaCode = geminiClient.solveCaptcha(captchaImagePath);
                logger.info(
                        "Captcha identified by AI: {}",
                        captchaCode
                );
                page.locator("#CaptchaText").fill("");
                page.locator("#CaptchaText").fill(captchaCode);
                page.locator("#Submit").click();
                handleRefreshPage(page);
                page.locator("#tbl_headertext")
                        .waitFor(
                                new Locator.WaitForOptions()
                                        .setTimeout(45000)
                        );
                logger.info(
                        "Captcha solved successfully on attempt {}",
                        attempt
                );
                return;
            } catch (Exception e) {
                logger.warn(
                        "Captcha attempt {} failed",
                        attempt
                );
                if (attempt == MAX_CAPTCHA_ATTEMPTS) {
                    throw new RuntimeException(
                            "Failed to solve captcha after "
                                    + MAX_CAPTCHA_ATTEMPTS
                                    + " attempts",
                            e
                    );
                }
                page.locator("#CaptchaText").fill("");
                page.waitForTimeout(1000);
            }
        }
    }

    private void verifyVillageDashboard(Page page, Village village) {
        Locator headerSection = page.locator("#tbl_headertext");
        assertThat(headerSection).isVisible();
        assertThat(headerSection).containsText(village.getDistrict());
        assertThat(headerSection).containsText(village.getTaluka());
        assertThat(headerSection).containsText(village.getVillage());
    }

    private String getText(Locator cells, int index) {
        String text = cells.nth(index).textContent();
        return text == null ? "" : text.trim();
    }

    private String getNoticeUrl(Locator cell) {
        String href = cell.locator("a").getAttribute("href");
        if (href == null || href.isBlank()) {
            return null;
        }
        if (!href.startsWith("http")) {
            href = "https://digitalsatbara.mahabhumi.gov.in" + href;
        }
        return href;
    }

    private void handleRefreshPage(Page page) {
        try {
            Locator refreshLink = page.getByRole(
                    com.microsoft.playwright.options.AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("\"Refresh\".")
            );

            if (refreshLink.isVisible(
                    new Locator.IsVisibleOptions().setTimeout(3000))) {

                log.warn("Refresh page detected. Clicking Refresh...");

                refreshLink.click();

                page.locator("#tbl_headertext")
                        .waitFor(new Locator.WaitForOptions()
                                .setTimeout(45000));

                log.info("Successfully navigated to notices page after refresh");
            }
        } catch (Exception ignored) {
            // Refresh page not present
        }
    }
}

