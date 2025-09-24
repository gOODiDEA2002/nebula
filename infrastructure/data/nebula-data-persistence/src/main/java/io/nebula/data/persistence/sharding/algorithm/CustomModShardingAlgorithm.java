package io.nebula.data.persistence.sharding.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 自定义取模分片算法
 * 支持精确分片和范围分片
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class CustomModShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    
    private Properties props;
    private int shardingCount;
    
    @Override
    public void init(Properties props) {
        this.props = props;
        this.shardingCount = Integer.parseInt(props.getProperty("sharding-count", "2"));
        log.info("Initialized CustomModShardingAlgorithm with sharding-count: {}", shardingCount);
    }
    
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
        Long value = shardingValue.getValue();
        
        if (value == null) {
            log.warn("Sharding value is null, using first available target");
            return availableTargetNames.iterator().next();
        }
        
        // 计算分片索引
        int shardIndex = (int) (value % shardingCount);
        
        // 查找对应的目标名称
        String targetName = findTargetByIndex(availableTargetNames, shardIndex);
        
        log.debug("Sharding value: {}, calculated index: {}, target: {}", value, shardIndex, targetName);
        
        return targetName;
    }
    
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Long> shardingValue) {
        Set<String> result = new LinkedHashSet<>();
        
        Long lowerBound = shardingValue.getValueRange().lowerEndpoint();
        Long upperBound = shardingValue.getValueRange().upperEndpoint();
        
        if (lowerBound == null || upperBound == null) {
            log.warn("Range sharding bounds are null, returning all available targets");
            return availableTargetNames;
        }
        
        // 计算范围内所有可能的分片
        Set<Integer> shardIndexes = new LinkedHashSet<>();
        for (long i = lowerBound; i <= upperBound; i++) {
            int shardIndex = (int) (i % shardingCount);
            shardIndexes.add(shardIndex);
        }
        
        // 根据分片索引查找目标名称
        for (int shardIndex : shardIndexes) {
            String targetName = findTargetByIndex(availableTargetNames, shardIndex);
            if (targetName != null) {
                result.add(targetName);
            }
        }
        
        log.debug("Range sharding [{}, {}], calculated indexes: {}, targets: {}", 
                 lowerBound, upperBound, shardIndexes, result);
        
        return result;
    }
    
    /**
     * 根据分片索引查找目标名称
     */
    private String findTargetByIndex(Collection<String> availableTargetNames, int shardIndex) {
        String[] targets = availableTargetNames.toArray(new String[0]);
        
        if (shardIndex >= 0 && shardIndex < targets.length) {
            return targets[shardIndex];
        }
        
        // 如果索引超出范围，使用取模再次计算
        int adjustedIndex = shardIndex % targets.length;
        return targets[adjustedIndex];
    }
    
    @Override
    public String getType() {
        return "CUSTOM_MOD";
    }
    
    @Override
    public Properties getProps() {
        return props;
    }
}
