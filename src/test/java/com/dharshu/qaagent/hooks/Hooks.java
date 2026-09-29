package com.dharshu.qaagent.hooks;

import com.dharshu.qaagent.ai.RcaAnalyzer;
import com.dharshu.qaagent.core.DriverManager;
import com.dharshu.qaagent.core.HealingReport;
import com.dharshu.qaagent.integrations.AzureDevOpsClient;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;

public class Hooks {

    @After
    public void afterScenario(Scenario scenario) {
        WebDriver driver = DriverManager.getDriver();

        if (scenario.isFailed()) {
            handleFailure(scenario, driver);
        }

        DriverManager.quitDriver();
    }

    @AfterAll
    public static void afterAllScenarios() {
        HealingReport.printSummary();
    }

    /**
     * On failure: capture a screenshot + page HTML, attach both to the Allure
     * report, ask the LLM for a plain-English root cause (Step 8), and -- if
     * confidence is high or medium -- file a bug in Azure DevOps (Step 9).
     *
     * Wrapped in a broad try/catch so that a missing LLM_API_KEY / ADO_PAT
     * locally (e.g. before those secrets are configured) never crashes the
     * whole suite -- it just skips the AI/bug-filing step and logs why.
     */
    private void handleFailure(Scenario scenario, WebDriver driver) {
        byte[] screenshot = null;
        String html = "";

        try {
            screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "failure-screenshot");
            html = driver.getPageSource();
        } catch (Exception e) {
            System.err.println("Could not capture screenshot/HTML: " + e.getMessage());
        }

        try {
            RcaAnalyzer analyzer = new RcaAnalyzer();
            RcaAnalyzer.RcaResult rca = analyzer.analyze(scenario.getName(), stackTraceOf(scenario), html);

            Allure.addAttachment("AI Root Cause Analysis", "text/plain", rca.explanation);
            System.out.println("[RCA] " + scenario.getName() + " -> " + rca.explanation);

            if (rca.confidence == RcaAnalyzer.Confidence.HIGH
                    || rca.confidence == RcaAnalyzer.Confidence.MEDIUM) {
                fileBug(scenario, rca, screenshot);
            } else {
                System.out.println("[BUG FILING] Skipped -- RCA confidence too low: " + rca.confidence);
            }
        } catch (IllegalStateException e) {
            System.out.println("[RCA] Skipped -- " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[RCA] Failed: " + e.getMessage());
        }
    }

    private void fileBug(Scenario scenario, RcaAnalyzer.RcaResult rca, byte[] screenshot) {
        try {
            AzureDevOpsClient ado = new AzureDevOpsClient();
            String title = "[Auto-filed] " + scenario.getName() + " is failing";
            String description = "Automatically filed by the QA agent.\n\n"
                    + "Scenario: " + scenario.getName() + "\n\n"
                    + "AI root cause analysis:\n" + rca.explanation;
            ado.createBug(title, description, screenshot);
        } catch (IllegalStateException e) {
            System.out.println("[BUG FILING] Skipped -- " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[BUG FILING] Failed: " + e.getMessage());
        }
    }

    private String stackTraceOf(Scenario scenario) {
        // Cucumber's Scenario doesn't expose the raw Throwable directly in all versions;
        // this pulls whatever failure text is available for the prompt.
        return "Scenario \"" + scenario.getName() + "\" failed. Status: " + scenario.getStatus();
    }
}
