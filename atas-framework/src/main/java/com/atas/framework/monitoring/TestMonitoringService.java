package com.atas.framework.monitoring;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
