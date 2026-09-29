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

    public CheckoutPage(WebDriver driver) {
        this.driver = driver;
    }

    public void fillInfo(String firstName, String lastName, String postalCode) {
        firstNameField.find(driver).sendKeys(firstName);
        lastNameField.find(driver).sendKeys(lastName);
        postalCodeField.find(driver).sendKeys(postalCode);
        continueButton.find(driver).click();
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
