package com.atas.framework.execution;

import com.atas.framework.execution.dto.TestDiscoveryResponse;
import com.atas.framework.execution.dto.TestExecutionRequest;
import com.atas.framework.execution.dto.TestExecutionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for test execution with various filtering options
 */
@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Slf4j
public class TestExecutionController {

    private final TestExecutionService testExecutionService;

    /**
     * Execute individual test
     * 
     * @param testClass Test class name (e.g., "LoginUiTest")
     * @param testMethod Optional test method name (e.g., "login_should_succeed")
     * @param environment Environment (dev, staging, prod)
     * @param browserType Browser type for UI tests (CHROMIUM, FIREFOX, WEBKIT)
     * @param recordVideo Whether to record video
     * @param captureScreenshots Whether to capture screenshots on failure
     * @return Test execution response
     */
    @PostMapping("/execute/individual")
    public ResponseEntity<TestExecutionResponse> executeIndividualTest(
            @RequestParam String testClass,
            @RequestParam(required = false) String testMethod,
            @RequestParam(defaultValue = "dev") String environment,
            @RequestParam(required = false) String browserType,
            @RequestParam(defaultValue = "true") boolean recordVideo,
            @RequestParam(defaultValue = "true") boolean captureScreenshots) {
        
        log.info("Executing individual test: {}.{}", testClass, testMethod);
        
        TestExecutionRequest request = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.INDIVIDUAL_TEST)
                .testClass(testClass)
                .testMethod(testMethod)
                .environment(environment)
                .browserType(browserType)
                .recordVideo(recordVideo)
                .captureScreenshots(captureScreenshots)
                .build();
        
