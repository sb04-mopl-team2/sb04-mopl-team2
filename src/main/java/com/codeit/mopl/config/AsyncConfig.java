package com.codeit.mopl.config;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean
  public TaskDecorator contextPropagatingTaskDecorator() {
    return (delegate) -> {
      Map<String, String> callerMdc = MDC.getCopyOfContextMap();
      SecurityContext callerCtx = SecurityContextHolder.getContext();

      return () -> {
        Map<String, String> prevMdc = MDC.getCopyOfContextMap();
        SecurityContext prevCtx = SecurityContextHolder.getContext();
        try {
          if (callerMdc != null) MDC.setContextMap(callerMdc); else MDC.clear();
          SecurityContextHolder.setContext(callerCtx);

          delegate.run();
        } finally {
          if (prevMdc != null) MDC.setContextMap(prevMdc); else MDC.clear();
          SecurityContextHolder.setContext(prevCtx);
        }
      };
    };
  }

  @Bean(name = "taskExecutor")
  public Executor taskExecutor(TaskDecorator contextPropagatingTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("async-");
    executor.setTaskDecorator(contextPropagatingTaskDecorator);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  @Bean(name = "mailExecutor")
  public Executor mailExecutor(TaskDecorator contextPropagatingTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("sendMail-");
    executor.setTaskDecorator(contextPropagatingTaskDecorator);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}