package io.nebula.core.common.util;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * DateUtils单元测试
 */
class DateUtilsTest {
    
    // ====================
    // 当前时间获取测试
    // ====================
    
    @Test
    void testNow() {
        LocalDate date = DateUtils.now();
        
        assertThat(date).isNotNull();
        assertThat(date).isToday();
    }
    
    @Test
    void testNowDateTime() {
        LocalDateTime dateTime = DateUtils.nowDateTime();
        
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.toLocalDate()).isToday();
    }
    
    @Test
    void testNowTimestamp() {
        long timestamp = DateUtils.nowTimestamp();
        
        assertThat(timestamp).isPositive();
        assertThat(timestamp).isGreaterThan(1640995200L);  // 2022-01-01之后
    }
    
    @Test
    void testNowTimestampMillis() {
        long timestamp = DateUtils.nowTimestampMillis();
        
        assertThat(timestamp).isPositive();
        assertThat(timestamp).isGreaterThan(1640995200000L);  // 2022-01-01之后
    }
    
    // ====================
    // 格式化测试
    // ====================
    
    @Test
    void testFormatDate() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        String formatted = DateUtils.formatDate(date);
        
        assertThat(formatted).isEqualTo("2025-01-15");
    }
    
    @Test
    void testFormatDateNull() {
        String formatted = DateUtils.formatDate(null);
        
        assertThat(formatted).isNull();
    }
    
    @Test
    void testFormatDateWithPattern() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        String formatted = DateUtils.formatDate(date, "yyyy/MM/dd");
        
        assertThat(formatted).isEqualTo("2025/01/15");
    }
    
    @Test
    void testFormatDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 14, 30, 45);
        
        String formatted = DateUtils.formatDateTime(dateTime);
        
        assertThat(formatted).isEqualTo("2025-01-15 14:30:45");
    }
    
    @Test
    void testFormatDateTimeWithPattern() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 14, 30, 45);
        
        String formatted = DateUtils.formatDateTime(dateTime, "yyyyMMddHHmmss");
        
        assertThat(formatted).isEqualTo("20250115143045");
    }
    
    // ====================
    // 解析测试
    // ====================
    
    @Test
    void testParseDate() {
        LocalDate date = DateUtils.parseDate("2025-01-15");
        
        assertThat(date).isNotNull();
        assertThat(date.getYear()).isEqualTo(2025);
        assertThat(date.getMonthValue()).isEqualTo(1);
        assertThat(date.getDayOfMonth()).isEqualTo(15);
    }
    
    @Test
    void testParseDateInvalid() {
        LocalDate date = DateUtils.parseDate("invalid-date");
        
        assertThat(date).isNull();
    }
    
    @Test
    void testParseDateNull() {
        LocalDate date = DateUtils.parseDate(null);
        
        assertThat(date).isNull();
    }
    
    @Test
    void testParseDateWithPattern() {
        LocalDate date = DateUtils.parseDate("2025/01/15", "yyyy/MM/dd");
        
        assertThat(date).isNotNull();
        assertThat(date.getYear()).isEqualTo(2025);
        assertThat(date.getMonthValue()).isEqualTo(1);
        assertThat(date.getDayOfMonth()).isEqualTo(15);
    }
    
    @Test
    void testParseDateTime() {
        LocalDateTime dateTime = DateUtils.parseDateTime("2025-01-15 14:30:45");
        
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2025);
        assertThat(dateTime.getHour()).isEqualTo(14);
        assertThat(dateTime.getMinute()).isEqualTo(30);
        assertThat(dateTime.getSecond()).isEqualTo(45);
    }
    
    @Test
    void testParseDateTimeWithPattern() {
        LocalDateTime dateTime = DateUtils.parseDateTime("20250115143045", "yyyyMMddHHmmss");
        
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2025);
        assertThat(dateTime.getHour()).isEqualTo(14);
    }
    
    // ====================
    // 日期计算测试
    // ====================
    
    @Test
    void testPlusDays() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDate newDate = DateUtils.plusDays(date, 10);
        
        assertThat(newDate).isEqualTo(LocalDate.of(2025, 1, 25));
    }
    
    @Test
    void testPlusDaysNegative() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDate newDate = DateUtils.plusDays(date, -5);
        
        assertThat(newDate).isEqualTo(LocalDate.of(2025, 1, 10));
    }
    
    @Test
    void testPlusMonths() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDate newDate = DateUtils.plusMonths(date, 3);
        
        assertThat(newDate).isEqualTo(LocalDate.of(2025, 4, 15));
    }
    
    @Test
    void testPlusYears() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDate newDate = DateUtils.plusYears(date, 2);
        
        assertThat(newDate).isEqualTo(LocalDate.of(2027, 1, 15));
    }
    
    @Test
    void testPlusHours() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 14, 30);
        
        LocalDateTime newDateTime = DateUtils.plusHours(dateTime, 5);
        
        assertThat(newDateTime).isEqualTo(LocalDateTime.of(2025, 1, 15, 19, 30));
    }
    
    @Test
    void testPlusMinutes() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 14, 30);
        
        LocalDateTime newDateTime = DateUtils.plusMinutes(dateTime, 45);
        
        assertThat(newDateTime).isEqualTo(LocalDateTime.of(2025, 1, 15, 15, 15));
    }
    
    @Test
    void testPlusSeconds() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 14, 30, 30);
        
        LocalDateTime newDateTime = DateUtils.plusSeconds(dateTime, 45);
        
        assertThat(newDateTime).isEqualTo(LocalDateTime.of(2025, 1, 15, 14, 31, 15));
    }
    
    // ====================
    // 日期比较测试
    // ====================
    
    @Test
    void testIsBetween() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        
        boolean isBetween = DateUtils.isBetween(date, startDate, endDate);
        
        assertThat(isBetween).isTrue();
    }
    
    @Test
    void testIsBetweenNotBetween() {
        LocalDate date = LocalDate.of(2025, 2, 15);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        
        boolean isBetween = DateUtils.isBetween(date, startDate, endDate);
        
        assertThat(isBetween).isFalse();
    }
    
    @Test
    void testIsBetweenEdge() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        
        boolean isBetween = DateUtils.isBetween(date, startDate, endDate);
        
        assertThat(isBetween).isTrue();  // 包含边界
    }
    
    @Test
    void testDaysBetween() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 11);
        
        long days = DateUtils.daysBetween(startDate, endDate);
        
        assertThat(days).isEqualTo(10);
    }
    
    @Test
    void testDaysBetweenNegative() {
        LocalDate startDate = LocalDate.of(2025, 1, 11);
        LocalDate endDate = LocalDate.of(2025, 1, 1);
        
        long days = DateUtils.daysBetween(startDate, endDate);
        
        assertThat(days).isEqualTo(-10);
    }
    
    @Test
    void testHoursBetween() {
        LocalDateTime startDateTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2025, 1, 1, 15, 0);
        
        long hours = DateUtils.hoursBetween(startDateTime, endDateTime);
        
        assertThat(hours).isEqualTo(5);
    }
    
    @Test
    void testMinutesBetween() {
        LocalDateTime startDateTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2025, 1, 1, 10, 30);
        
        long minutes = DateUtils.minutesBetween(startDateTime, endDateTime);
        
        assertThat(minutes).isEqualTo(30);
    }
    
    // ====================
    // 特殊日期计算测试
    // ====================
    
    @Test
    void testStartOfMonth() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDate startOfMonth = DateUtils.startOfMonth(date);
        
        assertThat(startOfMonth).isEqualTo(LocalDate.of(2025, 1, 1));
    }
    
    @Test
    void testEndOfMonth() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDate endOfMonth = DateUtils.endOfMonth(date);
        
        assertThat(endOfMonth).isEqualTo(LocalDate.of(2025, 1, 31));
    }
    
    @Test
    void testStartOfYear() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        
        LocalDate startOfYear = DateUtils.startOfYear(date);
        
        assertThat(startOfYear).isEqualTo(LocalDate.of(2025, 1, 1));
    }
    
    @Test
    void testEndOfYear() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        
        LocalDate endOfYear = DateUtils.endOfYear(date);
        
        assertThat(endOfYear).isEqualTo(LocalDate.of(2025, 12, 31));
    }
    
    @Test
    void testStartOfDay() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDateTime startOfDay = DateUtils.startOfDay(date);
        
        assertThat(startOfDay).isEqualTo(LocalDateTime.of(2025, 1, 15, 0, 0, 0));
    }
    
    @Test
    void testEndOfDay() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        
        LocalDateTime endOfDay = DateUtils.endOfDay(date);
        
        assertThat(endOfDay.getHour()).isEqualTo(23);
        assertThat(endOfDay.getMinute()).isEqualTo(59);
        assertThat(endOfDay.getSecond()).isEqualTo(59);
    }
    
    // ====================
    // 时区转换测试
    // ====================
    
    @Test
    void testToTimestamp() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        
        long timestamp = DateUtils.toTimestamp(dateTime, DateUtils.UTC_ZONE);
        
        assertThat(timestamp).isPositive();
    }
    
    @Test
    void testToTimestampMillis() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        
        long timestamp = DateUtils.toTimestampMillis(dateTime, DateUtils.UTC_ZONE);
        
        assertThat(timestamp).isPositive();
        assertThat(timestamp).isGreaterThan(1640995200000L);
    }
    
    @Test
    void testFromTimestamp() {
        long timestamp = 1735689600L;  // 2025-01-01 00:00:00 UTC
        
        LocalDateTime dateTime = DateUtils.fromTimestamp(timestamp, DateUtils.UTC_ZONE);
        
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2025);
        assertThat(dateTime.getMonthValue()).isEqualTo(1);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(1);
    }
    
    @Test
    void testFromTimestampMillis() {
        long timestamp = 1735689600000L;  // 2025-01-01 00:00:00 UTC
        
        LocalDateTime dateTime = DateUtils.fromTimestampMillis(timestamp, DateUtils.UTC_ZONE);
        
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2025);
    }
    
    @Test
    void testTimestampRoundTrip() {
        LocalDateTime original = LocalDateTime.of(2025, 1, 15, 14, 30, 45);
        
        long timestamp = DateUtils.toTimestamp(original, DateUtils.DEFAULT_ZONE);
        LocalDateTime restored = DateUtils.fromTimestamp(timestamp, DateUtils.DEFAULT_ZONE);
        
        // 注意：秒级时间戳会丢失毫秒和纳秒
        assertThat(restored.getYear()).isEqualTo(original.getYear());
        assertThat(restored.getMonthValue()).isEqualTo(original.getMonthValue());
        assertThat(restored.getDayOfMonth()).isEqualTo(original.getDayOfMonth());
        assertThat(restored.getHour()).isEqualTo(original.getHour());
        assertThat(restored.getMinute()).isEqualTo(original.getMinute());
        assertThat(restored.getSecond()).isEqualTo(original.getSecond());
    }
    
    // ====================
    // Date类型转换测试
    // ====================
    
    @Test
    void testToLocalDate() {
        Date date = new Date();
        
        LocalDate localDate = DateUtils.toLocalDate(date);
        
        assertThat(localDate).isNotNull();
        assertThat(localDate).isToday();
    }
    
    @Test
    void testToLocalDateTime() {
        Date date = new Date();
        
        LocalDateTime localDateTime = DateUtils.toLocalDateTime(date);
        
        assertThat(localDateTime).isNotNull();
        assertThat(localDateTime.toLocalDate()).isToday();
    }
    
    @Test
    void testToDate() {
        LocalDate localDate = LocalDate.of(2025, 1, 15);
        
        Date date = DateUtils.toDate(localDate);
        
        assertThat(date).isNotNull();
    }
    
    @Test
    void testToDateFromLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.of(2025, 1, 15, 14, 30);
        
        Date date = DateUtils.toDate(localDateTime);
        
        assertThat(date).isNotNull();
    }
    
    @Test
    void testDateConversionRoundTrip() {
        LocalDate original = LocalDate.of(2025, 1, 15);
        
        Date date = DateUtils.toDate(original);
        LocalDate restored = DateUtils.toLocalDate(date);
        
        assertThat(restored).isEqualTo(original);
    }
}

