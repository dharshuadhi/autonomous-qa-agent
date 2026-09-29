package com.dharshu.qaagent.steps;

import com.dharshu.qaagent.core.DriverManager;
import com.dharshu.qaagent.pages.*;
import org.openqa.selenium.WebDriver;

/** One instance per scenario (Cucumber re-injects picocontainer-style; kept simple here). */
public class TestContext {
    public final WebDriver driver = DriverManager.getDriver();
    public final LoginPage loginPage = new LoginPage(driver);
    public final InventoryPage inventoryPage = new InventoryPage(driver);
    public final CartPage cartPage = new CartPage(driver);
    public final CheckoutPage checkoutPage = new CheckoutPage(driver);
}
