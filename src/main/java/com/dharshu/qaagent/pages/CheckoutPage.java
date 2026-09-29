package com.dharshu.qaagent.pages;

import com.dharshu.qaagent.core.SmartLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

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
        // Wait for the checkout-info page to actually be present before typing --
        // avoids a race where the click that got us here hasn't finished rendering yet.
        firstNameField.find(driver).sendKeys(firstName);
        lastNameField.find(driver).sendKeys(lastName);
        postalCodeField.find(driver).sendKeys(postalCode);
        continueButton.find(driver).click();
    }

    /** Clicks Continue with every field left blank, to trigger SauceDemo's validation error. */
    public void submitEmptyForm() {
        continueButton.find(driver).click();
    }

    public String getCheckoutErrorText() {
        return checkoutErrorMessage.find(driver).getText();
    }

    public void finishOrder() {
        finishButton.find(driver).click();
    }

    public double getDisplayedTotal() {
        String text = totalLabel.find(driver).getText();
        String numeric = text.replaceAll("[^0-9.]", "");
        return Double.parseDouble(numeric);
    }
}
