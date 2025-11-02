package com.atas.products.automationexercise.features.product_catalog_and_filters.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.ProductsPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.PRODUCTS)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class ViewProductDetailsPageTest extends UiTestHooks {

  @Test
  void viewDetailsOfFirstProduct() {
    new ProductsPage(page).gotoPage().openFirstProductDetails();
    assertTrue(page.url().contains("/product_details"));
  }
}
