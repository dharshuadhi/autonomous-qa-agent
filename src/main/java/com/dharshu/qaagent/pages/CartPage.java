package com.dharshu.qaagent.pages;

import com.dharshu.qaagent.core.SmartLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CartPage {

    private final WebDriver driver;

    private final SmartLocator checkoutButton = SmartLocator.named("checkout button")
            .primary(By.id("checkout"))
            .fallback(By.cssSelector("[data-test='checkout']"))
            .build();

    public CartPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isCheckoutButtonVisible() {
        try {
            return checkoutButton.find(driver).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void removeItem(String productName) {
        String testId = "remove-" + productName.toLowerCase().replace(" ", "-");
        SmartLocator removeButton = SmartLocator.named("remove: " + productName)
                .primary(By.cssSelector("[data-test='" + testId + "']"))
                .fallback(By.xpath("//div[text()='" + productName + "']/ancestor::div[@class='cart_item']//button"))
                .build();
        removeButton.find(driver).click();
    }

    public void startCheckout() {
        checkoutButton.find(driver).click();
    }

    public List<WebElement> getLineItems() {
        return driver.findElements(By.className("cart_item"));
    }
}
