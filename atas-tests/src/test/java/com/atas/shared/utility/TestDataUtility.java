package com.atas.shared.utility;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for loading test configuration with a clear hierarchy.
 *
 * <p><strong>Configuration Hierarchy (highest to lowest priority):</strong>
 *
 * <ol>
 *   <li><strong>Environment Variables</strong> - Set in shell/CI/CD (e.g., export
 *       AUTH0_CLIENT_SECRET=xxx)
 *   <li><strong>System Properties</strong> - From .env file (loaded automatically) or
 *       -Dproperty=value
 *   <li><strong>Properties Files</strong> - Committed defaults in src/test/resources (non-secrets
 *       only)
 *   <li><strong>Default Values</strong> - Optional fallback values
 * </ol>
 *
 * <p><strong>When to use .env vs .properties:</strong>
 *
 * <ul>
 *   <li><strong>.env file</strong> - REQUIRED file for SECRETS only (NOT committed to git). Must
 *       exist in project root.
 *   <li><strong>.properties file</strong> - Use for NON-SECRET defaults and shared configuration
 *       (committed to git)
 * </ul>
 *
 * <p><strong>IMPORTANT:</strong> The .env file is REQUIRED and must contain all secrets. If .env
 * file is missing or empty, tests will fail with IllegalStateException.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>
 * // .env (not committed - contains secrets)
 * AUTH0_CLIENT_SECRET=my-secret-key
 * AUTH0_CLIENT_ID=my-client-id
 *
 * // auth0.properties (committed - contains non-secret defaults)
 * AUTH0_AUTH_URL=https://example.auth0.com/authorize
 * AUTH0_REDIRECT_URI=https://oauth.pstmn.io/v1/callback
 * </pre>
 */
@Slf4j
public class TestDataUtility {

  private TestDataUtility() {}

  static {
    loadEnvFile();
  }

  private static void loadEnvFile() {
    var currentDir = System.getProperty("user.dir");
    var projectRoot = findProjectRoot(currentDir);
    var envFile = new File(projectRoot, ".env");

    if (!envFile.isFile()) {
      // In containerized environments (K8s/Docker), env vars may be set directly
      // Check if ALL critical env vars are already set before skipping .env file load
      // Using allMatch() ensures we don't skip loading if only some vars are set,
      // which would prevent other essential secrets from being loaded
      var criticalVars =
          new String[] {
            "ATAS_FRAMEWORK_URL",
            "AUTH0_CLIENT_ID",
            "AUTH0_CLIENT_SECRET",
            "ADMIN_DASH_SERVICE_BASE_URL"
          };
      var allCriticalVarsSet =
          java.util.Arrays.stream(criticalVars)
              .allMatch(key -> System.getenv(key) != null && !System.getenv(key).isBlank());

      if (!allCriticalVarsSet) {
        throw new IllegalStateException(
            "Required .env file not found and not all critical environment variables are set. "
                + "Either provide .env file at: "
                + envFile.getAbsolutePath()
                + " or set ALL required environment variables directly. "
                + "Required variables: "
                + String.join(", ", criticalVars));
      }
      log.info("Skipping .env file load - all critical environment variables are set directly");
      return;
    }

    try {
      var dotenv = Dotenv.configure().directory(projectRoot).ignoreIfMissing().load();
      var entries = dotenv.entries();

      if (entries.isEmpty()) {
        throw new IllegalStateException(
            ".env file is empty. The .env file is REQUIRED and must contain all secrets. "
                + "Expected location: "
                + envFile.getAbsolutePath());
      }

      var loadedCount =
          entries.stream()
              .filter(
                  entry -> {
                    var key = entry.getKey();
                    var value = entry.getValue();
                    var alreadySet =
                        Optional.ofNullable(System.getProperty(key))
                            .or(() -> Optional.ofNullable(System.getenv(key)))
                            .isPresent();

                    if (!alreadySet) {
                      System.setProperty(key, value);
                      return true;
                    } else {
                      return false;
                    }
                  })
              .count();

      if (loadedCount > 0) {
        log.info("Loaded {} variables from .env file: {}", loadedCount, projectRoot);
      } else {
      }
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to load required .env file. The .env file is REQUIRED and must contain all secrets. "
              + "Expected location: "
              + envFile.getAbsolutePath()
              + " Error: "
              + e.getMessage(),
          e);
    }
  }

  private static String findProjectRoot(String currentDir) {
    var envFile = new File(currentDir, ".env");
    if (envFile.exists()) {
      return currentDir;
    }
    return Optional.ofNullable(new File(currentDir).getParentFile())
        .filter(parent -> new File(parent, ".env").exists())
        .map(File::getAbsolutePath)
        .orElse(currentDir);
  }

  public static Properties loadProperties(String resourcePath) {
    var props = new Properties();
    try (var is = TestDataUtility.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (is != null) {
        props.load(is);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load properties from: " + resourcePath, e);
    }
    return props;
  }

  public static String getProperty(String key) {
    return getProperty(key, null);
  }

  public static String getProperty(String key, Properties properties) {
    return getProperty(key, properties, null);
  }

  public static String getProperty(String key, Properties properties, String defaultValue) {
    return Stream.of(
            Optional.ofNullable(System.getenv(key)).filter(v -> !v.isBlank()),
            Optional.ofNullable(System.getProperty(key)).filter(v -> !v.isBlank()),
            Optional.ofNullable(properties)
                .map(p -> p.getProperty(key))
                .filter(v -> v != null && !v.isBlank()),
            Optional.ofNullable(defaultValue).filter(v -> !v.isBlank()))
        .flatMap(Optional::stream)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Required property not found: " + key));
  }
}
