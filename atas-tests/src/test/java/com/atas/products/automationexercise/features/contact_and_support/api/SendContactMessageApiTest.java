package com.atas.products.automationexercise.features.contact_and_support.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.CONTACT)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class SendContactMessageApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify contact page returns 200 OK")
  void contactEndpointReachable() {
    FluentApiRequest api = apiForService("automationexercise");
    int status = api.endpoint("/contact_us").get().expectStatus(200).getStatus();
    assertEquals(200, status, "Contact page should return 200 OK");
  }
}
