package io.nebula.core.metrics;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 默认指标收集器实现
 * 基于Micrometer的实现
 */
@RequiredArgsConstructor
public class DefaultMetricsCollector implements MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    @Override
    public Counter counter(String name, String... tags) {
        return Counter.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public Counter counter(String name, Tags tags) {
        return Counter.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public Counter counter(String name, String description, String... tags) {
        return Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public Timer timer(String name, String... tags) {
        return Timer.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public Timer timer(String name, Tags tags) {
        return Timer.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public Timer timer(String name, String description, String... tags) {
        return Timer.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public <T extends Number> Gauge gauge(String name, Supplier<T> valueSupplier, String... tags) {
        return Gauge.builder(name, valueSupplier, value -> value.get().doubleValue())
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public <T extends Number> Gauge gauge(String name, Supplier<T> valueSupplier, Tags tags) {
        return Gauge.builder(name, valueSupplier, value -> value.get().doubleValue())
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public <T extends Number> Gauge gauge(String name, String description, Supplier<T> valueSupplier, String... tags) {
        return Gauge.builder(name, valueSupplier, value -> value.get().doubleValue())
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public DistributionSummary summary(String name, String... tags) {
        return DistributionSummary.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public DistributionSummary summary(String name, Tags tags) {
        return DistributionSummary.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public DistributionSummary summary(String name, String description, String... tags) {
        return DistributionSummary.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }
    
    @Override
    public void recordTime(String name, Runnable runnable, String... tags) {
        Timer timer = timer(name, tags);
        timer.record(runnable);
    }
    
    @Override
    public <T> T recordTime(String name, Supplier<T> supplier, String... tags) {
        Timer timer = timer(name, tags);
        return timer.record(supplier);
    }
    
    @Override
    public void recordTime(String name, Duration duration, String... tags) {
        Timer timer = timer(name, tags);
        timer.record(duration);
    }
    
    @Override
    public void increment(String name, String... tags) {
        counter(name, tags).increment();
    }
    
    @Override
    public void increment(String name, double amount, String... tags) {
        counter(name, tags).increment(amount);
    }
    
    @Override
    public void recordValue(String name, double value, String... tags) {
        summary(name, tags).record(value);
    }
    
    @Override
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    @Override
    public Duration stopTimer(Timer.Sample sample, String name, String... tags) {
        Timer timer = timer(name, tags);
        long nanos = sample.stop(timer);
        return Duration.ofNanos(nanos);
    }
    
    @Override
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}