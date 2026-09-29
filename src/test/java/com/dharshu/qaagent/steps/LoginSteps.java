package com.dharshu.qaagent.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

public class LoginSteps {

    private final TestContext ctx = new TestContext();

    @Given("I am on the SauceDemo login page")
    public void i_am_on_the_login_page() {
        ctx.loginPage.open();
    }

    @Given("I am logged in as {string}")
    public void i_am_logged_in_as(String username) {
        ctx.loginPage.open();
        ctx.loginPage.login(username, "secret_sauce");
    }

    @When("I log in with username {string} and password {string}")
    public void i_log_in_with(String username, String password) {
        ctx.loginPage.login(username, password);
    }

    @Then("I should see the products page")
    public void i_should_see_products_page() {
        Assert.assertTrue("Expected the inventory page to be loaded", ctx.inventoryPage.isLoaded());
    }

    @Then("I should see an error message containing {string}")
    public void i_should_see_error_containing(String expectedSubstring) {
        String error = ctx.loginPage.getErrorText().toLowerCase();
        Assert.assertTrue(
                "Expected error to contain \"" + expectedSubstring + "\" but was: " + error,
                error.contains(expectedSubstring.toLowerCase()));
    }
}
