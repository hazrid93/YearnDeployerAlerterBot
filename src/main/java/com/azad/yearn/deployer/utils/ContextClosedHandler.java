package com.azad.yearn.deployer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ContextClosedHandler implements ApplicationListener<ContextClosedEvent>, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(ContextClosedHandler.class);

    private ApplicationContext context;


    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        Map<String, ThreadPoolTaskScheduler> schedulers = context.getBeansOfType(ThreadPoolTaskScheduler.class);

        for (ThreadPoolTaskScheduler scheduler : schedulers.values()) {
            scheduler.getScheduledExecutor().shutdown();
            try {
                scheduler.getScheduledExecutor().awaitTermination(20000, TimeUnit.MILLISECONDS);
                if(scheduler.getScheduledExecutor().isTerminated() || scheduler.getScheduledExecutor().isShutdown())
                    log.info("Scheduler "+scheduler.getThreadNamePrefix() + " has stoped");
                else{
                    log.info("Scheduler "+scheduler.getThreadNamePrefix() + " has not stoped normally and will be shut down immediately");
                    scheduler.getScheduledExecutor().shutdownNow();
                    log.info("Scheduler "+scheduler.getThreadNamePrefix() + " has shut down immediately");
                }
            } catch (IllegalStateException e) {
                log.error("Error " + e);
            } catch (InterruptedException e) {
                log.error("Error " + e);
            }
        }

        Map<String, ThreadPoolTaskExecutor> executers = context.getBeansOfType(ThreadPoolTaskExecutor.class);

        for (ThreadPoolTaskExecutor executor: executers.values()) {
            int retryCount = 0;
            while(executor.getActiveCount()>0 && ++retryCount<51){
                try {
                    log.info("Executer "+executor.getThreadNamePrefix()+" is still working with active " + executor.getActiveCount()+" work. Retry count is "+retryCount);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Error " + e);
                }
            }
            if(!(retryCount<51))
                log.info("Executer "+executor.getThreadNamePrefix()+" is still working.Since Retry count exceeded max value "+retryCount+", will be killed immediately");
            executor.shutdown();
            log.info("Executer "+executor.getThreadNamePrefix()+" with active " + executor.getActiveCount()+" work has killed");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}