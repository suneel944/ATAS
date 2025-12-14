package com.atas.shared.email;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.utility.TestDataUtility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for GuerrillaMail temporary email service integration.
 *
 * <p>GuerrillaMail provides temporary email addresses that can be used for testing purposes. This
 * helper manages the email session and provides methods to:
 *
 * <ul>
 *   <li>Generate new temporary email addresses
 *   <li>Reset/clear email sessions
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * GuerrillaMailHelper emailHelper = new GuerrillaMailHelper(request);
 * String email = emailHelper.generateEmailAddress();
 * // Use email for testing...
 * }</pre>
 */
@Slf4j
public class GuerrillaMailHelper {

  private static final Properties TEST_CONFIG =
      TestDataUtility.loadProperties("test-config.properties");
  private static final String GUERRILLA_MAIL_BASE_URL =
      TestDataUtility.getProperty("GUERRILLA_MAIL_BASE_URL", TEST_CONFIG);
  private static final String GUERRILLA_MAIL_USER_AGENT =
      TestDataUtility.getProperty("GUERRILLA_MAIL_USER_AGENT", TEST_CONFIG);

  private final APIRequestContext request;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS_TYPE =
      new TypeReference<>() {};
  private static final Pattern INVITATION_LINK_PATTERN =
      Pattern.compile(
          "https://[^/]+\\.auth0\\.com/invitation/[^\\s\"<>]+", Pattern.CASE_INSENSITIVE);

  private String currentEmail;
  private String sidToken;

  public GuerrillaMailHelper(APIRequestContext request) {
    this.request = request;
  }

  /**
   * Generates a new temporary email address from GuerrillaMail.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>First clears any existing session (forget_me)
   *   <li>Requests a new email address
   *   <li>Extracts and stores the email and session token
   * </ol>
   *
   * @return Temporary email address (e.g., "abc123@guerrillamail.com")
   */
  public String generateEmailAddress() {
    try {
      forgetMe();

      var ipAddress = getClientIpAddress();
      var queryParams =
          "f=get_email_address&ip=%s&agent=%s"
              .formatted(
                  java.net.URLEncoder.encode(ipAddress, StandardCharsets.UTF_8),
                  java.net.URLEncoder.encode(GUERRILLA_MAIL_USER_AGENT, StandardCharsets.UTF_8));
      var endpoint = "/ajax.php?" + queryParams;

      var api = new FluentApiRequest(request, GUERRILLA_MAIL_BASE_URL);
      var response =
          api.endpoint(endpoint).withHeader("User-Agent", GUERRILLA_MAIL_USER_AGENT).get();

      if (response.getStatus() != 200) {
        log.error(
            "Failed to generate email address. Status: {}, Response: {}",
            response.getStatus(),
            response.asString());
        throw new RuntimeException(
            "Failed to generate GuerrillaMail email address: " + response.getStatus());
      }

      var emailResponse = response.asMap();
      currentEmail =
          Optional.ofNullable(emailResponse.get("email_addr"))
              .map(String.class::cast)
              .filter(s -> !s.isBlank())
              .orElseThrow(
                  () -> new RuntimeException("GuerrillaMail did not return a valid email address"));

      sidToken =
          Optional.ofNullable(emailResponse.get("sid_token")).map(String.class::cast).orElse(null);

      log.info("Generated temporary email address: {}", currentEmail);
      return currentEmail;

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error generating GuerrillaMail email address", e);
      throw new RuntimeException("Failed to generate temporary email address", e);
    }
  }

  /**
   * Clears/resets the current GuerrillaMail session.
   *
   * <p>This is useful when you want to start fresh or ensure no previous session data exists.
   */
  public void forgetMe() {
    try {
      var api = new FluentApiRequest(request, GUERRILLA_MAIL_BASE_URL);
      var response =
          api.endpoint("/ajax.php?f=forget_me")
              .withHeader("User-Agent", GUERRILLA_MAIL_USER_AGENT)
              .get();

      if (response.getStatus() == 200) {
        currentEmail = null;
        sidToken = null;
      } else {
        log.warn(
            "Failed to clear GuerrillaMail session. Status: {}, Response: {}",
            response.getStatus(),
            response.asString());
      }
    } catch (Exception e) {
      log.warn("Error clearing GuerrillaMail session", e);
    }
  }

  /**
   * Gets the current email address if one has been generated.
   *
   * @return Current email address, or null if none has been generated
   */
  public String getCurrentEmail() {
    return currentEmail;
  }

  /**
   * Gets the current session token if one exists.
   *
   * @return Current session token, or null if none exists
   */
  public String getSidToken() {
    return sidToken;
  }

  /**
   * Gets the list of emails in the inbox.
   *
   * @param offset Offset for pagination (default: 0)
   * @return List of email summaries (id, from, subject, received, etc.)
   */
  public List<Map<String, Object>> getEmailList(int offset) {
    if (sidToken == null) {
      throw new IllegalStateException("No session token. Call generateEmailAddress() first.");
    }

    try {
      var endpoint = "/ajax.php?f=get_email_list&offset=" + offset + "&sid_token=" + sidToken;
      var api = new FluentApiRequest(request, GUERRILLA_MAIL_BASE_URL);
      var response =
          api.endpoint(endpoint).withHeader("User-Agent", GUERRILLA_MAIL_USER_AGENT).get();

      if (response.getStatus() != 200) {
        log.error(
            "Failed to get email list. Status: {}, Response: {}",
            response.getStatus(),
            response.asString());
        throw new RuntimeException(
            "Failed to get GuerrillaMail email list: " + response.getStatus());
      }

      var responseMap = response.asMap();
      JsonNode responseNode = objectMapper.valueToTree(responseMap);
      JsonNode listNode =
          Optional.ofNullable(responseNode.get("list")).filter(JsonNode::isArray).orElse(null);

      if (listNode == null) {
        return List.of();
      }

      return objectMapper.convertValue(listNode, LIST_OF_MAPS_TYPE);
    } catch (Exception e) {
      log.error("Error getting email list", e);
      throw new RuntimeException("Failed to get email list", e);
    }
  }

