package com.dharshu.qaagent.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.openqa.selenium.NoSuchElementException;

public class CartSteps {

    private final TestContext ctx = new TestContext();

    @When("I add {string} to the cart")
    public void i_add_item_to_cart(String productName) {
        ctx.inventoryPage.addToCart(productName);
    }

    @When("I remove {string} from the cart")
    public void i_remove_item_from_cart(String productName) {
        ctx.inventoryPage.goToCart();
        ctx.cartPage.removeItem(productName);
    }

    @When("I go to the cart page")
    public void i_go_to_the_cart_page() {
        ctx.inventoryPage.goToCart();
    }

    @Then("the cart badge should show {string}")
    public void the_cart_badge_should_show(String expectedCount) {
        Assert.assertEquals(expectedCount, ctx.inventoryPage.getCartBadgeCount());
    }

    @Then("the cart badge should not be visible")
    public void the_cart_badge_should_not_be_visible() {
        try {
            ctx.inventoryPage.getCartBadgeCount();
            Assert.fail("Expected the cart badge to be gone, but it was still visible.");
        } catch (NoSuchElementException expected) {
            // badge correctly absent
        }
    }

    @Then("the checkout button should not be visible")
    public void the_checkout_button_should_not_be_visible() {
        Assert.assertFalse(ctx.cartPage.isCheckoutButtonVisible());
    }
}
