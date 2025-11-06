package org.project.tiiasuporte.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class MetricsAspect {

    private static final Logger logger = LoggerFactory.getLogger(MetricsAspect.class);
    private final MeterRegistry meterRegistry;

    public MetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("execution(* org.project.tiiasuporte..*.ping(..)) || " +
            "execution(* org.project.tiiasuporte..*.dnsLookup(..)) || " +
            "execution(* org.project.tiiasuporte..*.obterLocalizacao(..)) || " +
            "execution(* org.project.tiiasuporte..*.getDomainInfo(..)) || " +
            "execution(* org.project.tiiasuporte..*.scanPorts(..)) || " +
            "execution(* org.project.tiiasuporte..*.traceroute(..))")
    public Object trackMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        long startTime = System.nanoTime();

        // Increment request counter
        Counter.builder("tiia.requests")
            .tag("service", className)
            .tag("method", methodName)
            .description("Total number of requests per service")
            .register(meterRegistry)
            .increment();

        try {
            Object result = joinPoint.proceed();

            // Track success
            Counter.builder("tiia.requests.success")
                .tag("service", className)
                .tag("method", methodName)
                .register(meterRegistry)
                .increment();

            return result;
        } catch (Exception e) {
            // Track errors
            Counter.builder("tiia.requests.error")
                .tag("service", className)
                .tag("method", methodName)
                .tag("exception", e.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();

            throw e;
        } finally {
            // Track execution time
            long duration = System.nanoTime() - startTime;
            Timer.builder("tiia.requests.duration")
                .tag("service", className)
                .tag("method", methodName)
                .description("Execution time of service methods")
                .register(meterRegistry)
                .record(duration, TimeUnit.NANOSECONDS);

            logger.debug("Metrics recorded for {}.{}: {}ms",
                className, methodName, TimeUnit.NANOSECONDS.toMillis(duration));
        }
    }
}
