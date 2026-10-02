package com.dharshu.qaagent.core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the per-run self-healing counters. No browser needed --
 * these exercise the reporting half of the self-healing design.
 */
public class HealingReportTest {

    @Before
    public void resetCounters() {
        HealingReport.reset();
    }

    @Test
    public void countersStartAtZero() {
        assertEquals(0, HealingReport.getHealedCount());
    }

    @Test
    public void primaryResolutionsDoNotCountAsHealed() {
        HealingReport.recordPrimary("username field");
        HealingReport.recordPrimary("password field");
        assertEquals(0, HealingReport.getHealedCount());
    }

    @Test
    public void healedResolutionsAreCounted() {
        HealingReport.recordPrimary("username field");
        HealingReport.recordHealed("login button", "By.cssSelector: [data-test='login-button']");
        HealingReport.recordHealed("login button", "By.xpath: //input[@type='submit']");
        assertEquals(2, HealingReport.getHealedCount());
    }

    @Test
    public void failuresDoNotCountAsHealed() {
        HealingReport.recordFailure("ghost element");
        assertEquals(0, HealingReport.getHealedCount());
    }

    @Test
    public void printSummaryDoesNotThrowOnEmptyReport() {
        HealingReport.printSummary();
    }
}
