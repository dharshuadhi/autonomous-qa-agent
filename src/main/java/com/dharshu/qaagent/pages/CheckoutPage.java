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
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains("checkout-step-two"));
    }

    /**
     * Types into a field and verifies the value stuck. Presence in the DOM is
     * not enough: right after navigation the input can exist while React is
     * still hydrating, and keys typed into that half-rendered input are wiped
     * on re-render -- leaving the form empty and the Continue click blocked
     * by validation. So we write via React's native value setter (so its
     * change tracking picks it up) with a sendKeys fallback, and verify
     * through the DOM property, retrying on a freshly resolved element.
     */
    private void typeReliably(SmartLocator field, String value) {
        for (int attempt = 0; attempt < 3; attempt++) {
            WebElement element = field.find(driver);
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(element));
            setValueReactAware(element, value);
            if (value.equals(getValueProperty(element))) {
                return;
            }
            try {
                element.clear();
                element.sendKeys(value);
            } catch (Exception ignored) {
            }
            if (value.equals(getValueProperty(element))) {
                return;
            }
            // Value didn't stick -- loop re-resolves (the element may have
            // been re-rendered) and retries.
        }
        throw new IllegalStateException(
                "Could not type value into field after 3 attempts.");
    }

    private String getValueProperty(WebElement element) {
        return (String) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].value;", element);
    }

    private void setValueReactAware(WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                        + "setter.call(arguments[0], arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
                        + "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                element, value);
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
