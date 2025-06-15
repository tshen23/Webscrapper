package net.neological.webscraping.specific;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Setter;
import net.neological.webscraping.FileDownloader;
import net.neological.webscraping.WebScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class FredWebScraper extends WebScraper {

    @Setter
    private String downloadFolder;

    /**
     * Constructor.
     *
     * @param userAgent     the User-Agent header to present when fetching pages.
     * @param timeoutMillis the timeout (in milliseconds) for the HTTP connection and read.
     */
    public FredWebScraper(String userAgent, int timeoutMillis) {
        super(userAgent, timeoutMillis);
        this.downloadFolder = System.getProperty("user.home") + File.separator + "Downloads";
    }

    @Override
    protected void parse(Document document) throws IOException {
        Elements seriesLinks = document.select("a[href^=/series/]");

        FredWebScraper.Series seriesScraper = new Series(userAgent, timeoutMillis);
        seriesScraper.setDownloadFolder(downloadFolder);

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

    private static class Series extends WebScraper implements FileDownloader {

        @Setter
        private String downloadFolder;

        /**
         * Constructor.
         *
         * @param userAgent     the User-Agent header to present when fetching pages.
         * @param timeoutMillis the timeout (in milliseconds) for the HTTP connection and read.
         */
        protected Series(String userAgent, int timeoutMillis) {
            super(userAgent, timeoutMillis);
            this.downloadFolder = System.getProperty("user.home") + File.separator + "Downloads";
        }

        @Override
        protected Document fetchDocument(String url) throws IOException {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--blink-settings=imagesEnabled=false");
            options.addArguments("--user-agent=" + userAgent);

            WebDriver driver = new ChromeDriver(options);
            try {
                driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeoutMillis));
                driver.get(url);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMillis));

                // Wait for the page to load completely
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("download-button")));

                // Try multiple strategies to click the button
                WebElement button = null;
                try {
                    // First try: wait for element to be clickable
                    button = wait.until(ExpectedConditions.elementToBeClickable(By.id("download-button")));
                    button.click();
                } catch (Exception e1) {
                    try {
                        // Second try: use JavaScript click
                        button = driver.findElement(By.id("download-button"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
                    } catch (Exception e2) {
                        // Third try: scroll to element and click
                        button = driver.findElement(By.id("download-button"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                        Thread.sleep(1000);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
                    }
                }

                Thread.sleep(3_000); // Wait longer for download options to appear
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

            try {
                // Create filename from the series URL
                String fileName = document.baseUri().substring(document.baseUri().lastIndexOf('/') + 1);
                if (fileName.isEmpty()) {
                    throw new IOException("Cannot infer filename from URL: " + document.baseUri());
                }
                fileName += ".csv";

                // Create full file path
                Path destFile = Paths.get(downloadFolder, fileName);
                String filePath = destFile.toString();

                // Use the FileDownloader interface to download the file
                downloadFile(csvUrl, filePath);

            } catch (Exception e) {
                System.err.println("Error downloading CSV: " + e.getMessage());
            }
        }

        @Override
        protected boolean isValid(String url) {
            return url != null
                    && url.startsWith("https://fred.stlouisfed.org/series/");
        }
    }
}
