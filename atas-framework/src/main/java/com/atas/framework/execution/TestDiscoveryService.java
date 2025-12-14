package com.atas.framework.execution;

import com.atas.framework.execution.dto.TestDiscoveryResponse;
import com.atas.framework.execution.dto.TestExecutionRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service for discovering available tests, suites, and tags */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestDiscoveryService {

  @Value("${atas.test.base.path:atas-tests/src/test/java/com/atas}")
  private String testBasePath;

  private static final Pattern TEST_METHOD_PATTERN =
      Pattern.compile("@Test\\s+.*?void\\s+(\\w+)\\s*\\(");
  private static final Pattern TAG_PATTERN = Pattern.compile("@Tag\\(\"([^\"]+)\"\\)");

  /** Discover all available tests, suites, and tags */
  public TestDiscoveryResponse discoverTests() {
    List<TestDiscoveryResponse.TestClassInfo> testClasses = getTestClasses();
    List<TestDiscoveryResponse.TestSuiteInfo> testSuites = getTestSuites();
    List<String> availableTags = getAvailableTags();

    int totalTests = testClasses.stream().mapToInt(tc -> tc.getTestMethods().size()).sum();

    return TestDiscoveryResponse.builder()
        .testClasses(testClasses)
        .testSuites(testSuites)
        .availableTags(availableTags)
        .totalTests(totalTests)
        .build();
  }

  /** Get all test classes */
  public List<TestDiscoveryResponse.TestClassInfo> getTestClasses() {
    List<TestDiscoveryResponse.TestClassInfo> testClasses = new ArrayList<>();

    try {
      Path testPath = Paths.get(testBasePath, "features");
      if (Files.exists(testPath)) {
        Files.walk(testPath)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith("Test.java"))
            .forEach(
                path -> {
                  try {
                    TestDiscoveryResponse.TestClassInfo testClass = parseTestClass(path);
                    if (testClass != null) {
                      testClasses.add(testClass);
                    }
                  } catch (Exception e) {
                    log.warn("Error parsing test class: {}", path, e);
                  }
                });
      } else {
        log.warn("Test path does not exist: {}. Returning empty list.", testPath);
      }
    } catch (IOException e) {
      log.error("Error discovering test classes", e);
    }

    return testClasses;
  }

  /** Get all test suites */
  public List<TestDiscoveryResponse.TestSuiteInfo> getTestSuites() {
    List<TestDiscoveryResponse.TestSuiteInfo> testSuites = new ArrayList<>();

    try {
      Path suitePath = Paths.get(testBasePath, "suites");
      if (Files.exists(suitePath)) {
        Files.walk(suitePath)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith("TestSuite.java"))
            .forEach(
                path -> {
                  try {
                    TestDiscoveryResponse.TestSuiteInfo testSuite = parseTestSuite(path);
                    if (testSuite != null) {
                      testSuites.add(testSuite);
                    }
                  } catch (Exception e) {
                    log.warn("Error parsing test suite: {}", path, e);
                  }
                });
      } else {
        log.warn("Test suite path does not exist: {}. Returning empty list.", suitePath);
      }
    } catch (IOException e) {
      log.error("Error discovering test suites", e);
    }

    return testSuites;
  }

  /** Get all available tags */
  public List<String> getAvailableTags() {
    Set<String> tags = new HashSet<>();

    try {
      Path testPath = Paths.get(testBasePath);
      if (Files.exists(testPath)) {
        Files.walk(testPath)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(
                path -> {
                  try {
                    String content = Files.readString(path);
                    Matcher matcher = TAG_PATTERN.matcher(content);
                    while (matcher.find()) {
                      tags.add(matcher.group(1));
                    }
                  } catch (IOException e) {
                    log.warn("Error reading file: {}", path, e);
                  }
                });
      } else {
        log.warn("Test path does not exist: {}. Returning empty tags list.", testPath);
      }
    } catch (IOException e) {
      log.error("Error discovering tags", e);
    }

    return new ArrayList<>(tags);
  }

  /** Discover tests that will be executed based on request */
  public List<String> discoverTestsToExecute(TestExecutionRequest request) {
    List<String> tests = new ArrayList<>();

    switch (request.getType()) {
      case INDIVIDUAL_TEST:
        if (request.getTestClass() != null) {
          tests.add(request.getTestClass());
          if (request.getTestMethod() != null) {
            tests.set(0, request.getTestClass() + "." + request.getTestMethod());
          }
        }
        break;

      case TAGS:
        tests.addAll(findTestsByTags(request.getTags()));
        break;

      case GREP:
        tests.addAll(findTestsByPattern(request.getGrepPattern()));
        break;

      case SUITE:
        tests.addAll(findTestsInSuite(request.getSuiteName()));
        break;
    }

    return tests;
  }

  /** Parse a test class file */
  private TestDiscoveryResponse.TestClassInfo parseTestClass(Path path) {
    try {
      String content = Files.readString(path);
      String fileName = path.getFileName().toString();
      String className = fileName.substring(0, fileName.lastIndexOf('.'));

      // Extract package name
      String packageName = extractPackageName(content);

      // Extract test methods
      List<String> testMethods = extractTestMethods(content);

      // Extract tags
      List<String> tags = extractTags(content);

      // Determine type (UI/API)
      String type = determineTestType(path);

      return TestDiscoveryResponse.TestClassInfo.builder()
          .className(className)
          .packageName(packageName)
          .fullName(packageName + "." + className)
          .testMethods(testMethods)
          .tags(tags)
          .type(type)
          .build();

    } catch (IOException e) {
      log.error("Error parsing test class: {}", path, e);
      return null;
    }
  }

  /** Parse a test suite file */
  private TestDiscoveryResponse.TestSuiteInfo parseTestSuite(Path path) {
    try {
      String content = Files.readString(path);
      String fileName = path.getFileName().toString();
      String suiteName = fileName.substring(0, fileName.lastIndexOf('.'));

      // Extract package name
      String packageName = extractPackageName(content);

      // Extract included test classes
      List<String> includedTestClasses = extractIncludedTestClasses(content);

      // Determine type
      String type = determineTestType(path);

      return TestDiscoveryResponse.TestSuiteInfo.builder()
          .suiteName(suiteName)
          .className(packageName + "." + suiteName)
          .description("Test suite for " + suiteName)
          .includedTestClasses(includedTestClasses)
          .type(type)
          .build();

    } catch (IOException e) {
      log.error("Error parsing test suite: {}", path, e);
      return null;
    }
  }

  /** Extract package name from Java file content */
  private String extractPackageName(String content) {
    Pattern packagePattern = Pattern.compile("package\\s+([^;]+);");
    Matcher matcher = packagePattern.matcher(content);
    return matcher.find() ? matcher.group(1) : "";
  }

  /** Extract test methods from Java file content */
  private List<String> extractTestMethods(String content) {
    List<String> methods = new ArrayList<>();
    Matcher matcher = TEST_METHOD_PATTERN.matcher(content);
    while (matcher.find()) {
      methods.add(matcher.group(1));
    }
    return methods;
  }

  /** Extract tags from Java file content */
  private List<String> extractTags(String content) {
    List<String> tags = new ArrayList<>();
    Matcher matcher = TAG_PATTERN.matcher(content);
    while (matcher.find()) {
      tags.add(matcher.group(1));
    }
    return tags;
  }

  /** Extract included test classes from suite file */
  private List<String> extractIncludedTestClasses(String content) {
    List<String> classes = new ArrayList<>();
    Pattern pattern = Pattern.compile("@SelectClasses\\s*\\{[^}]*\\}");
    Matcher matcher = pattern.matcher(content);
    if (matcher.find()) {
      String selectClassesContent = matcher.group(0);
      Pattern classPattern = Pattern.compile("([A-Za-z0-9_]+\\.class)");
      Matcher classMatcher = classPattern.matcher(selectClassesContent);
      while (classMatcher.find()) {
        classes.add(classMatcher.group(1).replace(".class", ""));
      }
    }
    return classes;
  }

  /** Determine test type based on path */
  private String determineTestType(Path path) {
    String pathStr = path.toString();
    if (pathStr.contains("/ui/")) {
      return "UI";
    } else if (pathStr.contains("/api/")) {
      return "API";
    } else {
      return "UNKNOWN";
    }
  }

  /** Find tests by tags */
  private List<String> findTestsByTags(List<String> tags) {
    List<String> tests = new ArrayList<>();
    List<TestDiscoveryResponse.TestClassInfo> testClasses = getTestClasses();

    for (TestDiscoveryResponse.TestClassInfo testClass : testClasses) {
      if (testClass.getTags().stream().anyMatch(tags::contains)) {
        tests.add(testClass.getFullName());
      }
    }

    return tests;
  }

  /** Find tests by pattern */
  private List<String> findTestsByPattern(String pattern) {
    List<String> tests = new ArrayList<>();
    List<TestDiscoveryResponse.TestClassInfo> testClasses = getTestClasses();

    Pattern regexPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

    for (TestDiscoveryResponse.TestClassInfo testClass : testClasses) {
      if (regexPattern.matcher(testClass.getFullName()).find()
          || testClass.getTestMethods().stream()
              .anyMatch(method -> regexPattern.matcher(method).find())) {
        tests.add(testClass.getFullName());
      }
    }

    return tests;
  }

  /** Find tests in a specific suite */
  private List<String> findTestsInSuite(String suiteName) {
    List<String> tests = new ArrayList<>();
    List<TestDiscoveryResponse.TestSuiteInfo> testSuites = getTestSuites();

    for (TestDiscoveryResponse.TestSuiteInfo testSuite : testSuites) {
      if (testSuite.getSuiteName().equals(suiteName)) {
        tests.addAll(testSuite.getIncludedTestClasses());
      }
    }

    return tests;
  }
}
