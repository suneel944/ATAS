package com.atas.shared.testing;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.api.FrameworkApiRequestContextHelper;
import com.atas.shared.utility.BaseUrlResolver;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class ApiTestHooks {

  protected APIRequestContext request;
  private String apiSessionId;
  private Playwright playwright;
  private String frameworkBaseUrl;

  @BeforeEach
  void apiSetUp() {
    frameworkBaseUrl = BaseUrlResolver.resolveFrameworkBaseUrl();

    var result = FrameworkApiRequestContextHelper.createApiRequestContext(frameworkBaseUrl, "");

    request = result.apiRequestContext();
    apiSessionId = result.sessionId();
    playwright = result.playwright();
  }

  @AfterEach
  void apiTearDown() {
    Optional.ofNullable(apiSessionId)
        .filter(s -> !s.isBlank())
        .ifPresent(
            sid ->
                Optional.ofNullable(frameworkBaseUrl)
                    .filter(url -> !url.isBlank())
                    .ifPresent(url -> FrameworkApiRequestContextHelper.closeApiSession(url, sid)));

    Optional.ofNullable(request).ifPresent(APIRequestContext::dispose);
    Optional.ofNullable(playwright).ifPresent(Playwright::close);
  }

  /**
   * Creates a FluentApiRequest instance for the specified service.
   *
   * @param serviceName Service name (e.g., "admin-dash-service", "order-service")
   * @return FluentApiRequest configured for the service
   */
  protected FluentApiRequest apiForService(String serviceName) {
    String baseUrl = BaseUrlResolver.resolveService(serviceName);
    return new FluentApiRequest(request, baseUrl);
  }

  /**
   * Creates a FluentApiRequest instance for the example 1 service.
   *
   * @return FluentApiRequest configured for example 1 service
   */
  protected FluentApiRequest example1Api() {
    return apiForService("example-1-service");
  }

  /**
   * Creates a FluentApiRequest instance for the example 2 service.
   *
   * @return FluentApiRequest configured for example 2 service
   */
  protected FluentApiRequest example2Api() {
    return apiForService("example-2-service");
  }

  /**
   * Creates a FluentApiRequest instance for the example 3 service.
   *
   * @return FluentApiRequest configured for example 3 service
   */
  protected FluentApiRequest example3Api() {
    return apiForService("example-3-service");
  }
}
