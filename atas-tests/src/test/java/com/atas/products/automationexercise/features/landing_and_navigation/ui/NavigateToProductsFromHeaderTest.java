package com.atas.products.automationexercise.features.landing_and_navigation.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.HomePage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Landing and Navigation")
@Feature("Header Navigation")
@Tag(TestTags.UI)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class NavigateToProductsFromHeaderTest extends UiTestHooks {

  private HomePage homePage;

  // Hooks manage browser/page

  @Test
  @DisplayName("Header Products link navigates to /products")
  @Story("Navigate to Products from header")
  void testNavigateToProducts() {
    homePage = new HomePage(page);
    homePage.gotoPage().navigateToProducts();
    assertTrue(page.url().contains("/products"));
  }
}
