package com.atas.products.automationexercise.features.testcases_reference.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class TestCasesMetadataApiExposureTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify test cases page returns 200 OK")
  void canFetchTestCasesPageAsHealth() {
    FluentApiRequest api = apiForService("automationexercise");
    int status = api.endpoint("/test_cases").get().expectStatus(200).getStatus();
    assertEquals(200, status, "Test cases page should return 200 OK");
  }
}
