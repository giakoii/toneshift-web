package vn.toneshift.api.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import vn.toneshift.api.common.async.LoggingAsyncUncaughtExceptionHandler;
import vn.toneshift.api.common.async.MdcTaskDecorator;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Runs post-commit event listeners off the request thread.
 */
@Configuration
@EnableAsync
public class AsyncEventConfig implements AsyncConfigurer {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 8;
    private static final int QUEUE_CAPACITY = 500;

    /**
     * Builds the executor used by asynchronous event listeners.
     *
     * @return the event executor
     */
    @Bean("eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("event-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        // Runs the task on the caller when the queue is full: slowing the caller down is better
        // than silently dropping an event.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return eventTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }
}
