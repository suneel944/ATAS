package com.atas.framework.monitoring;

import com.atas.framework.model.*;
import com.atas.framework.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for database management operations including CRUD monitoring,
 * data browsing, and real-time database operation tracking.
 */
@RestController
@RequestMapping("/api/v1/database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseManagementController {

    private final DatabaseHealthService databaseHealthService;
    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final TestStepRepository stepRepository;
    private final TestAttachmentRepository attachmentRepository;
    private final TestMetricRepository metricRepository;

    /**
     * Get comprehensive database health information
     */
    @GetMapping("/health")
    public ResponseEntity<DatabaseHealthService.DatabaseHealthDto> getDatabaseHealth() {
        DatabaseHealthService.DatabaseHealthDto health = databaseHealthService.getDatabaseHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get real-time database operations summary
     */
    @GetMapping("/operations")
    public ResponseEntity<DatabaseHealthService.DatabaseOperationsDto> getDatabaseOperations() {
        DatabaseHealthService.DatabaseOperationsDto operations = databaseHealthService.getDatabaseOperations();
        return ResponseEntity.ok(operations);
    }

    /**
     * Stream real-time database updates via Server-Sent Events
     */
    @GetMapping("/live")
    public SseEmitter streamDatabaseUpdates(@RequestParam(defaultValue = "default") String clientId) {
        return databaseHealthService.registerDatabaseEmitter(clientId);
    }

    /**
     * Browse test executions with pagination
     */
    @GetMapping("/browse/executions")
    public ResponseEntity<Page<ExecutionBrowseDto>> browseExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(Sort.Direction.DESC, sortBy) : 
            Sort.by(Sort.Direction.ASC, sortBy);
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TestExecution> executions = executionRepository.findAll(pageable);
        
        Page<ExecutionBrowseDto> dtoPage = executions.map(this::mapToExecutionBrowseDto);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Browse test results with pagination
     */
    @GetMapping("/browse/results")
    public ResponseEntity<Page<ResultBrowseDto>> browseResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(Sort.Direction.DESC, sortBy) : 
            Sort.by(Sort.Direction.ASC, sortBy);
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TestResult> results = resultRepository.findAll(pageable);
        
        Page<ResultBrowseDto> dtoPage = results.map(this::mapToResultBrowseDto);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Get detailed execution information
     */
    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionDetailDto> getExecutionDetail(@PathVariable Long id) {
        Optional<TestExecution> execution = executionRepository.findById(id);
        if (execution.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ExecutionDetailDto detail = mapToExecutionDetailDto(execution.get());
        return ResponseEntity.ok(detail);
    }

    /**
     * Get detailed result information
     */
    @GetMapping("/results/{id}")
    public ResponseEntity<ResultDetailDto> getResultDetail(@PathVariable Long id) {
        Optional<TestResult> result = resultRepository.findById(id);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ResultDetailDto detail = mapToResultDetailDto(result.get());
        return ResponseEntity.ok(detail);
    }

    /**
     * Delete test execution and all related data
     */
    @DeleteMapping("/executions/{id}")
    public ResponseEntity<OperationResultDto> deleteExecution(@PathVariable Long id) {
        try {
            Optional<TestExecution> execution = executionRepository.findById(id);
            if (execution.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            executionRepository.deleteById(id);
            databaseHealthService.trackOperation("DELETE");
            
            return ResponseEntity.ok(OperationResultDto.builder()
                    .success(true)
                    .message("Execution deleted successfully")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build());
        } catch (Exception e) {
            log.error("Error deleting execution {}", id, e);
            return ResponseEntity.ok(OperationResultDto.builder()
                    .success(false)
                    .message("Failed to delete execution: " + e.getMessage())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build());
        }
    }

    /**
     * Delete test result and all related data
     */
    @DeleteMapping("/results/{id}")
    public ResponseEntity<OperationResultDto> deleteResult(@PathVariable Long id) {
        try {
            Optional<TestResult> result = resultRepository.findById(id);
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            resultRepository.deleteById(id);
            databaseHealthService.trackOperation("DELETE");
            
            return ResponseEntity.ok(OperationResultDto.builder()
                    .success(true)
                    .message("Result deleted successfully")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build());
        } catch (Exception e) {
            log.error("Error deleting result {}", id, e);
            return ResponseEntity.ok(OperationResultDto.builder()
                    .success(false)
                    .message("Failed to delete result: " + e.getMessage())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build());
        }
    }

    /**
     * Get database statistics summary
     */
    @GetMapping("/statistics")
    public ResponseEntity<DatabaseStatisticsDto> getDatabaseStatistics() {
        DatabaseStatisticsDto stats = DatabaseStatisticsDto.builder()
                .totalExecutions(executionRepository.count())
                .totalResults(resultRepository.count())
                .totalSteps(stepRepository.count())
                .totalAttachments(attachmentRepository.count())
                .totalMetrics(metricRepository.count())
                .lastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        
        return ResponseEntity.ok(stats);
    }

    // Mapping methods
    private ExecutionBrowseDto mapToExecutionBrowseDto(TestExecution execution) {
        return ExecutionBrowseDto.builder()
                .id(execution.getId())
                .executionId(execution.getExecutionId())
                .suiteName(execution.getSuiteName())
                .status(execution.getStatus().name())
                .environment(execution.getEnvironment())
                .startTime(execution.getStartTime() != null ? 
                    execution.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .endTime(execution.getEndTime() != null ? 
                    execution.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .resultCount(execution.getResults().size())
                .build();
    }

    private ResultBrowseDto mapToResultBrowseDto(TestResult result) {
        return ResultBrowseDto.builder()
                .id(result.getId())
                .testId(result.getTestId())
                .testName(result.getTestName())
                .status(result.getStatus().name())
                .startTime(result.getStartTime() != null ? 
                    result.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .endTime(result.getEndTime() != null ? 
                    result.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .stepCount(result.getSteps().size())
                .attachmentCount(result.getAttachments().size())
                .metricCount(result.getMetrics().size())
                .build();
    }

    private ExecutionDetailDto mapToExecutionDetailDto(TestExecution execution) {
        return ExecutionDetailDto.builder()
                .id(execution.getId())
                .executionId(execution.getExecutionId())
                .suiteName(execution.getSuiteName())
                .status(execution.getStatus().name())
                .environment(execution.getEnvironment())
                .startTime(execution.getStartTime() != null ? 
                    execution.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .endTime(execution.getEndTime() != null ? 
                    execution.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .videoUrl(execution.getVideoUrl())
                .results(execution.getResults().stream()
                    .map(this::mapToResultBrowseDto)
                    .collect(Collectors.toList()))
                .build();
    }

    private ResultDetailDto mapToResultDetailDto(TestResult result) {
        return ResultDetailDto.builder()
                .id(result.getId())
                .testId(result.getTestId())
                .testName(result.getTestName())
                .status(result.getStatus().name())
                .startTime(result.getStartTime() != null ? 
                    result.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .endTime(result.getEndTime() != null ? 
                    result.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .steps(result.getSteps().stream()
                    .map(step -> StepDetailDto.builder()
                        .id(step.getId())
                        .description(step.getDescription())
                        .status(step.getStatus().name())
                        .startTime(step.getStartTime() != null ? 
                            step.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                        .endTime(step.getEndTime() != null ? 
                            step.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                        .build())
                    .collect(Collectors.toList()))
                .attachments(result.getAttachments().stream()
                    .map(attachment -> AttachmentDetailDto.builder()
                        .id(attachment.getId())
                        .type(attachment.getType().name())
                        .fileName(attachment.getFileName())
                        .mimeType(attachment.getMimeType())
                        .url(attachment.getUrl())
                        .createdAt(attachment.getCreatedAt() != null ? 
                            attachment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                        .build())
                    .collect(Collectors.toList()))
                .metrics(result.getMetrics().stream()
                    .map(metric -> MetricDetailDto.builder()
                        .id(metric.getId())
                        .key(metric.getKey())
                        .value(metric.getValue())
                        .build())
                    .collect(Collectors.toList()))
                .build();
    }

    // DTOs for database management
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ExecutionBrowseDto {
        private Long id;
        private String executionId;
        private String suiteName;
        private String status;
        private String environment;
        private String startTime;
        private String endTime;
        private int resultCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ResultBrowseDto {
        private Long id;
        private String testId;
        private String testName;
        private String status;
        private String startTime;
        private String endTime;
        private int stepCount;
        private int attachmentCount;
        private int metricCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ExecutionDetailDto {
        private Long id;
        private String executionId;
        private String suiteName;
        private String status;
        private String environment;
        private String startTime;
        private String endTime;
        private String videoUrl;
        private List<ResultBrowseDto> results;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ResultDetailDto {
        private Long id;
        private String testId;
        private String testName;
        private String status;
        private String startTime;
        private String endTime;
        private List<StepDetailDto> steps;
        private List<AttachmentDetailDto> attachments;
        private List<MetricDetailDto> metrics;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class StepDetailDto {
        private Long id;
        private String description;
        private String status;
        private String startTime;
        private String endTime;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AttachmentDetailDto {
        private Long id;
        private String type;
        private String fileName;
        private String mimeType;
        private String url;
        private String createdAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class MetricDetailDto {
        private Long id;
        private String key;
        private String value;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OperationResultDto {
        private boolean success;
        private String message;
        private String timestamp;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DatabaseStatisticsDto {
        private long totalExecutions;
        private long totalResults;
        private long totalSteps;
        private long totalAttachments;
        private long totalMetrics;
        private String lastUpdated;
    }
}
