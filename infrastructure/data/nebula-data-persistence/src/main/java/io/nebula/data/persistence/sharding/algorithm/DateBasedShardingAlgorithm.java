package io.nebula.data.persistence.sharding.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 基于日期的分片算法
 * 支持按年、月、日进行分片
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class DateBasedShardingAlgorithm implements StandardShardingAlgorithm<Date> {
    
    private Properties props;
    private DateTimeFormatter dateFormatter;
    private String shardingUnit; // YEAR, MONTH, DAY
    private String suffixPattern;
    
    @Override
    public void init(Properties props) {
        this.props = props;
        
        // 获取配置参数
        String datePattern = props.getProperty("date-pattern", "yyyy-MM-dd");
        this.dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        
        this.shardingUnit = props.getProperty("sharding-unit", "MONTH");
        this.suffixPattern = props.getProperty("suffix-pattern", "yyyyMM");
        
        log.info("Initialized DateBasedShardingAlgorithm with unit: {}, suffix-pattern: {}", 
                shardingUnit, suffixPattern);
    }
    
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Date> shardingValue) {
        Date date = shardingValue.getValue();
        
        if (date == null) {
            log.warn("Sharding date value is null, using first available target");
            return availableTargetNames.iterator().next();
        }
        
        // 计算分片后缀
        String suffix = calculateShardingSuffix(date);
        
        // 查找匹配的目标名称
        String targetName = findTargetBysuffix(availableTargetNames, suffix);
        
        log.debug("Sharding date: {}, calculated suffix: {}, target: {}", date, suffix, targetName);
        
        return targetName;
    }
    
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Date> shardingValue) {
        Set<String> result = new LinkedHashSet<>();
        
        Date lowerBound = shardingValue.getValueRange().lowerEndpoint();
        Date upperBound = shardingValue.getValueRange().upperEndpoint();
        
        if (lowerBound == null || upperBound == null) {
            log.warn("Range sharding date bounds are null, returning all available targets");
            return availableTargetNames;
        }
        
        // 计算范围内所有可能的分片后缀
        Set<String> suffixes = calculateRangeSuffixes(lowerBound, upperBound);
        
        // 根据后缀查找目标名称
        for (String suffix : suffixes) {
            String targetName = findTargetByPartialSuffix(availableTargetNames, suffix);
            if (targetName != null) {
                result.add(targetName);
            }
        }
        
        log.debug("Range sharding dates [{}, {}], calculated suffixes: {}, targets: {}", 
                 lowerBound, upperBound, suffixes, result);
        
        return result;
    }
    
    /**
     * 计算分片后缀
     */
    private String calculateShardingSuffix(Date date) {
        LocalDate localDate = LocalDate.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
        
        switch (shardingUnit.toUpperCase()) {
            case "YEAR":
                return localDate.format(DateTimeFormatter.ofPattern("yyyy"));
            case "MONTH":
                return localDate.format(DateTimeFormatter.ofPattern(suffixPattern));
            case "DAY":
                return localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            case "WEEK":
                // 按周分片，使用年份+周数
                int weekOfYear = localDate.getDayOfYear() / 7 + 1;
                return String.format("%d%02d", localDate.getYear(), weekOfYear);
            default:
                return localDate.format(DateTimeFormatter.ofPattern(suffixPattern));
        }
    }
    
    /**
     * 计算范围内的所有分片后缀
     */
    private Set<String> calculateRangeSuffixes(Date startDate, Date endDate) {
        Set<String> suffixes = new LinkedHashSet<>();
        
        LocalDate start = LocalDate.ofInstant(startDate.toInstant(), java.time.ZoneId.systemDefault());
        LocalDate end = LocalDate.ofInstant(endDate.toInstant(), java.time.ZoneId.systemDefault());
        
        switch (shardingUnit.toUpperCase()) {
            case "YEAR":
                calculateYearlySuffixes(start, end, suffixes);
                break;
            case "MONTH":
                calculateMonthlySuffixes(start, end, suffixes);
                break;
            case "DAY":
                calculateDailySuffixes(start, end, suffixes);
                break;
            case "WEEK":
                calculateWeeklySuffixes(start, end, suffixes);
                break;
            default:
                calculateMonthlySuffixes(start, end, suffixes);
                break;
        }
        
        return suffixes;
    }
    
    private void calculateYearlySuffixes(LocalDate start, LocalDate end, Set<String> suffixes) {
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            suffixes.add(String.valueOf(year));
        }
    }
    
    private void calculateMonthlySuffixes(LocalDate start, LocalDate end, Set<String> suffixes) {
        LocalDate current = start.withDayOfMonth(1);
        while (!current.isAfter(end)) {
            suffixes.add(current.format(DateTimeFormatter.ofPattern(suffixPattern)));
            current = current.plusMonths(1);
        }
    }
    
    private void calculateDailySuffixes(LocalDate start, LocalDate end, Set<String> suffixes) {
        LocalDate current = start;
        while (!current.isAfter(end)) {
            suffixes.add(current.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            current = current.plusDays(1);
        }
    }
    
    private void calculateWeeklySuffixes(LocalDate start, LocalDate end, Set<String> suffixes) {
        LocalDate current = start;
        while (!current.isAfter(end)) {
            int weekOfYear = current.getDayOfYear() / 7 + 1;
            suffixes.add(String.format("%d%02d", current.getYear(), weekOfYear));
            current = current.plusWeeks(1);
        }
    }
    
    /**
     * 根据后缀查找目标名称（精确匹配）
     */
    private String findTargetBysuffix(Collection<String> availableTargetNames, String suffix) {
        for (String targetName : availableTargetNames) {
            if (targetName.endsWith("_" + suffix) || targetName.endsWith(suffix)) {
                return targetName;
            }
        }
        
        // 如果没有找到精确匹配，返回第一个可用的目标
        log.warn("No target found for suffix: {}, using first available target", suffix);
        return availableTargetNames.iterator().next();
    }
    
    /**
     * 根据部分后缀查找目标名称（用于范围查询）
     */
    private String findTargetByPartialSuffix(Collection<String> availableTargetNames, String suffix) {
        for (String targetName : availableTargetNames) {
            if (targetName.contains(suffix)) {
                return targetName;
            }
        }
        return null;
    }
    
    @Override
    public String getType() {
        return "DATE_BASED";
    }
    
    @Override
    public Properties getProps() {
        return props;
    }
}
