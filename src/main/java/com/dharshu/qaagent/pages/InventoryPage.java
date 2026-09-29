package com.dharshu.qaagent.pages;

import com.dharshu.qaagent.core.SmartLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class InventoryPage {

    private final WebDriver driver;

    private final SmartLocator inventoryContainer = SmartLocator.named("inventory container")
            .primary(By.id("inventory_container"))
            .fallback(By.className("inventory_list"))
            .build();

    private final SmartLocator cartBadge = SmartLocator.named("cart badge")
            .primary(By.cssSelector(".shopping_cart_badge"))
            .fallback(By.xpath("//a[contains(@class,'shopping_cart_link')]//span"))
            .build();

    private final SmartLocator cartLink = SmartLocator.named("cart link")
            .primary(By.cssSelector(".shopping_cart_link"))
            .fallback(By.id("shopping_cart_container"))
            .build();

    public InventoryPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isLoaded() {
        return inventoryContainer.find(driver).isDisplayed();
    }

    private By addToCartButtonFor(String productName) {
        String testId = "add-to-cart-" + productName.toLowerCase().replace(" ", "-");
        return By.cssSelector("[data-test='" + testId + "']");
    }

    public void addToCart(String productName) {
        SmartLocator button = SmartLocator.named("add to cart: " + productName)
                .primary(addToCartButtonFor(productName))
                .fallback(By.xpath("//div[text()='" + productName + "']/ancestor::div[@class='inventory_item']//button"))
                .build();
        button.find(driver).click();
    }

    public String getCartBadgeCount() {
        return cartBadge.find(driver).getText();
    }

    public void goToCart() {
        cartLink.find(driver).click();
    }
}
