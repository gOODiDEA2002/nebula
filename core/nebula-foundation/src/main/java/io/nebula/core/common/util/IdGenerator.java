package io.nebula.core.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器工具类
 * 提供各种类型的ID生成功能
 */
public final class IdGenerator {
    
    /**
     * 雪花算法相关常量
     */
    private static final long EPOCH = 1640995200000L; // 2022-01-01 00:00:00 UTC
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    
    /**
     * 默认雪花算法实例
     */
    private static final SnowflakeIdGenerator DEFAULT_SNOWFLAKE = new SnowflakeIdGenerator(1, 1);
    
    /**
     * 随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * 短ID字符集
     */
    private static final String SHORT_ID_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    
    /**
     * 私有构造函数，防止实例化
     */
    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ====================
    // UUID生成
    // ====================
    
    /**
     * 生成标准UUID
     * 
     * @return UUID字符串
     */
    public static String uuid() {
        return java.util.UUID.randomUUID().toString();
    }
    
    /**
     * 生成无横线的UUID
     * 
     * @return 无横线的UUID字符串
     */
    public static String uuidSimple() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成大写的UUID
     * 
     * @return 大写UUID字符串
     */
    public static String uuidUpper() {
        return java.util.UUID.randomUUID().toString().toUpperCase();
    }
    
