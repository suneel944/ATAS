package com.atas.products.automationexercise.features.testcases_reference.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.TestCasesPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class TestCasesPageLoadsAndListsScenariosTest extends UiTestHooks {

  @Test
  void testCasesPageLoads() {
    TestCasesPage testCasesPage = new TestCasesPage(page);
    testCasesPage.gotoPage();
    assertTrue(testCasesPage.isPageLoaded());
  }
}
