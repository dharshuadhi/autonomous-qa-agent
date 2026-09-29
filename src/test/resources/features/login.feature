Feature: Login to SauceDemo

  Scenario: Successful login with a standard user
    Given I am on the SauceDemo login page
    When I log in with username "standard_user" and password "secret_sauce"
    Then I should see the products page

  Scenario: Login is blocked for a locked out user
    Given I am on the SauceDemo login page
    When I log in with username "locked_out_user" and password "secret_sauce"
    Then I should see an error message containing "locked out"

  Scenario: Login fails with a wrong password
    Given I am on the SauceDemo login page
    When I log in with username "standard_user" and password "wrong_password"
    Then I should see an error message containing "do not match"
