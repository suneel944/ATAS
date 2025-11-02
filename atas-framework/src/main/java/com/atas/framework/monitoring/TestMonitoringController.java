package com.atas.framework.monitoring;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.repository.TestExecutionRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller exposing endpoints for monitoring test executions. Clients can poll for the
 * current status or subscribe to live updates via Server-Sent Events (SSE). Results for a given
 * execution can also be fetched via a simple REST call.
 */
@RestController
@RequestMapping("/api/v1/test-execution")
@RequiredArgsConstructor
@Slf4j
public class TestMonitoringController {

  private final TestMonitoringService monitoringService;
  private final TestExecutionRepository executionRepository;
  private final DatabaseHealthService databaseHealthService;

  /**
   * Endpoint to retrieve the current aggregated status of a test execution. The client must provide
   * the executionId as a request parameter.
   *
   * @param executionId unique identifier of the execution
   * @return status summary, or 404 if no such execution exists
   */
  @GetMapping("/status")
  public ResponseEntity<TestExecutionStatus> getStatus(@RequestParam String executionId) {
    TestExecutionStatus status = monitoringService.getStatus(executionId);
    if (status == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(status);
  }

  @GetMapping("/live")
  public SseEmitter streamUpdates(@RequestParam String executionId) {
    return monitoringService.registerEmitter(executionId);
  }

  @GetMapping("/results/{executionId}")
  public ResponseEntity<List<TestResultDto>> getResults(@PathVariable String executionId) {
    TestExecution execution =
        executionRepository.findByExecutionIdWithResults(executionId).orElse(null);
    if (execution == null) {
      return ResponseEntity.notFound().build();
    }
    List<TestResultDto> dtos =
        execution.getResults().stream().map(TestResultDto::fromEntity).collect(Collectors.toList());
    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/dashboard/overview")
  public ResponseEntity<TestMonitoringService.DashboardOverviewDto> getDashboardOverview() {
    TestMonitoringService.DashboardOverviewDto overview = monitoringService.getDashboardOverview();
    return ResponseEntity.ok(overview);
  }

  @GetMapping("/dashboard/recent")
  public ResponseEntity<List<TestMonitoringService.RecentExecutionDto>> getRecentExecutions(
      @RequestParam(defaultValue = "10") int limit) {
    List<TestMonitoringService.RecentExecutionDto> recent =
        monitoringService.getRecentExecutions(limit);
    return ResponseEntity.ok(recent);
  }

  @GetMapping("/dashboard/database-health")
  public ResponseEntity<DatabaseHealthService.DatabaseHealthDto> getDatabaseHealth() {
    DatabaseHealthService.DatabaseHealthDto health = databaseHealthService.getDatabaseHealth();
    return ResponseEntity.ok(health);
  }

  @GetMapping("/dashboard/database-operations")
  public ResponseEntity<DatabaseHealthService.DatabaseOperationsDto> getDatabaseOperations() {
    DatabaseHealthService.DatabaseOperationsDto operations =
        databaseHealthService.getDatabaseOperations();
    return ResponseEntity.ok(operations);
  }

  @GetMapping("/dashboard/execution-trends")
  public ResponseEntity<TestMonitoringService.ExecutionTrendsDto> getExecutionTrends(
      @RequestParam(defaultValue = "7") int days) {
    TestMonitoringService.ExecutionTrendsDto trends = monitoringService.getExecutionTrends(days);
    return ResponseEntity.ok(trends);
  }

  @GetMapping("/dashboard/active")
  public ResponseEntity<List<TestMonitoringService.RecentExecutionDto>> getActiveExecutions() {
    List<TestMonitoringService.RecentExecutionDto> active = monitoringService.getActiveExecutions();
    return ResponseEntity.ok(active);
  }

  @GetMapping("/dashboard/active/live")
  public SseEmitter streamActiveExecutions() {
    return monitoringService.registerActiveExecutionsEmitter();
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  static class TestResultDto {
    private String testId;
    private String testName;
    private String status;
    private String startTime;
    private String endTime;

    static TestResultDto fromEntity(TestResult entity) {
      return new TestResultDto(
          entity.getTestId(),
          entity.getTestName(),
          entity.getStatus().name(),
          entity.getStartTime() != null ? entity.getStartTime().toString() : null,
          entity.getEndTime() != null ? entity.getEndTime().toString() : null);
    }
  }
}
