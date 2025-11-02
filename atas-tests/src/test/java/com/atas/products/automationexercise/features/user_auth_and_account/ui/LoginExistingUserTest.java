package com.atas.products.automationexercise.features.user_auth_and_account.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.LoginPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("User Authentication and Account")
@Feature("User Login")
@Tag(TestTags.UI)
@Tag(TestTags.AUTH)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P0)
public class LoginExistingUserTest extends UiTestHooks {

  private LoginPage loginPage;

  // Hooks manage browser/page

  @Test
  @DisplayName("Verify login page loads correctly")
  @Story("Login page functionality")
  void testLoginPageLoads() {
    loginPage = new LoginPage(page);
    loginPage.gotoPage();
    assertTrue(loginPage.isPageLoaded(), "Login page should load successfully");
  }
}
