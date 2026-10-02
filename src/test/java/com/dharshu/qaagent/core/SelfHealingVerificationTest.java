package com.dharshu.qaagent.core;

import org.junit.After;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

import static org.junit.Assert.assertTrue;

/**
 * Proves the self-healing design against the real SauceDemo site: the primary
 * locator is deliberately wrong (simulating a UI change where a developer
 * renamed an element id), and the test only passes if the fallback chain
 * heals the lookup and HealingReport records it.
 *
 * Run: mvn -B test -Dtest=SelfHealingVerificationTest
 * (HEADLESS=true in CI; Chrome is installed by the workflow.)
 */
public class SelfHealingVerificationTest {

    private WebDriver driver;

    @After
    public void tearDown() {
        DriverManager.quitDriver();
        driver = null;
    }

    @Test
    public void brokenPrimaryLocatorIsHealedByFallbackChain() {
        driver = DriverManager.getDriver();
        driver.get("https://www.saucedemo.com/");

        int healedBefore = HealingReport.getHealedCount();

        // What the suite *thinks* the id is after a fictional UI change...
        SmartLocator usernameField = SmartLocator.named("username field (healing demo)")
                .primary(By.id("user-name-RENAMED-IN-LATEST-RELEASE"))
                .timeout(Duration.ofSeconds(3))
                // ...and the fallbacks that still match the real page.
                .fallback(By.cssSelector("[data-test='username']"))
                .fallback(By.xpath("//input[@placeholder='Username']"))
                .build();

        WebElement element = usernameField.find(driver);

        assertTrue("Fallback should have resolved a visible username field",
                element.isDisplayed());
        assertTrue("HealingReport should have recorded exactly one healed lookup",
                HealingReport.getHealedCount() == healedBefore + 1);
    }
}
