package net.neological.gui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Manages the assisted mode functionality, allowing users to interact with a Chrome browser
 * and capture the final URL they navigate to.
 */
public class AssistedModeManager {
    private WebDriver driver;
    private boolean isActive = false;

    /**
     * Starts the assisted mode by launching a visible Chrome browser.
     *
     * @throws Exception if there's an error starting the browser
     */
    public void startAssistedMode() throws Exception {
        if (isActive) {
            throw new IllegalStateException("Assisted mode is already active");
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        // Start with a blank page or Google
        driver.get("https://www.google.com");

        isActive = true;
    }

    /**
     * Gets the current URL from the browser.
     *
     * @return the current URL, or null if assisted mode is not active
     */
    public String getCurrentUrl() {
        if (!isActive || driver == null) {
            return null;
        }

        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Stops the assisted mode and closes the browser.
     */
    public void stopAssistedMode() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Ignore errors when closing
            }
            driver = null;
        }
        isActive = false;
    }

    /**
     * Checks if assisted mode is currently active.
     *
     * @return true if assisted mode is active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }
}