package com.dharshu.qaagent.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

public class CheckoutSteps {

    private final TestContext ctx = new TestContext();

    // SauceDemo's fixed catalog prices -- used to independently verify the displayed total.
    private static final double BACKPACK_PRICE = 29.99;
    private static final double BIKE_LIGHT_PRICE = 9.99;
    private static final double TAX_RATE = 0.08;

    @When("I complete checkout with {string} {string} {string}")
    public void i_complete_checkout_with(String firstName, String lastName, String postalCode) {
        ctx.inventoryPage.goToCart();
        ctx.cartPage.startCheckout();
        ctx.checkoutPage.fillInfo(firstName, lastName, postalCode);
    }

    @Then("the order total should equal the sum of the item prices plus tax")
    public void the_order_total_should_be_correct() {
        double expectedSubtotal = BACKPACK_PRICE + BIKE_LIGHT_PRICE;
        double expectedTotal = Math.round(expectedSubtotal * (1 + TAX_RATE) * 100.0) / 100.0;
        double actualTotal = ctx.checkoutPage.getDisplayedTotal();
        Assert.assertEquals(expectedTotal, actualTotal, 0.05);
    }
}
