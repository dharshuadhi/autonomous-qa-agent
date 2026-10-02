package com.dharshu.qaagent.pages;

import com.dharshu.qaagent.core.SmartLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class CheckoutPage {

    private final WebDriver driver;

    private final SmartLocator firstNameField = SmartLocator.named("first name field")
            .primary(By.id("first-name"))
            .fallback(By.cssSelector("[data-test='firstName']"))
            .build();

    private final SmartLocator lastNameField = SmartLocator.named("last name field")
            .primary(By.id("last-name"))
            .fallback(By.cssSelector("[data-test='lastName']"))
            .build();

    private final SmartLocator postalCodeField = SmartLocator.named("postal code field")
            .primary(By.id("postal-code"))
            .fallback(By.cssSelector("[data-test='postalCode']"))
            .build();

    private final SmartLocator continueButton = SmartLocator.named("continue button")
            .primary(By.id("continue"))
            .fallback(By.cssSelector("[data-test='continue']"))
            .build();

    private final SmartLocator finishButton = SmartLocator.named("finish button")
            .primary(By.id("finish"))
            .fallback(By.cssSelector("[data-test='finish']"))
            .build();

    private final SmartLocator totalLabel = SmartLocator.named("total label")
            .primary(By.className("summary_total_label"))
            .fallback(By.xpath("//div[contains(text(),'Total')]"))
            .build();

    private final SmartLocator checkoutErrorMessage = SmartLocator.named("checkout error message")
            .primary(By.cssSelector("[data-test='error']"))
            .fallback(By.className("error-message-container"))
            .build();

    public CheckoutPage(WebDriver driver) {
        this.driver = driver;
    }

    public void fillInfo(String firstName, String lastName, String postalCode) {
        typeReliably(firstNameField, firstName);
        typeReliably(lastNameField, lastName);
        typeReliably(postalCodeField, postalCode);
        continueButton.click(driver);
        // The total label only exists on the overview page. Without this wait,
        // getDisplayedTotal() races the step-one -> step-two navigation and flakes
        // on slow runners when the overview page takes more than a few seconds.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.urlContains("checkout-step-two"));
        } catch (org.openqa.selenium.TimeoutException e) {
            // TEMPORARY CI DEBUG: dump form state to diagnose the stuck navigation.
            try {
                String debug = "URL=" + driver.getCurrentUrl()
                        + " | first-name='" + driver.findElement(By.id("first-name")).getAttribute("value") + "'"
                        + " | last-name='" + driver.findElement(By.id("last-name")).getAttribute("value") + "'"
                        + " | postal-code='" + driver.findElement(By.id("postal-code")).getAttribute("value") + "'"
                        + " | react-first-name='" + ((JavascriptExecutor) driver).executeScript(
                                "return document.querySelector(\"[data-test='firstName']\").value") + "'"
                        + " | error-container=" + driver.findElements(By.cssSelector("[data-test='error']")).size();
                java.nio.file.Files.writeString(java.nio.file.Path.of("target/checkout-debug.txt"), debug);
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    /**
     * Types into a field and verifies the value stuck. Presence in the DOM is
     * not enough: right after navigation the input can exist while React is
     * still hydrating, and keys typed into that half-rendered input are wiped
     * on re-render -- leaving the form empty and the Continue click blocked
     * by validation. So we wait for true interactability first and re-type
     * once if verification shows the value didn't stick.
     */
    private void typeReliably(SmartLocator field, String value) {
        for (int attempt = 0; attempt < 3; attempt++) {
            WebElement element = field.find(driver);
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(element));
            element.clear();
            element.sendKeys(value);
            if (value.equals(element.getAttribute("value"))) {
                return;
            }
            // Value didn't stick -- re-resolve (the element may have been
            // re-rendered) and retry.
        }
        throw new IllegalStateException(
                "Could not type value into field after 3 attempts (React hydration race).");
    }

    /** Clicks Continue with every field left blank, to trigger SauceDemo's validation error. */
    public void submitEmptyForm() {
        continueButton.click(driver);
    }

    public String getCheckoutErrorText() {
        return checkoutErrorMessage.find(driver).getText();
    }

    public void finishOrder() {
        finishButton.click(driver);
    }

    public double getDisplayedTotal() {
        String text = totalLabel.find(driver).getText();
        String numeric = text.replaceAll("[^0-9.]", "");
        return Double.parseDouble(numeric);
    }
}
