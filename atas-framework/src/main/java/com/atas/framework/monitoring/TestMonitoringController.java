package com.atas.framework.monitoring;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.repository.TestExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller exposing endpoints for monitoring test executions.
 * Clients can poll for the current status or subscribe to live
 * updates via Server-Sent Events (SSE).  Results for a given
 * execution can also be fetched via a simple REST call.
 */
@RestController
@RequestMapping("/api/v1/test-execution")
@RequiredArgsConstructor
@Slf4j
public class TestMonitoringController {

    private final TestMonitoringService monitoringService;
    private final TestExecutionRepository executionRepository;

    /**
     * Endpoint to retrieve the current aggregated status of a test
     * execution.  The client must provide the executionId as a
     * request parameter.
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

    /**
     * Endpoint to stream live updates for a given execution.  Returns
     * a {@link SseEmitter} that clients can listen to.  The caller
     * must include the executionId as a request parameter.
     *
     * @param executionId unique identifier of the execution
     * @return SSE emitter providing status updates
     */
    @GetMapping("/live")
    public SseEmitter streamUpdates(@RequestParam String executionId) {
        return monitoringService.registerEmitter(executionId);
    }

    /**
     * Endpoint to fetch all test results for a given execution.  The
     * results are returned in the order they were recorded.  This
     * endpoint can be polled to obtain incremental updates.
     *
     * @param executionId external identifier
     * @return list of results or 404 if execution not found
     */
    @GetMapping("/results/{executionId}")
    public ResponseEntity<List<TestResultDto>> getResults(@PathVariable String executionId) {
        TestExecution execution = executionRepository.findByExecutionId(executionId)
                .orElse(null);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        List<TestResultDto> dtos = execution.getResults().stream()
                .map(TestResultDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Simple DTO for exposing test results via the API without
     * exposing internal JPA entities.  Includes minimal fields; more
     * can be added as needed (e.g. attachments, metrics).
     */
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
                    entity.getEndTime() != null ? entity.getEndTime().toString() : null
            );
        }
    }
}