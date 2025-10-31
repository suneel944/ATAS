package com.atas.products.automationexercise.features.product_catalog_and_filters.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.ProductsPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.PRODUCTS)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class FilterByBrandTest extends UiTestHooks {
  private ProductsPage productsPage;

  @Test
  void filterByPolo() {
    productsPage = new ProductsPage(page);
    productsPage.gotoPage();
    assertTrue(productsPage.isPageLoaded());
    productsPage.filterByPoloBrand();
    assertTrue(productsPage.getProductCount() > 0);
  }
}
