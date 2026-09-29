package com.dharshu.qaagent.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Starts and stops a Chrome WebDriver. Selenium Manager (bundled with
 * Selenium 4.6+) resolves the matching chromedriver automatically, so no
 * manual driver download or path configuration is needed.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            ChromeOptions options = new ChromeOptions();
            if ("true".equalsIgnoreCase(System.getenv("HEADLESS"))) {
                options.addArguments("--headless=new");
            }
            options.addArguments("--window-size=1400,1000");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            WebDriver driver = new ChromeDriver(options);
            driverThreadLocal.set(driver);
        }
        return driverThreadLocal.get();
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }
}
