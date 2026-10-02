package com.dharshu.qaagent.pages;

import com.dharshu.qaagent.core.SmartLocator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage {

    private final WebDriver driver;
    private static final String URL = "https://www.saucedemo.com/";

    private final SmartLocator usernameField = SmartLocator.named("username field")
            .primary(By.id("user-name"))
            .fallback(By.cssSelector("[data-test='username']"))
            .fallback(By.xpath("//input[@placeholder='Username']"))
            .build();

    private final SmartLocator passwordField = SmartLocator.named("password field")
            .primary(By.id("password"))
            .fallback(By.cssSelector("[data-test='password']"))
            .fallback(By.xpath("//input[@placeholder='Password']"))
            .build();

    private final SmartLocator loginButton = SmartLocator.named("login button")
            .primary(By.id("login-button"))
            .fallback(By.cssSelector("[data-test='login-button']"))
            .fallback(By.xpath("//input[@type='submit']"))
            .build();

    private final SmartLocator errorMessage = SmartLocator.named("login error message")
            .primary(By.cssSelector("[data-test='error']"))
            .fallback(By.className("error-message-container"))
            .build();

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    public void open() {
        driver.get(URL);
    }

    public void login(String username, String password) {
        usernameField.find(driver).sendKeys(username);
        passwordField.find(driver).sendKeys(password);
        loginButton.click(driver);
    }

    public String getErrorText() {
        return errorMessage.find(driver).getText();
    }
}
