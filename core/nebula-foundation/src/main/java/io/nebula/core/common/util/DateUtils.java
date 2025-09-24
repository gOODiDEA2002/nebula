package io.nebula.core.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 日期时间工具类
 * 提供常用的日期时间操作功能
 */
public final class DateUtils {
    
    /**
     * 常用日期时间格式
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String COMPACT_DATE_FORMAT = "yyyyMMdd";
    public static final String COMPACT_DATETIME_FORMAT = "yyyyMMddHHmmss";
    
    /**
     * 常用格式化器
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT);
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATETIME_FORMAT);
    public static final DateTimeFormatter COMPACT_DATE_FORMATTER = DateTimeFormatter.ofPattern(COMPACT_DATE_FORMAT);
    public static final DateTimeFormatter COMPACT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(COMPACT_DATETIME_FORMAT);
    
    /**
     * 默认时区
     */
    public static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    public static final ZoneId UTC_ZONE = ZoneOffset.UTC;
    
    /**
     * 私有构造函数，防止实例化
     */
    private DateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ====================
    // 当前时间获取
    // ====================
    
    /**
     * 获取当前日期
     * 
     * @return 当前日期
     */
    public static LocalDate now() {
        return LocalDate.now();
    }
    
    /**
     * 获取当前日期时间
     * 
     * @return 当前日期时间
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }
    
    /**
     * 获取当前时间戳（秒）
     * 
     * @return 当前时间戳
     */
    public static long nowTimestamp() {
        return Instant.now().getEpochSecond();
    }
    
    /**
     * 获取当前时间戳（毫秒）
     * 
     * @return 当前时间戳（毫秒）
     */
    public static long nowTimestampMillis() {
        return Instant.now().toEpochMilli();
    }
    
    // ====================
    // 格式化
    // ====================
    
    /**
     * 格式化日期为字符串（默认格式：yyyy-MM-dd）
     * 
     * @param date 日期
     * @return 格式化字符串
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }
    
    /**
     * 格式化日期时间为字符串（默认格式：yyyy-MM-dd HH:mm:ss）
     * 
     * @param dateTime 日期时间
     * @return 格式化字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
    
    /**
     * 格式化日期为指定格式字符串
     * 
     * @param date    日期
     * @param pattern 格式模式
     * @return 格式化字符串
     */
    public static String formatDate(LocalDate date, String pattern) {
        if (date == null || Strings.isBlank(pattern)) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * 格式化日期时间为指定格式字符串
     * 
     * @param dateTime 日期时间
     * @param pattern  格式模式
     * @return 格式化字符串
     */
    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || Strings.isBlank(pattern)) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    // ====================
    // 解析
    // ====================
    
