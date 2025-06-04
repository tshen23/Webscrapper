package net.neological.webscraping.specific;

import io.github.bonigarcia.wdm.WebDriverManager;
import net.neological.webscraping.WebScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;

public class FredWebScraper extends WebScraper {
    /**
     * Constructor.
     *
     * @param name          the name of the web scraper
     * @param userAgent     the User-Agent header to present when fetching pages.
     * @param timeoutMillis the timeout (in milliseconds) for the HTTP connection and read.
     */
    public FredWebScraper(String name, String userAgent, int timeoutMillis) {
        super(name, userAgent, timeoutMillis);
    }

    @Override
    protected void parse(Document document) throws IOException {
        Elements seriesLinks = document.select("a[href^=/series/]");

        FredWebScraper.Series seriesScraper = new Series(null, userAgent, timeoutMillis);

        for (Element link : seriesLinks) {
            String fullUrl = link.absUrl("href");

            seriesScraper.scrape(fullUrl);
        }
    }

    @Override
    protected boolean isValid(String url) {
        return url != null
                && url.startsWith("https://fred.stlouisfed.org/searchresults/");
    }

    private static class Series extends WebScraper {

        /**
         * Constructor.
         *
         * @param name          the name of the web scraper
         * @param userAgent     the User-Agent header to present when fetching pages.
         * @param timeoutMillis the timeout (in milliseconds) for the HTTP connection and read.
         */
        protected Series(String name, String userAgent, int timeoutMillis) {
            super(name, userAgent, timeoutMillis);
        }

        @Override
        protected Document fetchDocument(String url) throws IOException {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--blink-settings=imagesEnabled=false"); // turn off image loading
            options.addArguments("--user-agent=" + userAgent);

            WebDriver driver = new ChromeDriver(options);
            try {
                driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeoutMillis));
                driver.get(url);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMillis));
                WebElement button = wait.until(
                        ExpectedConditions.elementToBeClickable(By.id("download-button"))
                );
                button.click();

                Thread.sleep(2_000);
                String updatedHtml = driver.getPageSource();
                return Jsoup.parse(updatedHtml, driver.getCurrentUrl());
            } catch (Exception e) {
                throw new IOException("Failed to fetch/render page via Selenium: " + e.getMessage(), e);
            } finally {
                driver.quit();
            }
        }

        @Override
        protected void parse(Document document) {
            Element csvAnchor = document.selectFirst("a#download-data-csv");
            if (csvAnchor == null) {
                System.err.println("No CSV link found on the page.");
                return;
            }

            String csvUrl = csvAnchor.absUrl("href");
            if (csvUrl.isBlank()) {
                System.err.println("CSV link had an empty href.");
                return;
            }

            System.out.println("Downloading CSV from: " + csvUrl);
        }

        @Override
        protected boolean isValid(String url) {
            return url != null
                    && url.startsWith("https://fred.stlouisfed.org/series/");
        }
    }
}
