package com.dharshu.qaagent.core;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps element lookup with a primary locator plus an ordered fallback chain.
 * If the primary locator fails (e.g. an id was renamed), each fallback is tried
 * in order. Every resolution is logged via HealingReport so a run's "self-healed"
 * count is measurable, not just claimed.
 */
public class SmartLocator {

    private final String elementName;
    private final By primary;
    private final List<By> fallbacks;
    private final Duration timeout;

    private SmartLocator(String elementName, By primary, List<By> fallbacks, Duration timeout) {
        this.elementName = elementName;
        this.primary = primary;
        this.fallbacks = fallbacks;
        this.timeout = timeout;
    }

    public static Builder named(String elementName) {
        return new Builder(elementName);
    }

    public WebElement find(WebDriver driver) {
        WebElement element = tryLocator(driver, primary, timeout);
        if (element != null) {
            HealingReport.recordPrimary(elementName);
            return element;
        }

        for (int i = 0; i < fallbacks.size(); i++) {
            By fallback = fallbacks.get(i);
            element = tryLocator(driver, fallback, Duration.ofSeconds(2));
            if (element != null) {
                HealingReport.recordHealed(elementName, fallback.toString());
                return element;
            }
        }

        HealingReport.recordFailure(elementName);
        throw new NoSuchElementException(
                "SmartLocator could not find element \"" + elementName + "\" using the primary "
                        + "locator or any of its " + fallbacks.size() + " fallback locator(s).");
    }

    /**
     * Clicks the element, waiting until it is actually clickable (visible and
     * enabled) instead of merely present in the DOM. Clicking a present-but-
     * not-yet-interactable element silently does nothing on a slow/loaded
     * page -- this was the source of flaky "add to cart" and navigation
     * clicks. Fallbacks are healed the same way as {@link #find(WebDriver)}.
     */
    public void click(WebDriver driver) {
        WebElement element = tryClickable(driver, primary, timeout);
        if (element != null) {
            HealingReport.recordPrimary(elementName);
            element.click();
            return;
        }

        for (By fallback : fallbacks) {
            element = tryClickable(driver, fallback, Duration.ofSeconds(2));
            if (element != null) {
                HealingReport.recordHealed(elementName, fallback.toString());
                element.click();
                return;
            }
        }

        HealingReport.recordFailure(elementName);
        throw new NoSuchElementException(
                "SmartLocator could not click element \"" + elementName + "\" using the primary "
                        + "locator or any of its " + fallbacks.size() + " fallback locator(s).");
    }

    private WebElement tryLocator(WebDriver driver, By locator, Duration timeout) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, timeout);
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            return null;
        }
    }

    private WebElement tryClickable(WebDriver driver, By locator, Duration timeout) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, timeout);
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (Exception e) {
            return null;
        }
    }

    public static class Builder {
        private final String elementName;
        private By primary;
        private final List<By> fallbacks = new ArrayList<>();
        private Duration timeout = Duration.ofSeconds(5);

        private Builder(String elementName) {
            this.elementName = elementName;
        }

        public Builder primary(By locator) {
            this.primary = locator;
            return this;
        }

        public Builder fallback(By locator) {
            this.fallbacks.add(locator);
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public SmartLocator build() {
            if (primary == null) {
                throw new IllegalStateException("SmartLocator \"" + elementName + "\" needs a primary locator.");
            }
            return new SmartLocator(elementName, primary, fallbacks, timeout);
        }
    }
}