    /**
     * 生成大写无横线的UUID
     * 
     * @return 大写无横线的UUID字符串
     */
    public static String uuidSimpleUpper() {
        return java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
    
    // ====================
    // 雪花算法ID生成
    // ====================
    
    /**
     * 生成雪花算法ID（使用默认实例）
     * 
     * @return 雪花算法ID
     */
    public static long snowflakeId() {
        return DEFAULT_SNOWFLAKE.nextId();
    }
    
    /**
     * 生成雪花算法ID字符串（使用默认实例）
     * 
     * @return 雪花算法ID字符串
     */
    public static String snowflakeIdString() {
        return String.valueOf(DEFAULT_SNOWFLAKE.nextId());
    }
    
    /**
     * 创建雪花算法ID生成器
     * 
     * @param workerId     工作机器ID（0-31）
     * @param datacenterId 数据中心ID（0-31）
     * @return 雪花算法ID生成器实例
     */
    public static SnowflakeIdGenerator createSnowflake(long workerId, long datacenterId) {
        return new SnowflakeIdGenerator(workerId, datacenterId);
    }
    
    // ====================
    // 时间戳ID生成
    // ====================
    
    /**
     * 生成基于时间戳的ID
     * 
     * @return 时间戳ID
     */
    public static long timestampId() {
        return Instant.now().toEpochMilli();
    }
    
    /**
     * 生成基于时间戳的ID字符串
     * 
     * @return 时间戳ID字符串
     */
    public static String timestampIdString() {
        return String.valueOf(Instant.now().toEpochMilli());
    }
    
    /**
     * 生成基于纳秒时间戳的ID
     * 
     * @return 纳秒时间戳ID
     */
    public static long nanoTimestampId() {
        return System.nanoTime();
    }
    
    // ====================
    // 随机数ID生成
    // ====================
    
    /**
     * 生成指定长度的数字ID
     * 
     * @param length ID长度
     * @return 数字ID字符串
     */
    public static String numericId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                // 第一位不能为0
                sb.append(SECURE_RANDOM.nextInt(9) + 1);
            } else {
                sb.append(SECURE_RANDOM.nextInt(10));
            }
        }
        return sb.toString();
    }
    
    /**
     * 生成指定长度的字母数字ID
     * 
     * @param length ID长度
     * @return 字母数字ID字符串
     */
    public static String alphanumericId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(SHORT_ID_CHARS.length());
            sb.append(SHORT_ID_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }
    
    /**
     * 生成随机长整型ID
     * 
     * @return 随机长整型ID
     */
    public static long randomLongId() {
        return Math.abs(SECURE_RANDOM.nextLong());
    }
    
    // ====================
    // 短ID生成
    // ====================
    
    /**
     * 生成短ID（8位字母数字）
     * 
     * @return 短ID字符串
     */
    public static String shortId() {
        return alphanumericId(8);
    }
    
    /**
     * 生成短ID（指定长度）
     * 
     * @param length ID长度
     * @return 短ID字符串
     */
    public static String shortId(int length) {
        return alphanumericId(length);
    }
    
    // ====================
    // 业务ID生成
    // ====================
    
    /**
     * 生成带前缀的ID
     * 
     * @param prefix 前缀
     * @param length ID长度（不包括前缀）
     * @return 带前缀的ID
     */
    public static String prefixedId(String prefix, int length) {
        if (Strings.isBlank(prefix)) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }
        return prefix + alphanumericId(length);
    }
    
    /**
     * 生成带前缀的数字ID
     * 
     * @param prefix 前缀
     * @param length ID长度（不包括前缀）
     * @return 带前缀的数字ID
     */
    public static String prefixedNumericId(String prefix, int length) {
        if (Strings.isBlank(prefix)) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }
        return prefix + numericId(length);
    }
    
    /**
     * 生成订单号（日期+随机数字）
     * 
     * @return 订单号
     */
    public static String orderNo() {
        String dateStr = DateUtils.formatDateTime(DateUtils.nowDateTime(), "yyyyMMddHHmmss");
        String randomStr = numericId(6);
        return dateStr + randomStr;
    }
    
    /**
     * 生成用户ID（字母开头的字母数字组合）
     * 
     * @return 用户ID
     */
    public static String userId() {
        char firstChar = (char) ('A' + SECURE_RANDOM.nextInt(26));
        return firstChar + alphanumericId(7);
    }
    
    // ====================
    // 雪花算法实现类
    // ====================
    
    /**
     * 雪花算法ID生成器
     */
    public static class SnowflakeIdGenerator {
        
        private final long workerId;
        private final long datacenterId;
        
        private long sequence = 0L;
        private long lastTimestamp = -1L;
        
        private final Object lock = new Object();
        
        /**
         * 构造函数
         * 
         * @param workerId     工作机器ID（0-31）
         * @param datacenterId 数据中心ID（0-31）
         */
        public SnowflakeIdGenerator(long workerId, long datacenterId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException(
                        String.format("Worker ID can't be greater than %d or less than 0", MAX_WORKER_ID));
            }
            if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
                throw new IllegalArgumentException(
                        String.format("Datacenter ID can't be greater than %d or less than 0", MAX_DATACENTER_ID));
            }
            
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }
        
        /**
         * 生成下一个ID
         * 
         * @return 雪花算法ID
         */
        public long nextId() {
            synchronized (lock) {
                long timestamp = timeGen();
                
                // 时钟回拨检查
                if (timestamp < lastTimestamp) {
                    throw new RuntimeException(
                            String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
                                    lastTimestamp - timestamp));
                }
                
                // 同一毫秒内的序号
                if (lastTimestamp == timestamp) {
                    sequence = (sequence + 1) & SEQUENCE_MASK;
                    if (sequence == 0) {
                        // 序号溢出，等待下一毫秒
                        timestamp = tilNextMillis(lastTimestamp);
                    }
                } else {
                    sequence = 0L;
                }
                
                lastTimestamp = timestamp;
                
                // 构造ID
                return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                        | (datacenterId << DATACENTER_ID_SHIFT)
                        | (workerId << WORKER_ID_SHIFT)
                        | sequence;
            }
        }
        
        /**
         * 等待到下一毫秒
         * 
         * @param lastTimestamp 上次时间戳
         * @return 下一毫秒时间戳
         */
        private long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen();
            }
            return timestamp;
        }
        
        /**
         * 获取当前时间戳
         * 
         * @return 当前时间戳
         */
        private long timeGen() {
            return System.currentTimeMillis();
        }
        
        /**
         * 解析雪花算法ID
         * 
         * @param id 雪花算法ID
         * @return ID信息
         */
        public IdInfo parseId(long id) {
            long timestamp = (id >> TIMESTAMP_LEFT_SHIFT) + EPOCH;
            long datacenterId = (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
            long workerId = (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
            long sequence = id & SEQUENCE_MASK;
            
            return new IdInfo(timestamp, datacenterId, workerId, sequence);
        }
    }
    
    /**
     * ID解析信息
     */
    public static class IdInfo {
        private final long timestamp;
        private final long datacenterId;
        private final long workerId;
        private final long sequence;
        
        public IdInfo(long timestamp, long datacenterId, long workerId, long sequence) {
            this.timestamp = timestamp;
            this.datacenterId = datacenterId;
            this.workerId = workerId;
            this.sequence = sequence;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public long getDatacenterId() {
            return datacenterId;
        }
        
        public long getWorkerId() {
            return workerId;
        }
        
        public long getSequence() {
            return sequence;
        }
        
        @Override
        public String toString() {
            return String.format("IdInfo{timestamp=%d, datacenterId=%d, workerId=%d, sequence=%d}",
                    timestamp, datacenterId, workerId, sequence);
        }
    }
    
    // ====================
    // 序列号生成器
    // ====================
    
    /**
     * 简单序列号生成器
     */
    public static class SequenceGenerator {
        private final AtomicLong sequence;
        private final long start;
        private final long max;
        
        /**
         * 构造函数
         * 
         * @param start 起始值
         * @param max   最大值（达到后重置为起始值）
         */
        public SequenceGenerator(long start, long max) {
            if (start < 0 || max <= start) {
                throw new IllegalArgumentException("Invalid start or max value");
            }
            this.start = start;
            this.max = max;
            this.sequence = new AtomicLong(start);
        }
        
        /**
         * 获取下一个序列号
         * 
         * @return 序列号
         */
        public long nextValue() {
            long current = sequence.getAndIncrement();
            if (current >= max) {
                // 重置序列号
                if (sequence.compareAndSet(current + 1, start)) {
                    return start;
                } else {
                    // 并发情况下可能已经被其他线程重置，直接返回当前值
                    return sequence.get();
                }
            }
            return current;
        }
        
        /**
         * 获取当前序列号
         * 
         * @return 当前序列号
         */
        public long currentValue() {
            return sequence.get();
        }
        
        /**
         * 重置序列号到起始值
         */
        public void reset() {
            sequence.set(start);
        }
    }
}
