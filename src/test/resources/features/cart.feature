Feature: Shopping cart

  Background:
    Given I am logged in as "standard_user"

  Scenario: Add a single item to the cart
    When I add "Sauce Labs Backpack" to the cart
    Then the cart badge should show "1"

  Scenario: Add multiple items to the cart
    When I add "Sauce Labs Backpack" to the cart
    And I add "Sauce Labs Bike Light" to the cart
    And I add "Sauce Labs Bolt T-Shirt" to the cart
    Then the cart badge should show "3"

  Scenario: Remove an item from the cart
    When I add "Sauce Labs Backpack" to the cart
    And I remove "Sauce Labs Backpack" from the cart
    Then the cart badge should not be visible
