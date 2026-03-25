package com.project.suporte.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(DiagnosticsProperties.class)
public class AppConfig {

    @Bean(name = "portScanExecutor", destroyMethod = "shutdown")
    public Executor portScanExecutor(DiagnosticsProperties properties) {
        return Executors.newFixedThreadPool(
                properties.getPortscan().getThreadPoolSize(),
                Thread.ofPlatform().name("portscan-", 0).factory()
        );
    }
}
