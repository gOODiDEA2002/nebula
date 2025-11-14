package io.nebula.core.common.util;

import io.nebula.core.common.util.IdGenerator.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * IdGenerator单元测试
 */
class IdGeneratorTest {
    
    // ====================
    // UUID生成测试
    // ====================
    
    @Test
    void testUuidGeneration() {
        String uuid = IdGenerator.uuid();
        
        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(36);  // 标准UUID长度：8-4-4-4-12 = 36
        assertThat(uuid).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
    }
    
    @Test
    void testUuidSimple() {
        String uuid = IdGenerator.uuidSimple();
        
        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(32);  // 无横线UUID长度：32
        assertThat(uuid).matches("^[a-f0-9]{32}$");
        assertThat(uuid).doesNotContain("-");
    }
    
    @Test
    void testUuidUpper() {
        String uuid = IdGenerator.uuidUpper();
        
        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(36);
        assertThat(uuid).matches("^[A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12}$");
        assertThat(uuid).isUpperCase();
    }
    
    @Test
    void testUuidUniqueness() {
        Set<String> uuids = new HashSet<>();
        int count = 1000;
        
        for (int i = 0; i < count; i++) {
            uuids.add(IdGenerator.uuid());
        }
        
        // 验证UUID唯一性
        assertThat(uuids).hasSize(count);
    }
    
    // ====================
    // 雪花算法ID测试
    // ====================
    
    @Test
    void testSnowflakeId() {
        long id = IdGenerator.snowflakeId();
        
        assertThat(id).isPositive();
    }
    
    @Test
    void testSnowflakeIdString() {
        String id = IdGenerator.snowflakeIdString();
        
        assertThat(id).isNotNull();
        assertThat(id).matches("^\\d+$");  // 纯数字字符串
        assertThat(Long.parseLong(id)).isPositive();
    }
    
    @Test
    void testSnowflakeIdUniqueness() {
        Set<Long> ids = new HashSet<>();
        int count = 1000;
        
        for (int i = 0; i < count; i++) {
            ids.add(IdGenerator.snowflakeId());
        }
        
        // 验证雪花ID唯一性
        assertThat(ids).hasSize(count);
    }
    
    @Test
    void testSnowflakeIdIncreasing() {
        long id1 = IdGenerator.snowflakeId();
        long id2 = IdGenerator.snowflakeId();
        long id3 = IdGenerator.snowflakeId();
        
        // 验证雪花ID递增性
        assertThat(id2).isGreaterThan(id1);
        assertThat(id3).isGreaterThan(id2);
    }
    
    @Test
    void testCreateSnowflake() {
        SnowflakeIdGenerator generator = IdGenerator.createSnowflake(1, 1);
        
        assertThat(generator).isNotNull();
        
        long id = generator.nextId();
        assertThat(id).isPositive();
    }
    
