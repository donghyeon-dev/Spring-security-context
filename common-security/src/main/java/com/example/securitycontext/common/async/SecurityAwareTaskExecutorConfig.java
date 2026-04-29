package com.example.securitycontext.common.async;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class SecurityAwareTaskExecutorConfig implements AsyncConfigurer {

    public SecurityAwareTaskExecutorConfig() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(4);
        pool.setMaxPoolSize(16);
        pool.setQueueCapacity(64);
        pool.setThreadNamePrefix("app-async-");
        pool.setTaskDecorator(new MdcTaskDecorator());
        pool.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(pool);
    }
}
