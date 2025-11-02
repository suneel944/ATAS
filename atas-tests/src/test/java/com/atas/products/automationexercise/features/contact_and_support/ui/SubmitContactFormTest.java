package com.atas.products.automationexercise.features.contact_and_support.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.ContactPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.CONTACT)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class SubmitContactFormTest extends UiTestHooks {

  @Test
  void contactPageLoads() {
    ContactPage contactPage = new ContactPage(page);
    contactPage.gotoPage();
    assertTrue(contactPage.isContactFormVisible());
  }
}