    @Test
    void testCreateSnowflakeInvalidWorkerId() {
        assertThatThrownBy(() -> IdGenerator.createSnowflake(32, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Worker ID");
    }
    
    @Test
    void testCreateSnowflakeInvalidDatacenterId() {
        assertThatThrownBy(() -> IdGenerator.createSnowflake(1, 32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Datacenter ID");
    }
    
    // ====================
    // 业务ID测试
    // ====================
    
    @Test
    void testOrderNo() {
        String orderNo = IdGenerator.orderNo();
        
        assertThat(orderNo).isNotNull();
        assertThat(orderNo).hasSize(20);  // yyyyMMddHHmmss(14位) + 6位随机数
        assertThat(orderNo).matches("^\\d{20}$");
    }
    
    @Test
    void testOrderNoFormat() {
        String orderNo = IdGenerator.orderNo();
        String datePrefix = orderNo.substring(0, 8);  // yyyyMMdd
        
        // 验证日期部分格式正确
        assertThat(datePrefix).matches("^\\d{8}$");
        int year = Integer.parseInt(datePrefix.substring(0, 4));
        assertThat(year).isBetween(2020, 2100);
    }
    
    @Test
    void testUserId() {
        String userId = IdGenerator.userId();
        
        assertThat(userId).isNotNull();
        assertThat(userId).hasSize(8);
        assertThat(userId.charAt(0)).isBetween('A', 'Z');  // 首字母大写
    }
    
    @Test
    void testPrefixedId() {
        String id = IdGenerator.prefixedId("USER", 10);
        
        assertThat(id).isNotNull();
        assertThat(id).startsWith("USER");
        assertThat(id).hasSize(14);  // USER(4) + 10位
    }
    
    @Test
    void testPrefixedIdInvalidPrefix() {
        assertThatThrownBy(() -> IdGenerator.prefixedId("", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prefix");
        
        assertThatThrownBy(() -> IdGenerator.prefixedId(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prefix");
    }
    
    @Test
    void testPrefixedNumericId() {
        String id = IdGenerator.prefixedNumericId("ORDER", 10);
        
        assertThat(id).isNotNull();
        assertThat(id).startsWith("ORDER");
        assertThat(id).hasSize(15);  // ORDER(5) + 10位
        
        // 验证后10位是数字
        String numericPart = id.substring(5);
        assertThat(numericPart).matches("^\\d{10}$");
    }
    
    @Test
    void testShortId() {
        String id = IdGenerator.shortId();
        
        assertThat(id).isNotNull();
        assertThat(id).hasSize(8);  // 默认8位
        assertThat(id).matches("^[0-9A-Za-z]+$");
    }
    
    @Test
    void testShortIdWithLength() {
        String id = IdGenerator.shortId(12);
        
        assertThat(id).isNotNull();
        assertThat(id).hasSize(12);
        assertThat(id).matches("^[0-9A-Za-z]+$");
    }
    
    // ====================
    // 数字ID测试
    // ====================
    
    @Test
    void testNumericId() {
        String id = IdGenerator.numericId(10);
        
        assertThat(id).isNotNull();
        assertThat(id).hasSize(10);
        assertThat(id).matches("^\\d{10}$");
        assertThat(id.charAt(0)).isNotEqualTo('0');  // 第一位不能为0
    }
    
    @Test
    void testNumericIdInvalidLength() {
        assertThatThrownBy(() -> IdGenerator.numericId(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Length must be positive");
        
        assertThatThrownBy(() -> IdGenerator.numericId(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Length must be positive");
    }
    
    @Test
    void testAlphanumericId() {
        String id = IdGenerator.alphanumericId(16);
        
        assertThat(id).isNotNull();
        assertThat(id).hasSize(16);
        assertThat(id).matches("^[0-9A-Za-z]+$");
    }
    
    @Test
    void testRandomLongId() {
        long id = IdGenerator.randomLongId();
        
        assertThat(id).isPositive();
    }
    
    // ====================
    // 时间戳ID测试
    // ====================
    
    @Test
    void testTimestampId() {
        long id = IdGenerator.timestampId();
        
        assertThat(id).isPositive();
        assertThat(id).isGreaterThan(1640995200000L);  // 2022-01-01之后
    }
    
    @Test
    void testTimestampIdString() {
        String id = IdGenerator.timestampIdString();
        
        assertThat(id).isNotNull();
        assertThat(id).matches("^\\d{13}$");  // 13位时间戳（毫秒）
    }
    
    // ====================
    // SnowflakeIdGenerator测试
    // ====================
    
    @Test
    void testSnowflakeIdGeneratorParseId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(5, 10);
        long id = generator.nextId();
        
        IdGenerator.IdInfo info = generator.parseId(id);
        
        assertThat(info).isNotNull();
        assertThat(info.getWorkerId()).isEqualTo(5);
        assertThat(info.getDatacenterId()).isEqualTo(10);
        assertThat(info.getTimestamp()).isGreaterThan(1640995200000L);
        assertThat(info.getSequence()).isGreaterThanOrEqualTo(0);
    }
    
    // ====================
    // SequenceGenerator测试
    // ====================
    
    @Test
    void testSequenceGenerator() {
        IdGenerator.SequenceGenerator generator = new IdGenerator.SequenceGenerator(1, 100);
        
        long value1 = generator.nextValue();
        long value2 = generator.nextValue();
        long value3 = generator.nextValue();
        
        assertThat(value1).isEqualTo(1);
        assertThat(value2).isEqualTo(2);
        assertThat(value3).isEqualTo(3);
    }
    
    @Test
    void testSequenceGeneratorReset() {
        IdGenerator.SequenceGenerator generator = new IdGenerator.SequenceGenerator(1, 100);
        
        generator.nextValue();
        generator.nextValue();
        generator.reset();
        
        long value = generator.nextValue();
        assertThat(value).isEqualTo(1);
    }
    
    @Test
    void testSequenceGeneratorCurrentValue() {
        IdGenerator.SequenceGenerator generator = new IdGenerator.SequenceGenerator(1, 100);
        
        generator.nextValue();
        generator.nextValue();
        
        long current = generator.currentValue();
        assertThat(current).isEqualTo(3);
    }
    
    @Test
    void testSequenceGeneratorInvalidArguments() {
        assertThatThrownBy(() -> new IdGenerator.SequenceGenerator(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid start or max value");
        
        assertThatThrownBy(() -> new IdGenerator.SequenceGenerator(100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid start or max value");
    }
}

