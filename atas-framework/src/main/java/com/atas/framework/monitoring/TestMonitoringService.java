package com.atas.framework.monitoring;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service that tracks the progress of running test executions and notifies subscribers via
 * Server-Sent Events (SSE). It exposes methods to retrieve the current status of a test execution
 * as well as to register SSE clients for live updates. A scheduled task periodically sends updates
 * to all active emitters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestMonitoringService {

  private final TestExecutionRepository executionRepository;

  /** Map of execution ID to list of SSE emitters subscribed to updates */
  private final Map<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

  /**
   * Get the current status of the specified execution. This method queries the database for the
   * execution and its results and aggregates statistics such as the number of passed/failed tests
   * and overall progress.
   *
   * @param executionId external identifier of the execution
   * @return a DTO with aggregated status information
   */
  public TestExecutionStatus getStatus(String executionId) {
    TestExecution execution = executionRepository.findByExecutionId(executionId).orElse(null);
    if (execution == null) {
      return null;
    }
    List<TestResult> results = execution.getResults();
    int total = results.size();
    int passed = (int) results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
    int failed = (int) results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
    int skipped = (int) results.stream().filter(r -> r.getStatus() == TestStatus.SKIPPED).count();
    int running = (int) results.stream().filter(r -> r.getStatus() == TestStatus.RUNNING).count();
    double progress = TestExecutionStatus.computeProgress(total, passed, failed, skipped);
    LocalDateTime endTime = execution.getEndTime();
    Duration duration;
    if (endTime != null) {
      duration = Duration.between(execution.getStartTime(), endTime);
    } else {
      duration = Duration.between(execution.getStartTime(), LocalDateTime.now());
    }
    return TestExecutionStatus.builder()
        .executionId(execution.getExecutionId())
        .suiteName(execution.getSuiteName())
        .environment(execution.getEnvironment())
        .startTime(execution.getStartTime())
        .endTime(execution.getEndTime())
        .total(total)
        .passed(passed)
        .failed(failed)
        .skipped(skipped)
        .running(running)
        .progress(progress)
        .duration(duration)
        .build();
  }

  /**
   * Register an SSE emitter for the given execution. The emitter will receive periodic updates
   * until it times out or completes.
   *
   * @param executionId the execution to subscribe to
   * @return a new {@link SseEmitter}
   */
  public SseEmitter registerEmitter(String executionId) {
    SseEmitter emitter = new SseEmitter(0L); // no timeout
    emitter.onCompletion(() -> removeEmitter(executionId, emitter));
    emitter.onTimeout(() -> removeEmitter(executionId, emitter));
    emitterMap.computeIfAbsent(executionId, id -> new ArrayList<>()).add(emitter);
    return emitter;
  }

  private void removeEmitter(String executionId, SseEmitter emitter) {
    List<SseEmitter> emitters = emitterMap.get(executionId);
    if (emitters != null) {
      emitters.remove(emitter);
    }
  }

  /**
   * Scheduled task that broadcasts execution status updates to registered SSE emitters. Executions
   * with no active emitters are skipped. This method runs every second by default.
   */
  @Scheduled(fixedDelay = 1000)
  public void broadcastUpdates() {
    if (emitterMap.isEmpty()) {
      return;
    }
    for (Map.Entry<String, List<SseEmitter>> entry : emitterMap.entrySet()) {
      String executionId = entry.getKey();
      TestExecutionStatus status = getStatus(executionId);
      if (status == null) {
        continue;
      }
      for (Iterator<SseEmitter> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
        SseEmitter emitter = iterator.next();
        try {
          emitter.send(status, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
          log.warn("Removing dead SSE emitter for execution {}", executionId, e);
          iterator.remove();
        }
      }
    }
  }

  /** Get dashboard overview statistics */
  public DashboardOverviewDto getDashboardOverview() {
    List<TestExecution> allExecutions = executionRepository.findAll();

    long totalExecutions = allExecutions.size();
    long totalTests = 0;
    long passedTests = 0;
    long failedTests = 0;
    long skippedTests = 0;
    long runningTests = 0;
    long activeExecutions = 0;

    LocalDateTime lastExecutionTime = null;
    Duration totalDuration = Duration.ZERO;
    int completedExecutions = 0;

    for (TestExecution execution : allExecutions) {
      List<TestResult> results = execution.getResults();
      totalTests += results.size();

      for (TestResult result : results) {
        switch (result.getStatus()) {
          case PASSED -> passedTests++;
          case FAILED -> failedTests++;
          case SKIPPED -> skippedTests++;
          case RUNNING -> runningTests++;
          case ERROR -> failedTests++; // Treat ERROR as FAILED for statistics
        }
      }

      if (execution.getStatus() == TestStatus.RUNNING) {
        activeExecutions++;
      }

      if (lastExecutionTime == null || execution.getStartTime().isAfter(lastExecutionTime)) {
        lastExecutionTime = execution.getStartTime();
      }

      if (execution.getEndTime() != null) {
        totalDuration =
            totalDuration.plus(Duration.between(execution.getStartTime(), execution.getEndTime()));
        completedExecutions++;
      }
    }

    double successRate = totalTests > 0 ? (double) passedTests / totalTests * 100.0 : 0.0;
    String averageExecutionTime =
        completedExecutions > 0
            ? formatDuration(totalDuration.dividedBy(completedExecutions))
            : "0s";

    DashboardOverviewDto dto =
        DashboardOverviewDto.builder()
            .totalExecutions(totalExecutions)
            .totalTests(totalTests)
            .passedTests(passedTests)
            .failedTests(failedTests)
            .skippedTests(skippedTests)
            .runningTests(runningTests)
            .successRate(successRate)
            .activeExecutions(activeExecutions)
            .lastExecutionTime(
                lastExecutionTime != null
                    ? lastExecutionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : null)
            .averageExecutionTime(averageExecutionTime)
            .build();

    return dto;
  }

  /** Get recent test executions for dashboard */
  public List<RecentExecutionDto> getRecentExecutions(int limit) {
    List<TestExecution> recentExecutions =
        executionRepository
            .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startTime")))
            .getContent();

    return recentExecutions.stream()
        .map(
            execution -> {
              List<TestResult> results = execution.getResults();
              int total = results.size();
              int passed =
                  (int) results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
              int failed =
                  (int)
                      results.stream()
                          .filter(
                              r ->
                                  r.getStatus() == TestStatus.FAILED
                                      || r.getStatus() == TestStatus.ERROR)
                          .count();
              int skipped =
                  (int) results.stream().filter(r -> r.getStatus() == TestStatus.SKIPPED).count();
              double progress = TestExecutionStatus.computeProgress(total, passed, failed, skipped);

              Duration duration;
              if (execution.getEndTime() != null) {
                duration = Duration.between(execution.getStartTime(), execution.getEndTime());
              } else {
                duration = Duration.between(execution.getStartTime(), LocalDateTime.now());
              }

              return RecentExecutionDto.builder()
                  .executionId(execution.getExecutionId())
                  .suiteName(
                      execution.getSuiteName() != null ? execution.getSuiteName() : "Unknown Suite")
                  .environment(
                      execution.getEnvironment() != null ? execution.getEnvironment() : "N/A")
                  .status(execution.getStatus().name())
                  .startTime(execution.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                  .endTime(
                      execution.getEndTime() != null
                          ? execution.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                          : null)
                  .totalTests(total)
                  .passedTests(passed)
                  .failedTests(failed)
                  .skippedTests(skipped)
                  .progress(progress)
                  .duration(formatDuration(duration))
                  .build();
            })
        .collect(Collectors.toList());
  }

  /** Get execution trends data for dashboard charts */
  public ExecutionTrendsDto getExecutionTrends(int days) {
    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days - 1);

    // Get all executions within the date range
    List<TestExecution> executions =
        executionRepository.findAll().stream()
            .filter(
                exec ->
                    exec.getStartTime().isAfter(startDate.minusDays(1))
                        && exec.getStartTime().isBefore(endDate.plusDays(1)))
            .collect(Collectors.toList());

    // Group executions by date
    Map<String, List<TestExecution>> executionsByDate =
        executions.stream()
            .collect(Collectors.groupingBy(exec -> exec.getStartTime().toLocalDate().toString()));

    // Generate data for each day
    List<String> labels = new ArrayList<>();
    List<Integer> passedData = new ArrayList<>();
    List<Integer> failedData = new ArrayList<>();
    List<Integer> skippedData = new ArrayList<>();

    for (int i = days - 1; i >= 0; i--) {
      LocalDateTime date = endDate.minusDays(i);
      String dateStr = date.toLocalDate().toString();
      String label =
          date.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));

      labels.add(label);

      List<TestExecution> dayExecutions =
          executionsByDate.getOrDefault(dateStr, Collections.emptyList());

      int passed = 0;
      int failed = 0;
      int skipped = 0;

      for (TestExecution execution : dayExecutions) {
        for (TestResult result : execution.getResults()) {
          switch (result.getStatus()) {
            case PASSED -> passed++;
            case FAILED, ERROR -> failed++;
            case SKIPPED -> skipped++;
            case RUNNING -> {} // Running tests are not counted in trends
          }
        }
      }

      passedData.add(passed);
      failedData.add(failed);
      skippedData.add(skipped);
    }

    return ExecutionTrendsDto.builder()
        .labels(labels)
        .passedData(passedData)
        .failedData(failedData)
        .skippedData(skippedData)
        .build();
  }

  private String formatDuration(Duration duration) {
    long seconds = duration.getSeconds();
    if (seconds < 60) {
      return seconds + "s";
    } else if (seconds < 3600) {
      return (seconds / 60) + "m " + (seconds % 60) + "s";
    } else {
      return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
  }

  /** DTO for dashboard overview statistics */
  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  @lombok.Builder
  public static class DashboardOverviewDto {
    private long totalExecutions;
    private long totalTests;
    private long passedTests;
    private long failedTests;
    private long skippedTests;
    private long runningTests;
    private double successRate;
    private long activeExecutions;
    private String lastExecutionTime;
    private String averageExecutionTime;
  }

  /** DTO for recent execution summary */
  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  @lombok.Builder
  public static class RecentExecutionDto {
    private String executionId;
    private String suiteName;
    private String environment;
    private String status;
    private String startTime;
    private String endTime;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int skippedTests;
    private double progress;
    private String duration;
  }

  /** DTO for execution trends data */
  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  @lombok.Builder
  public static class ExecutionTrendsDto {
    private List<String> labels;
    private List<Integer> passedData;
    private List<Integer> failedData;
    private List<Integer> skippedData;
  }
}
