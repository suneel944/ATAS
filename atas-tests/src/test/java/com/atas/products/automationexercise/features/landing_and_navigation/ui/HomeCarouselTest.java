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
@Feature("Home Page Carousel")
@Tag(TestTags.UI)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class HomeCarouselTest extends UiTestHooks {

  private HomePage homePage;

  // Hooks manage browser/page

  @Test
  @DisplayName("Verify carousel is visible on home page")
  @Story("Home page carousel visibility")
  void testCarouselIsVisible() {
    homePage = new HomePage(page);
    homePage.gotoPage();
    assertTrue(homePage.isCarouselVisible(), "Carousel should be visible on home page");
  }
}
