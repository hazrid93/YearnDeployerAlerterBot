package com.azad.yearn.deployer.utils;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final String TASK_EXECUTOR_NAME_PREFIX_DEFAULT = "taskExecutor-";
    private static final String TASK_EXECUTOR_NAME_PREFIX_REPOSITORY = "serviceTaskExecutor-";
    private static final String TASK_EXECUTOR_NAME_PREFIX_CONTROLLER = "controllerTaskExecutor-";
    private static final String TASK_EXECUTOR_NAME_PREFIX_SERVICE = "serviceTaskExecutor-";
    private static final String TASK_EXECUTOR_NAME_PREFIX_FORK = "forkTaskExecutor-";

    public static final String TASK_EXECUTOR_REPOSITORY = "repositoryTaskExecutor";
    public static final String TASK_EXECUTOR_SERVICE = "serviceTaskExecutor";
    public static final String TASK_EXECUTOR_CONTROLLER = "controllerTaskExecutor";
    public static final String TASK_EXECUTOR_FORK = "forkTaskExecutor";
    public static final String TASK_EXECUTOR_DEFAULT = "taskExecutor";

    @Autowired
    private AppProperties applicationProperties;

    @Override
    @Bean(name = TASK_EXECUTOR_DEFAULT)
    public Executor getAsyncExecutor() {
        return newTaskExecutor(TASK_EXECUTOR_NAME_PREFIX_DEFAULT);
    }
    /**
    @Bean(name = TASK_EXECUTOR_REPOSITORY)
    public Executor getRepositoryAsyncExecutor() {
        return newTaskExecutor(TASK_EXECUTOR_NAME_PREFIX_REPOSITORY);
    }

    @Bean(name = TASK_EXECUTOR_SERVICE)
    public Executor getServiceAsyncExecutor() {
        return newTaskExecutor(TASK_EXECUTOR_NAME_PREFIX_SERVICE);
    }

    @Bean(name = TASK_EXECUTOR_CONTROLLER)
    public Executor getControllerAsyncExecutor() {
        return newTaskExecutor(TASK_EXECUTOR_NAME_PREFIX_CONTROLLER);
    }
    **/
    @Bean(name = TASK_EXECUTOR_FORK)
    public Executor getForkAsyncExecutor() {
        return newForkTaskExecutor(TASK_EXECUTOR_NAME_PREFIX_FORK);
    }


    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    private Executor newTaskExecutor(final String taskExecutorNamePrefix) {
        final AppProperties.Async asyncProperties = applicationProperties.getAsync();
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutorNamePrefix);
        return executor;
    }

    private Executor newForkTaskExecutor(final String taskExecutorNamePrefix) {
        final AppProperties.Async asyncProperties = applicationProperties.getAsync();
        final ForkJoinPool executor = new ForkJoinPool(applicationProperties.getAsync().getForkThreadSize());
        return executor;
    }

    /* Handle this under contextclosehandler instead
    @PreDestroy
    public void beandestroy() {
        System.out.print("STOPPING");
    }
     */

}
