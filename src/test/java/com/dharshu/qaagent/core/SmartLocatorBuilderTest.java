package com.dharshu.qaagent.core;

import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for the SmartLocator builder contract. No browser needed --
 * these pin down the fail-fast behavior when a locator is misconfigured.
 */
public class SmartLocatorBuilderTest {

    @Test
    public void builderRequiresAPrimaryLocator() {
        try {
            SmartLocator.named("username field")
                    .fallback(By.id("user-name"))
                    .build();
            fail("Expected IllegalStateException when no primary locator is set");
        } catch (IllegalStateException expected) {
            // expected -- a SmartLocator with only fallbacks is a configuration bug
        }
    }

    @Test
    public void builderAcceptsPrimaryOnly() {
        SmartLocator locator = SmartLocator.named("username field")
                .primary(By.id("user-name"))
                .build();
        assertNotNull(locator);
    }

    @Test
    public void builderAcceptsPrimaryPlusFallbackChain() {
        SmartLocator locator = SmartLocator.named("login button")
                .primary(By.id("login-button"))
                .fallback(By.cssSelector("[data-test='login-button']"))
                .fallback(By.xpath("//input[@type='submit']"))
                .build();
        assertNotNull(locator);
    }
}
