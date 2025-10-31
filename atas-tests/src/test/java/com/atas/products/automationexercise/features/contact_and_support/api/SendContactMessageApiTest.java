package com.atas.products.automationexercise.features.contact_and_support.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.CONTACT)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class SendContactMessageApiTest extends ApiTestHooks {

  @Test
  void contactEndpointReachable() {
    APIResponse response = request.get("/contact_us");
    assertEquals(200, response.status());
  }
}
