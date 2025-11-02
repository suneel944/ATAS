package com.atas.products.automationexercise.features.api_reference.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.ApiListPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class ApiListPageLoadsTest extends UiTestHooks {

  @Test
  void apiListLoads() {
    ApiListPage apiListPage = new ApiListPage(page);
    apiListPage.gotoPage();
    assertTrue(apiListPage.isPageLoaded());
  }
}
