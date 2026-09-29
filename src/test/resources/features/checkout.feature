Feature: Checkout

  Background:
    Given I am logged in as "standard_user"

  Scenario: Complete checkout and see the correct total
    When I add "Sauce Labs Backpack" to the cart
    And I add "Sauce Labs Bike Light" to the cart
    And I complete checkout with "Dharshu" "A" "60067"
    Then the order total should equal the sum of the item prices plus tax

  Scenario: Checkout cannot proceed with an empty cart
    When I go to the cart page
    Then the checkout button should not be visible
