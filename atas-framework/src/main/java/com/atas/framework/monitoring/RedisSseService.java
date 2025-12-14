package com.atas.framework.monitoring;

import com.atas.framework.model.TestStatus;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service for managing SSE connections with Redis Pub/Sub for horizontal scaling. This allows SSE
 * updates to work across multiple service instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSseService implements MessageListener {

  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisMessageListenerContainer redisMessageListenerContainer;
  private final TestMonitoringService testMonitoringService;

  // Map of executionId -> list of SSE emitters for that execution
  private final Map<String, List<SseEmitter>> executionEmitters = new ConcurrentHashMap<>();

  // List of SSE emitters for active executions
  private final List<SseEmitter> activeExecutionsEmitters =
      new java.util.concurrent.CopyOnWriteArrayList<>();

  /** Initialize Redis subscription on service startup. */
  @jakarta.annotation.PostConstruct
  public void init() {
    try {
      redisMessageListenerContainer.addMessageListener(
          this, new ChannelTopic("atas:execution:updates"));
      // Start the container if not already running
      // This happens after ApplicationContext is fully initialized
      if (!redisMessageListenerContainer.isRunning()) {
        redisMessageListenerContainer.start();
      }
      log.info("Redis SSE service initialized - subscribed to execution updates");
    } catch (Exception e) {
      log.warn(
          "Failed to initialize Redis SSE service (Redis may not be available): {}",
          e.getMessage());
      // Don't fail application startup if Redis is unavailable
      // The service will work without Pub/Sub scaling
    }
  }

  /** Register an SSE emitter for a specific execution. */
  public SseEmitter registerExecutionEmitter(String executionId) {
    SseEmitter emitter = new SseEmitter(0L);

    emitter.onCompletion(
        () -> {
          removeExecutionEmitter(executionId, emitter);
        });

    emitter.onTimeout(
        () -> {
          removeExecutionEmitter(executionId, emitter);
        });

    emitter.onError(
        (ex) -> {
          removeExecutionEmitter(executionId, emitter);
          log.warn("SSE emitter error for execution {}: {}", executionId, ex.getMessage());
        });

    executionEmitters
        .computeIfAbsent(executionId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
        .add(emitter);

    // Send initial status
    try {
      var status = testMonitoringService.getStatus(executionId);
      if (status != null) {
        emitter.send(status, MediaType.APPLICATION_JSON);
      }
    } catch (IOException e) {
      log.warn("Failed to send initial status to SSE emitter: {}", e.getMessage());
      removeExecutionEmitter(executionId, emitter);
    }

    return emitter;
  }

  /** Register an SSE emitter for active executions list. */
  public SseEmitter registerActiveExecutionsEmitter() {
    SseEmitter emitter = new SseEmitter(0L);

    emitter.onCompletion(
        () -> {
          activeExecutionsEmitters.remove(emitter);
        });

    emitter.onTimeout(
        () -> {
          activeExecutionsEmitters.remove(emitter);
        });

    emitter.onError(
        (ex) -> {
          activeExecutionsEmitters.remove(emitter);
          log.warn("Active executions SSE emitter error: {}", ex.getMessage());
        });

    activeExecutionsEmitters.add(emitter);

    // Send initial data
    try {
      var activeExecutions = testMonitoringService.getActiveExecutions();
      emitter.send(activeExecutions, MediaType.APPLICATION_JSON);
    } catch (IOException e) {
      log.warn("Failed to send initial active executions to SSE emitter: {}", e.getMessage());
      activeExecutionsEmitters.remove(emitter);
    }

    return emitter;
  }

  /** Handle Redis messages for execution updates. */
  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String channel = new String(message.getChannel());
      if (!"atas:execution:updates".equals(channel)) {
        return;
      }

      // Parse the message (assuming it's a Map)
      Object payload = redisTemplate.getValueSerializer().deserialize(message.getBody());
      if (payload instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> update = (Map<String, Object>) payload;
        String executionId = (String) update.get("executionId");
        String status = (String) update.get("status");

        if (executionId != null) {
          // Broadcast to all emitters for this execution
          broadcastToExecution(executionId);
        }

        // Broadcast to active executions emitters if status changed
        if (status != null
            && (TestStatus.RUNNING.name().equals(status)
                || TestStatus.PASSED.name().equals(status)
                || TestStatus.FAILED.name().equals(status))) {
          broadcastToActiveExecutions();
        }
      }
    } catch (Exception e) {
      log.error("Error processing Redis message: {}", e.getMessage(), e);
    }
  }

  /** Broadcast status update to all emitters for a specific execution. */
  private void broadcastToExecution(String executionId) {
    List<SseEmitter> emitters = executionEmitters.get(executionId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    var status = testMonitoringService.getStatus(executionId);
    if (status == null) {
      return;
    }

    emitters.removeIf(
        emitter -> {
          try {
            emitter.send(status, MediaType.APPLICATION_JSON);
            return false;
          } catch (IOException e) {
            return true;
          }
        });
  }

  /** Broadcast active executions update to all active executions emitters. */
  private void broadcastToActiveExecutions() {
    if (activeExecutionsEmitters.isEmpty()) {
      return;
    }

    var activeExecutions = testMonitoringService.getActiveExecutions();

    activeExecutionsEmitters.removeIf(
        emitter -> {
          try {
            emitter.send(activeExecutions, MediaType.APPLICATION_JSON);
            return false;
          } catch (IOException e) {
            return true;
          }
        });
  }

  /** Remove an emitter from the execution's emitter list. */
  private void removeExecutionEmitter(String executionId, SseEmitter emitter) {
    List<SseEmitter> emitters = executionEmitters.get(executionId);
    if (emitters != null) {
      emitters.remove(emitter);
      if (emitters.isEmpty()) {
        executionEmitters.remove(executionId);
      }
    }
  }
}
