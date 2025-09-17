package io.nebula.core.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.DistributionSummary;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 指标收集器接口
 * 提供统一的指标收集API
 */
public interface MetricsCollector {
    
    /**
     * 创建或获取计数器
     * 
     * @param name 指标名称
     * @param tags 标签
     * @return 计数器
     */
    Counter counter(String name, String... tags);
    
    /**
     * 创建或获取计数器
     * 
     * @param name 指标名称
     * @param tags 标签
     * @return 计数器
     */
    Counter counter(String name, Tags tags);
    
    /**
     * 创建或获取计数器
     * 
     * @param name        指标名称
     * @param description 描述
     * @param tags        标签
     * @return 计数器
     */
    Counter counter(String name, String description, String... tags);
    
    /**
     * 创建或获取计时器
     * 
     * @param name 指标名称
     * @param tags 标签
     * @return 计时器
     */
    Timer timer(String name, String... tags);
    
    /**
     * 创建或获取计时器
     * 
     * @param name 指标名称
     * @param tags 标签
     * @return 计时器
     */
    Timer timer(String name, Tags tags);
    
    /**
     * 创建或获取计时器
     * 
     * @param name        指标名称
     * @param description 描述
     * @param tags        标签
     * @return 计时器
     */
    Timer timer(String name, String description, String... tags);
    
    /**
     * 创建或获取仪表盘
     * 
     * @param name          指标名称
     * @param valueSupplier 值供应器
     * @param tags          标签
     * @param <T>           值类型
     * @return 仪表盘
     */
    <T extends Number> Gauge gauge(String name, Supplier<T> valueSupplier, String... tags);
    
    /**
     * 创建或获取仪表盘
     * 
     * @param name          指标名称
     * @param valueSupplier 值供应器
     * @param tags          标签
     * @param <T>           值类型
     * @return 仪表盘
     */
    <T extends Number> Gauge gauge(String name, Supplier<T> valueSupplier, Tags tags);
    
    /**
     * 创建或获取仪表盘
     * 
     * @param name          指标名称
     * @param description   描述
     * @param valueSupplier 值供应器
     * @param tags          标签
     * @param <T>           值类型
     * @return 仪表盘
     */
    <T extends Number> Gauge gauge(String name, String description, Supplier<T> valueSupplier, String... tags);
    
    /**
     * 创建或获取分布摘要
     * 
     * @param name 指标名称
     * @param tags 标签
     * @return 分布摘要
     */
    DistributionSummary summary(String name, String... tags);
    
    /**
     * 创建或获取分布摘要
     * 
     * @param name 指标名称
     * @param tags 标签
     * @return 分布摘要
     */
    DistributionSummary summary(String name, Tags tags);
    
    /**
     * 创建或获取分布摘要
     * 
     * @param name        指标名称
     * @param description 描述
     * @param tags        标签
     * @return 分布摘要
     */
    DistributionSummary summary(String name, String description, String... tags);
    
    /**
     * 记录计时操作
     * 
     * @param name     指标名称
     * @param runnable 操作
     * @param tags     标签
     */
    void recordTime(String name, Runnable runnable, String... tags);
    
    /**
     * 记录计时操作
     * 
     * @param name     指标名称
     * @param supplier 操作
     * @param tags     标签
     * @param <T>      返回类型
     * @return 操作结果
     */
    <T> T recordTime(String name, Supplier<T> supplier, String... tags);
    
    /**
     * 记录时间
     * 
     * @param name     指标名称
     * @param duration 时长
     * @param tags     标签
     */
    void recordTime(String name, Duration duration, String... tags);
    
    /**
     * 增加计数
     * 
     * @param name 指标名称
     * @param tags 标签
     */
    void increment(String name, String... tags);
    
    /**
     * 增加计数
     * 
     * @param name   指标名称
     * @param amount 增加量
     * @param tags   标签
     */
    void increment(String name, double amount, String... tags);
    
    /**
     * 记录值
     * 
     * @param name  指标名称
     * @param value 值
     * @param tags  标签
     */
    void recordValue(String name, double value, String... tags);
    
    /**
     * 创建计时器样本
     * 
     * @return 计时器样本
     */
    Timer.Sample startTimer();
    
    /**
     * 停止计时器样本
     * 
     * @param sample 计时器样本
     * @param name   指标名称
     * @param tags   标签
     * @return 记录的时长
     */
    Duration stopTimer(Timer.Sample sample, String name, String... tags);
    
    /**
     * 获取底层的MeterRegistry
     * 
     * @return MeterRegistry
     */
    MeterRegistry getMeterRegistry();
}