        TestExecutionResponse response = testExecutionService.executeTests(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute tests by tags
     * 
     * @param tags List of tags to filter by (e.g., "smoke", "regression")
     * @param environment Environment (dev, staging, prod)
     * @param browserType Browser type for UI tests
     * @param recordVideo Whether to record video
     * @param captureScreenshots Whether to capture screenshots on failure
     * @return Test execution response
     */
    @PostMapping("/execute/tags")
    public ResponseEntity<TestExecutionResponse> executeTestsByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "dev") String environment,
            @RequestParam(required = false) String browserType,
            @RequestParam(defaultValue = "true") boolean recordVideo,
            @RequestParam(defaultValue = "true") boolean captureScreenshots) {
        
        log.info("Executing tests with tags: {}", tags);
        
        TestExecutionRequest request = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.TAGS)
                .tags(tags)
                .environment(environment)
                .browserType(browserType)
                .recordVideo(recordVideo)
                .captureScreenshots(captureScreenshots)
                .build();
        
        TestExecutionResponse response = testExecutionService.executeTests(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute tests by grep pattern
     * 
     * @param pattern Grep pattern to match test names or classes
     * @param environment Environment (dev, staging, prod)
     * @param browserType Browser type for UI tests
     * @param recordVideo Whether to record video
     * @param captureScreenshots Whether to capture screenshots on failure
     * @return Test execution response
     */
    @PostMapping("/execute/grep")
    public ResponseEntity<TestExecutionResponse> executeTestsByGrep(
            @RequestParam String pattern,
            @RequestParam(defaultValue = "dev") String environment,
            @RequestParam(required = false) String browserType,
            @RequestParam(defaultValue = "true") boolean recordVideo,
            @RequestParam(defaultValue = "true") boolean captureScreenshots) {
        
        log.info("Executing tests matching pattern: {}", pattern);
        
        TestExecutionRequest request = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.GREP)
                .grepPattern(pattern)
                .environment(environment)
                .browserType(browserType)
                .recordVideo(recordVideo)
                .captureScreenshots(captureScreenshots)
                .build();
        
        TestExecutionResponse response = testExecutionService.executeTests(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute test suite
     * 
     * @param suiteName Test suite name (e.g., "authentication-ui", "monitoring-api")
     * @param environment Environment (dev, staging, prod)
     * @param browserType Browser type for UI tests
     * @param recordVideo Whether to record video
     * @param captureScreenshots Whether to capture screenshots on failure
     * @return Test execution response
     */
    @PostMapping("/execute/suite")
    public ResponseEntity<TestExecutionResponse> executeTestSuite(
            @RequestParam String suiteName,
            @RequestParam(defaultValue = "dev") String environment,
            @RequestParam(required = false) String browserType,
            @RequestParam(defaultValue = "true") boolean recordVideo,
            @RequestParam(defaultValue = "true") boolean captureScreenshots) {
        
        log.info("Executing test suite: {}", suiteName);
        
        TestExecutionRequest request = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.SUITE)
                .suiteName(suiteName)
                .environment(environment)
                .browserType(browserType)
                .recordVideo(recordVideo)
                .captureScreenshots(captureScreenshots)
                .build();
        
        TestExecutionResponse response = testExecutionService.executeTests(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute tests with custom request
     * 
     * @param request Custom test execution request
     * @return Test execution response
     */
    @PostMapping("/execute/custom")
    public ResponseEntity<TestExecutionResponse> executeCustomTests(@Valid @RequestBody TestExecutionRequest request) {
        log.info("Executing custom test request: {}", request.getType());
        
        TestExecutionResponse response = testExecutionService.executeTests(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Discover available tests, suites, and tags
     * 
     * @return Test discovery response
     */
    @GetMapping("/discover")
    public ResponseEntity<TestDiscoveryResponse> discoverTests() {
        log.info("Discovering available tests");
        
        TestDiscoveryResponse response = testExecutionService.discoverTests();
        return ResponseEntity.ok(response);
    }

    /**
     * Get available test classes
     * 
     * @return List of test classes
     */
    @GetMapping("/discover/classes")
    public ResponseEntity<List<TestDiscoveryResponse.TestClassInfo>> getTestClasses() {
        log.info("Getting available test classes");
        
        List<TestDiscoveryResponse.TestClassInfo> testClasses = testExecutionService.getTestClasses();
        return ResponseEntity.ok(testClasses);
    }

    /**
     * Get available test suites
     * 
     * @return List of test suites
     */
    @GetMapping("/discover/suites")
    public ResponseEntity<List<TestDiscoveryResponse.TestSuiteInfo>> getTestSuites() {
        log.info("Getting available test suites");
        
        List<TestDiscoveryResponse.TestSuiteInfo> testSuites = testExecutionService.getTestSuites();
        return ResponseEntity.ok(testSuites);
    }

    /**
     * Get available tags
     * 
     * @return List of available tags
     */
    @GetMapping("/discover/tags")
    public ResponseEntity<List<String>> getAvailableTags() {
        log.info("Getting available tags");
        
        List<String> tags = testExecutionService.getAvailableTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * Get test classes by type (UI/API)
     * 
     * @param type Test type (UI, API)
     * @return List of test classes of specified type
     */
    @GetMapping("/discover/classes/{type}")
    public ResponseEntity<List<TestDiscoveryResponse.TestClassInfo>> getTestClassesByType(@PathVariable String type) {
        log.info("Getting test classes by type: {}", type);
        
        List<TestDiscoveryResponse.TestClassInfo> testClasses = testExecutionService.getTestClasses()
                .stream()
                .filter(tc -> tc.getType().equalsIgnoreCase(type))
                .toList();
        
        return ResponseEntity.ok(testClasses);
    }

    /**
     * Get test suites by type (UI/API)
     * 
     * @param type Test type (UI, API)
     * @return List of test suites of specified type
     */
    @GetMapping("/discover/suites/{type}")
    public ResponseEntity<List<TestDiscoveryResponse.TestSuiteInfo>> getTestSuitesByType(@PathVariable String type) {
        log.info("Getting test suites by type: {}", type);
        
        List<TestDiscoveryResponse.TestSuiteInfo> testSuites = testExecutionService.getTestSuites()
                .stream()
                .filter(ts -> ts.getType().equalsIgnoreCase(type))
                .toList();
        
        return ResponseEntity.ok(testSuites);
    }
}