    /**
     * 解析日期字符串（默认格式：yyyy-MM-dd）
     * 
     * @param dateStr 日期字符串
     * @return 日期对象，解析失败返回null
     */
    public static LocalDate parseDate(String dateStr) {
        if (Strings.isBlank(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * 解析日期时间字符串（默认格式：yyyy-MM-dd HH:mm:ss）
     * 
     * @param dateTimeStr 日期时间字符串
     * @return 日期时间对象，解析失败返回null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (Strings.isBlank(dateTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * 解析日期字符串（指定格式）
     * 
     * @param dateStr 日期字符串
     * @param pattern 格式模式
     * @return 日期对象，解析失败返回null
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (Strings.isBlank(dateStr) || Strings.isBlank(pattern)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * 解析日期时间字符串（指定格式）
     * 
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式模式
     * @return 日期时间对象，解析失败返回null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (Strings.isBlank(dateTimeStr) || Strings.isBlank(pattern)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    // ====================
    // 日期计算
    // ====================
    
    /**
     * 增加天数
     * 
     * @param date 原日期
     * @param days 天数（可以为负数）
     * @return 新日期
     */
    public static LocalDate plusDays(LocalDate date, long days) {
        return date != null ? date.plusDays(days) : null;
    }
    
    /**
     * 增加月数
     * 
     * @param date   原日期
     * @param months 月数（可以为负数）
     * @return 新日期
     */
    public static LocalDate plusMonths(LocalDate date, long months) {
        return date != null ? date.plusMonths(months) : null;
    }
    
    /**
     * 增加年数
     * 
     * @param date  原日期
     * @param years 年数（可以为负数）
     * @return 新日期
     */
    public static LocalDate plusYears(LocalDate date, long years) {
        return date != null ? date.plusYears(years) : null;
    }
    
    /**
     * 增加小时数
     * 
     * @param dateTime 原日期时间
     * @param hours    小时数（可以为负数）
     * @return 新日期时间
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        return dateTime != null ? dateTime.plusHours(hours) : null;
    }
    
    /**
     * 增加分钟数
     * 
     * @param dateTime 原日期时间
     * @param minutes  分钟数（可以为负数）
     * @return 新日期时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime != null ? dateTime.plusMinutes(minutes) : null;
    }
    
    /**
     * 增加秒数
     * 
     * @param dateTime 原日期时间
     * @param seconds  秒数（可以为负数）
     * @return 新日期时间
     */
    public static LocalDateTime plusSeconds(LocalDateTime dateTime, long seconds) {
        return dateTime != null ? dateTime.plusSeconds(seconds) : null;
    }
    
    // ====================
    // 日期比较
    // ====================
    
    /**
     * 检查日期是否在指定范围内
     * 
     * @param date      要检查的日期
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    /**
     * 计算两个日期之间的天数差
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 天数差（endDate - startDate）
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * 计算两个日期时间之间的小时数差
     * 
     * @param startDateTime 开始日期时间
     * @param endDateTime   结束日期时间
     * @return 小时数差
     */
    public static long hoursBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(startDateTime, endDateTime);
    }
    
    /**
     * 计算两个日期时间之间的分钟数差
     * 
     * @param startDateTime 开始日期时间
     * @param endDateTime   结束日期时间
     * @return 分钟数差
     */
    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }
    
    // ====================
    // 特殊日期计算
    // ====================
    
    /**
     * 获取月初日期
     * 
     * @param date 原日期
     * @return 月初日期
     */
    public static LocalDate startOfMonth(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.firstDayOfMonth()) : null;
    }
    
    /**
     * 获取月末日期
     * 
     * @param date 原日期
     * @return 月末日期
     */
    public static LocalDate endOfMonth(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.lastDayOfMonth()) : null;
    }
    
    /**
     * 获取年初日期
     * 
     * @param date 原日期
     * @return 年初日期
     */
    public static LocalDate startOfYear(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.firstDayOfYear()) : null;
    }
    
    /**
     * 获取年末日期
     * 
     * @param date 原日期
     * @return 年末日期
     */
    public static LocalDate endOfYear(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.lastDayOfYear()) : null;
    }
    
    /**
     * 获取一天的开始时间（00:00:00）
     * 
     * @param date 日期
     * @return 一天的开始时间
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }
    
    /**
     * 获取一天的结束时间（23:59:59.999999999）
     * 
     * @param date 日期
     * @return 一天的结束时间
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }
    
    // ====================
    // 时区转换
    // ====================
    
    /**
     * 将LocalDateTime转换为时间戳（秒）
     * 
     * @param dateTime 日期时间
     * @param zoneId   时区
     * @return 时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(zoneId != null ? zoneId : DEFAULT_ZONE).toEpochSecond();
    }
    
    /**
     * 将LocalDateTime转换为时间戳（毫秒）
     * 
     * @param dateTime 日期时间
     * @param zoneId   时区
     * @return 时间戳（毫秒）
     */
    public static long toTimestampMillis(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(zoneId != null ? zoneId : DEFAULT_ZONE).toInstant().toEpochMilli();
    }
    
    /**
     * 将时间戳转换为LocalDateTime
     * 
     * @param timestamp 时间戳（秒）
     * @param zoneId    时区
     * @return 日期时间
     */
    public static LocalDateTime fromTimestamp(long timestamp, ZoneId zoneId) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                zoneId != null ? zoneId : DEFAULT_ZONE
        );
    }
    
    /**
     * 将时间戳转换为LocalDateTime
     * 
     * @param timestampMillis 时间戳（毫秒）
     * @param zoneId          时区
     * @return 日期时间
     */
    public static LocalDateTime fromTimestampMillis(long timestampMillis, ZoneId zoneId) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMillis),
                zoneId != null ? zoneId : DEFAULT_ZONE
        );
    }
    
    // ====================
    // Date类型转换（兼容性）
    // ====================
    
    /**
     * Date转LocalDate
     * 
     * @param date Date对象
     * @return LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDate();
    }
    
    /**
     * Date转LocalDateTime
     * 
     * @param date Date对象
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime();
    }
    
    /**
     * LocalDate转Date
     * 
     * @param localDate LocalDate对象
     * @return Date
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(DEFAULT_ZONE).toInstant());
    }
    
    /**
     * LocalDateTime转Date
     * 
     * @param localDateTime LocalDateTime对象
     * @return Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(DEFAULT_ZONE).toInstant());
    }
}
