package com.atas.framework.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for test execution thread pool with bounded resources. */
@Configuration
@Slf4j
public class ExecutorConfig {

  @Value("${ataas.execution.core-pool-size:5}")
  private int corePoolSize;

  @Value("${ataas.execution.max-pool-size:20}")
  private int maxPoolSize;

  @Value("${ataas.execution.queue-capacity:100}")
  private int queueCapacity;

  @Value("${ataas.execution.keep-alive-seconds:60}")
  private long keepAliveSeconds;

  @Value("${ataas.execution.output-capture-pool-size:50}")
  private int outputCapturePoolSize;

  @Bean(name = "testExecutionExecutor")
  public ExecutorService testExecutionExecutor() {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadFactoryBuilder()
                .setNameFormat("test-exec-%d")
                .setDaemon(false)
                .setUncaughtExceptionHandler(
                    (t, e) ->
                        log.error(
                            "Uncaught exception in test execution thread: {}", t.getName(), e))
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // Reject policy: caller runs
            );

    // Allow core threads to timeout if idle
    executor.allowCoreThreadTimeOut(true);

    log.info(
        "Configured test execution executor: core={}, max={}, queue={}",
        corePoolSize,
        maxPoolSize,
        queueCapacity);

    return executor;
  }

  /**
   * Separate executor for output capture tasks. This prevents output capture from consuming threads
   * from the main test execution pool, allowing more tests to run concurrently.
   */
  @Bean(name = "outputCaptureExecutor")
  public ExecutorService outputCaptureExecutor() {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            outputCapturePoolSize,
            outputCapturePoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactoryBuilder()
                .setNameFormat("output-capture-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(
                    (t, e) ->
                        log.error(
                            "Uncaught exception in output capture thread: {}", t.getName(), e))
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    log.info("Configured output capture executor: pool-size={}", outputCapturePoolSize);

    return executor;
  }

  /** Shutdown hook to gracefully shutdown executor on application stop. */
  @Bean
  public ExecutorServiceShutdownHook executorServiceShutdownHook() {
    return new ExecutorServiceShutdownHook();
  }

  public static class ExecutorServiceShutdownHook {
    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(ExecutorServiceShutdownHook.class);

    public void shutdown(ExecutorService executor, String name) {
      log.info("Shutting down executor: {}", name);
      executor.shutdown();
      try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          log.warn("Executor {} did not terminate gracefully, forcing shutdown", name);
          executor.shutdownNow();
          if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            log.error("Executor {} did not terminate after forced shutdown", name);
          }
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
