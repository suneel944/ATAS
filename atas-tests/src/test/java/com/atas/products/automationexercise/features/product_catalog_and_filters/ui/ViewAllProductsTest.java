package com.atas.products.automationexercise.features.product_catalog_and_filters.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.products.automationexercise.pages.ProductsPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.utils.TestUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Product Catalog and Filters")
@Feature("Product Listing")
@Tag(TestTags.UI)
@Tag(TestTags.PRODUCTS)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class ViewAllProductsTest {

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  private Page page;
  private ProductsPage productsPage;
  private TestExecution testExecution;

  @BeforeEach
  void setUp() {
    testExecution = TestUtils.createTestExecution("product_catalog_and_filters", "ui");
    playwright = Playwright.create();
    browser = playwright.chromium().launch();
    context = browser.newContext();
    page = context.newPage();
    productsPage = new ProductsPage(page);
  }

  @AfterEach
  void tearDown() {
    if (context != null) context.close();
    if (browser != null) browser.close();
    if (playwright != null) playwright.close();
  }

  @Test
  @DisplayName("Verify all products page loads correctly")
  @Story("Product listing display")
  void testAllProductsPageLoads() {
    TestResult testResult =
        TestUtils.createTestResult(
            testExecution,
            TestUtils.generateTestId("products-page-load"),
            "Verify all products page loads correctly",
            TestStatus.RUNNING);

    productsPage.gotoPage();
    assertTrue(productsPage.isPageLoaded(), "Products page should load successfully");
    assertTrue(productsPage.getProductCount() > 0, "Should display at least one product");
    testResult.setStatus(TestStatus.PASSED);
    TestUtils.logTestResult(testResult);
  }
}