  /**
   * Gets the list of emails in the inbox (offset 0).
   *
   * @return List of email summaries
   */
  public List<Map<String, Object>> getEmailList() {
    return getEmailList(0);
  }

  /**
   * Fetches the full content of a specific email.
   *
   * @param emailId Email ID from the email list
   * @return Email content as Map (mail_id, mail_from, mail_subject, mail_body, etc.)
   */
  public Map<String, Object> fetchEmail(String emailId) {
    if (sidToken == null) {
      throw new IllegalStateException("No session token. Call generateEmailAddress() first.");
    }

    try {
      var endpoint = "/ajax.php?f=fetch_email&email_id=" + emailId + "&sid_token=" + sidToken;
      var api = new FluentApiRequest(request, GUERRILLA_MAIL_BASE_URL);
      var response =
          api.endpoint(endpoint).withHeader("User-Agent", GUERRILLA_MAIL_USER_AGENT).get();

      if (response.getStatus() != 200) {
        log.error(
            "Failed to fetch email. Status: {}, Response: {}",
            response.getStatus(),
            response.asString());
        throw new RuntimeException("Failed to fetch GuerrillaMail email: " + response.getStatus());
      }

      return response.asMap();
    } catch (Exception e) {
      log.error("Error fetching email", e);
      throw new RuntimeException("Failed to fetch email", e);
    }
  }

  /**
   * Waits for an email matching the given criteria and returns it.
   *
   * @param fromEmail Optional sender email to filter by
   * @param subjectContains Optional subject text to filter by
   * @param maxWaitSeconds Maximum time to wait in seconds (default: 60)
   * @return Email content as Map, or null if not found
   */
  public Map<String, Object> waitForEmail(
      String fromEmail, String subjectContains, int maxWaitSeconds) {
    var startTime = System.currentTimeMillis();
    var maxWaitMillis = Duration.ofSeconds(maxWaitSeconds).toMillis();

    while (System.currentTimeMillis() - startTime < maxWaitMillis) {
      var emails = getEmailList();

      for (var emailSummary : emails) {
        var emailId = String.valueOf(emailSummary.get("mail_id"));
        var email = fetchEmail(emailId);

        var emailFrom = Optional.ofNullable(email.get("mail_from")).map(String::valueOf).orElse("");
        var emailSubject =
            Optional.ofNullable(email.get("mail_subject")).map(String::valueOf).orElse("");

        boolean matches = true;
        if (fromEmail != null && !fromEmail.isBlank()) {
          matches = matches && emailFrom.contains(fromEmail);
        }
        if (subjectContains != null && !subjectContains.isBlank()) {
          matches = matches && emailSubject.contains(subjectContains);
        }

        if (matches) {
          log.info("Found matching email: from={}, subject={}", emailFrom, emailSubject);
          return email;
        }
      }

      try {
        Thread.sleep(2000); // Wait 2 seconds before checking again
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for email", e);
      }
    }

    log.warn(
        "Email not found after waiting {} seconds (from={}, subjectContains={})",
        maxWaitSeconds,
        fromEmail,
        subjectContains);
    return null;
  }

  /**
   * Extracts invitation link from email body.
   *
   * @param email Email Map from fetchEmail()
   * @return Invitation URL, or null if not found
   */
  public Optional<String> extractInvitationLink(Map<String, Object> email) {
    var mailBody = Optional.ofNullable(email.get("mail_body")).map(String::valueOf).orElse("");

    var matcher = INVITATION_LINK_PATTERN.matcher(mailBody);
    if (matcher.find()) {
      return Optional.of(matcher.group());
    }

    // Also check mail_excerpt (preview text)
    var mailExcerpt =
        Optional.ofNullable(email.get("mail_excerpt")).map(String::valueOf).orElse("");

    matcher = INVITATION_LINK_PATTERN.matcher(mailExcerpt);
    if (matcher.find()) {
      return Optional.of(matcher.group());
    }

    return Optional.empty();
  }

  /**
   * Waits for invitation email and extracts the invitation link.
   *
   * @param fromEmail Sender email (e.g., "noreply@auth0.com")
   * @param maxWaitSeconds Maximum time to wait in seconds
   * @return Invitation URL, or empty if not found
   */
  public Optional<String> waitForInvitationLink(String fromEmail, int maxWaitSeconds) {
    var email = waitForEmail(fromEmail, "invitation", maxWaitSeconds);
    if (email == null) {
      return Optional.empty();
    }
    return extractInvitationLink(email);
  }

  private String getClientIpAddress() {
    return TestDataUtility.getProperty("GUERRILLA_MAIL_CLIENT_IP_ADDRESS", TEST_CONFIG);
  }
}
