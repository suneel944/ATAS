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
public class NavigateToApiListFromHeaderTest extends UiTestHooks {

  @Test
  void navigateToApiList() {
    new HomePage(page).gotoPage().navigateToApiList();
    assertTrue(page.url().contains("/api_list"));
  }
}
