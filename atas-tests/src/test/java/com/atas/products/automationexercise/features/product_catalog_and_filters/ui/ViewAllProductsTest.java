package com.atas.products.automationexercise.features.product_catalog_and_filters.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.ProductsPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
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
public class ViewAllProductsTest extends UiTestHooks {

  @Test
  @DisplayName("Verify all products page loads correctly")
  @Story("Product listing display")
  void testAllProductsPageLoads() {
    ProductsPage productsPage = new ProductsPage(page);
    productsPage.gotoPage();
    assertTrue(productsPage.isPageLoaded(), "Products page should load successfully");
    assertTrue(productsPage.getProductCount() > 0, "Should display at least one product");
  }
}
