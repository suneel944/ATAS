package com.atas.framework.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Unit test to detect version mismatches between Docker Compose and integration test
 * configurations. This ensures consistency across different environments.
 */
class VersionConsistencyTest {

  @Test
  void shouldHaveConsistentPostgreSQLVersions() {
    // Given - Extract PostgreSQL version from Docker Compose
    String dockerComposeVersion = extractPostgreSQLVersionFromDockerCompose();

    // Given - Extract PostgreSQL version from integration test
    String integrationTestVersion = extractPostgreSQLVersionFromIntegrationTest();

    // Then - Versions should match
    assertThat(integrationTestVersion)
        .withFailMessage(
            "PostgreSQL version mismatch detected! "
                + "Docker Compose uses: %s, Integration test uses: %s. "
                + "Update DatabaseIntegrationTest to use postgres:%s",
            dockerComposeVersion, integrationTestVersion, dockerComposeVersion)
        .isEqualTo(dockerComposeVersion);
  }

  @Test
  void shouldHaveValidPostgreSQLVersions() {
    // Given
    String dockerComposeVersion = extractPostgreSQLVersionFromDockerCompose();
    String integrationTestVersion = extractPostgreSQLVersionFromIntegrationTest();

    // Then - Both versions should be valid PostgreSQL versions
    assertThat(dockerComposeVersion)
        .withFailMessage("Invalid PostgreSQL version in Docker Compose: %s", dockerComposeVersion)
        .matches("\\d+(\\.\\d+)?(-[a-zA-Z0-9]+)?");

    assertThat(integrationTestVersion)
        .withFailMessage(
            "Invalid PostgreSQL version in integration test: %s", integrationTestVersion)
        .matches("\\d+(\\.\\d+)?(-[a-zA-Z0-9]+)?");
  }

  @Test
  void shouldUseSupportedPostgreSQLVersions() {
    // Given
    String dockerComposeVersion = extractPostgreSQLVersionFromDockerCompose();
    String integrationTestVersion = extractPostgreSQLVersionFromIntegrationTest();

    // Then - Versions should be supported (>= 12.0)
    int dockerMajorVersion = extractMajorVersion(dockerComposeVersion);
    int integrationMajorVersion = extractMajorVersion(integrationTestVersion);

    assertThat(dockerMajorVersion)
        .withFailMessage(
            "Docker Compose PostgreSQL version %s is not supported (minimum: 12)",
            dockerComposeVersion)
        .isGreaterThanOrEqualTo(12);

    assertThat(integrationMajorVersion)
        .withFailMessage(
            "Integration test PostgreSQL version %s is not supported (minimum: 12)",
            integrationTestVersion)
        .isGreaterThanOrEqualTo(12);
  }

  private String extractPostgreSQLVersionFromDockerCompose() {
    try {
      // Read the Docker Compose file directly from the filesystem
      java.nio.file.Path dockerComposePath =
          java.nio.file.Paths.get(
                  System.getProperty("user.dir"), "..", "docker", "docker-compose-local-db.yml")
              .normalize();

      if (!java.nio.file.Files.exists(dockerComposePath)) {
        throw new IllegalStateException(
            "Could not find docker-compose-local-db.yml file at: " + dockerComposePath);
      }

      try (InputStream inputStream = java.nio.file.Files.newInputStream(dockerComposePath)) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);

        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) data.get("services");
        @SuppressWarnings("unchecked")
        Map<String, Object> atasDb = (Map<String, Object>) services.get("atas-db");

        String image = (String) atasDb.get("image");
        if (image == null || !image.startsWith("postgres:")) {
          throw new IllegalStateException("PostgreSQL image not found in Docker Compose");
        }

        return image.substring("postgres:".length());
      }

    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to extract PostgreSQL version from Docker Compose", e);
    }
  }

  private String extractPostgreSQLVersionFromIntegrationTest() {
    try {
      // Read the integration test file directly to extract the version
      java.nio.file.Path integrationTestPath =
          java.nio.file.Paths.get(
                  System.getProperty("user.dir"),
                  "src",
                  "test",
                  "java",
                  "com",
                  "atas",
                  "framework",
                  "integration",
                  "DatabaseIntegrationTest.java")
              .normalize();

      if (!java.nio.file.Files.exists(integrationTestPath)) {
        throw new IllegalStateException(
            "Could not find DatabaseIntegrationTest.java file at: " + integrationTestPath);
      }

      String content = java.nio.file.Files.readString(integrationTestPath);

      // Look for the PostgreSQL container declaration - simpler pattern
      java.util.regex.Pattern pattern =
          java.util.regex.Pattern.compile("postgres:([a-zA-Z0-9.-]+)");
      java.util.regex.Matcher matcher = pattern.matcher(content);

      if (matcher.find()) {
        return matcher.group(1);
      } else {
        throw new IllegalStateException(
            "PostgreSQL container declaration not found in integration test");
      }

    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to extract PostgreSQL version from integration test", e);
    }
  }

  private int extractMajorVersion(String version) {
    try {
      // Handle versions like "18" or "18-alpine" or "18.5" or "18.5-alpine"
      String cleanVersion = version.split("-")[0]; // Remove any suffix like "-alpine"
      String majorVersionStr = cleanVersion.split("\\.")[0];
      return Integer.parseInt(majorVersionStr);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid version format: " + version, e);
    }
  }
}
