package com.atas.products.automationexercise.features.landing_and_navigation.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.HomePage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class NavigateToTestCasesFromHeaderTest extends UiTestHooks {

  @Test
  void navigateToTestCases() {
    new HomePage(page).gotoPage().navigateToTestCases();
    assertTrue(page.url().contains("/test_cases"));
  }
}
