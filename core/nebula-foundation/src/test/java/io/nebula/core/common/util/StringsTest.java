package io.nebula.core.common.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.List;

/**
 * Strings 工具类单元测试
 */
class StringsTest {
    
    @Test
    void testIsEmpty() {
        // Test null
        assertThat(Strings.isEmpty(null)).isTrue();
        
        // Test empty string
        assertThat(Strings.isEmpty("")).isTrue();
        
        // Test non-empty string
        assertThat(Strings.isEmpty("hello")).isFalse();
        
        // Test whitespace
        assertThat(Strings.isEmpty(" ")).isFalse();
        assertThat(Strings.isEmpty("\t")).isFalse();
        assertThat(Strings.isEmpty("\n")).isFalse();
    }
    
    @Test
    void testIsNotEmpty() {
        // Test null
        assertThat(Strings.isNotEmpty(null)).isFalse();
        
        // Test empty string
        assertThat(Strings.isNotEmpty("")).isFalse();
        
        // Test non-empty string
        assertThat(Strings.isNotEmpty("hello")).isTrue();
        
        // Test whitespace
        assertThat(Strings.isNotEmpty(" ")).isTrue();
        assertThat(Strings.isNotEmpty("\t")).isTrue();
    }
    
    @Test
    void testIsBlank() {
        // Test null
        assertThat(Strings.isBlank(null)).isTrue();
        
        // Test empty string
        assertThat(Strings.isBlank("")).isTrue();
        
        // Test whitespace only
        assertThat(Strings.isBlank(" ")).isTrue();
        assertThat(Strings.isBlank("  ")).isTrue();
        assertThat(Strings.isBlank("\t")).isTrue();
        assertThat(Strings.isBlank("\n")).isTrue();
        assertThat(Strings.isBlank(" \t\n ")).isTrue();
        
        // Test non-blank
        assertThat(Strings.isBlank("hello")).isFalse();
        assertThat(Strings.isBlank(" hello ")).isFalse();
        assertThat(Strings.isBlank("a")).isFalse();
    }
    
    @Test
    void testIsNotBlank() {
        // Test null and empty/blank
        assertThat(Strings.isNotBlank(null)).isFalse();
        assertThat(Strings.isNotBlank("")).isFalse();
        assertThat(Strings.isNotBlank(" ")).isFalse();
        assertThat(Strings.isNotBlank("\t\n")).isFalse();
        
        // Test non-blank
        assertThat(Strings.isNotBlank("hello")).isTrue();
        assertThat(Strings.isNotBlank(" hello ")).isTrue();
        assertThat(Strings.isNotBlank("a")).isTrue();
    }
    
    @Test
    void testDefaultIfEmpty() {
        // Test null
        assertThat(Strings.defaultIfEmpty(null, "default")).isEqualTo("default");
        
        // Test empty string
        assertThat(Strings.defaultIfEmpty("", "default")).isEqualTo("default");
        
        // Test non-empty string
        assertThat(Strings.defaultIfEmpty("hello", "default")).isEqualTo("hello");
        
        // Test whitespace (should not use default)
        assertThat(Strings.defaultIfEmpty(" ", "default")).isEqualTo(" ");
    }
    
    @Test
    void testDefaultIfBlank() {
        // Test null
        assertThat(Strings.defaultIfBlank(null, "default")).isEqualTo("default");
        
        // Test empty string
        assertThat(Strings.defaultIfBlank("", "default")).isEqualTo("default");
        
        // Test blank string
        assertThat(Strings.defaultIfBlank(" ", "default")).isEqualTo("default");
        assertThat(Strings.defaultIfBlank("\t\n", "default")).isEqualTo("default");
        
        // Test non-blank string
        assertThat(Strings.defaultIfBlank("hello", "default")).isEqualTo("hello");
        assertThat(Strings.defaultIfBlank(" hello ", "default")).isEqualTo(" hello ");
    }
    
    @Test
    void testTrim() {
        // Test null
        assertThat(Strings.trim(null)).isNull();
        
        // Test empty string
        assertThat(Strings.trim("")).isEqualTo("");
        
        // Test string with whitespace
        assertThat(Strings.trim(" hello ")).isEqualTo("hello");
        assertThat(Strings.trim("\t\nhello\t\n")).isEqualTo("hello");
        
        // Test string without whitespace
        assertThat(Strings.trim("hello")).isEqualTo("hello");
        
        // Test whitespace only
        assertThat(Strings.trim("   ")).isEqualTo("");
    }
    
    @Test
    void testCapitalize() {
        // Test null
        assertThat(Strings.capitalize(null)).isNull();
        
        // Test empty string
        assertThat(Strings.capitalize("")).isEqualTo("");
        
        // Test single character
        assertThat(Strings.capitalize("a")).isEqualTo("A");
        assertThat(Strings.capitalize("A")).isEqualTo("A");
        
        // Test normal string
        assertThat(Strings.capitalize("hello")).isEqualTo("Hello");
        assertThat(Strings.capitalize("Hello")).isEqualTo("Hello");
        assertThat(Strings.capitalize("HELLO")).isEqualTo("HELLO");
        
        // Test with numbers and special characters
        assertThat(Strings.capitalize("123abc")).isEqualTo("123abc");
        assertThat(Strings.capitalize("!hello")).isEqualTo("!hello");
    }
    
    @Test
    void testUncapitalize() {
        // Test null
        assertThat(Strings.uncapitalize(null)).isNull();
        
        // Test empty string
        assertThat(Strings.uncapitalize("")).isEqualTo("");
        
        // Test single character
        assertThat(Strings.uncapitalize("A")).isEqualTo("a");
        assertThat(Strings.uncapitalize("a")).isEqualTo("a");
        
        // Test normal string
        assertThat(Strings.uncapitalize("Hello")).isEqualTo("hello");
        assertThat(Strings.uncapitalize("hello")).isEqualTo("hello");
        assertThat(Strings.uncapitalize("HELLO")).isEqualTo("hELLO");
        
        // Test with numbers and special characters
        assertThat(Strings.uncapitalize("123ABC")).isEqualTo("123ABC");
        assertThat(Strings.uncapitalize("!Hello")).isEqualTo("!Hello");
    }
    
    @Test
    void testJoin() {
        // Test null collection
        assertThat(Strings.join(",", (Collection<String>) null)).isEqualTo("");
        
        // Test empty collection
        assertThat(Strings.join(",", List.of())).isEqualTo("");
        
        // Test single element
        assertThat(Strings.join(",", List.of("hello"))).isEqualTo("hello");
        
        // Test multiple elements
        assertThat(Strings.join(",", List.of("a", "b", "c"))).isEqualTo("a,b,c");
        assertThat(Strings.join(" ", List.of("hello", "world"))).isEqualTo("hello world");
        
        // Test with null delimiter - Note: This would throw NPE in StringJoiner, let's test valid case
        assertThat(Strings.join("", List.of("a", "b"))).isEqualTo("ab");
        
        // Test with null elements - create list manually since List.of doesn't allow nulls
        List<String> listWithNulls = new java.util.ArrayList<>();
        listWithNulls.add("a");
        listWithNulls.add(null);
        listWithNulls.add("c");
        assertThat(Strings.join(",", listWithNulls)).isEqualTo("a,c"); // nulls are skipped
    }
    
    @Test
    void testSplit() {
        // Test null string
        assertThat(Strings.split(null, ",")).isEmpty();
        
        // Test empty string
        assertThat(Strings.split("", ",")).isEmpty();
        
        // Test single value
        assertThat(Strings.split("hello", ",")).containsExactly("hello");
        
        // Test multiple values
        assertThat(Strings.split("a,b,c", ",")).containsExactly("a", "b", "c");
        assertThat(Strings.split("hello world", " ")).containsExactly("hello", "world");
        
        // Test with empty parts
        assertThat(Strings.split("a,,c", ",")).containsExactly("a", "", "c");
        
        // Test with delimiter at start/end
        assertThat(Strings.split(",a,b,", ",")).containsExactly("", "a", "b", "");
        
        // Test delimiter not found
        assertThat(Strings.split("hello", ",")).containsExactly("hello");
    }
    
    @Test
    void testSafeEquals() {
        // Test both null
        assertThat(Strings.safeEquals(null, null)).isTrue();
        
        // Test one null
        assertThat(Strings.safeEquals(null, "hello")).isFalse();
        assertThat(Strings.safeEquals("hello", null)).isFalse();
        
        // Test same values
        assertThat(Strings.safeEquals("hello", "hello")).isTrue();
        assertThat(Strings.safeEquals("", "")).isTrue();
        
        // Test different values
        assertThat(Strings.safeEquals("hello", "world")).isFalse();
        assertThat(Strings.safeEquals("Hello", "hello")).isFalse();
        
        // Test same instance
        String str = "hello";
        assertThat(Strings.safeEquals(str, str)).isTrue();
    }
    
    @Test
    void testContainsIgnoreCase() {
        // Test null strings
        assertThat(Strings.containsIgnoreCase(null, "hello")).isFalse();
        assertThat(Strings.containsIgnoreCase("hello", null)).isFalse();
        assertThat(Strings.containsIgnoreCase(null, null)).isFalse();
        
        // Test empty strings
        assertThat(Strings.containsIgnoreCase("", "")).isTrue();
        assertThat(Strings.containsIgnoreCase("hello", "")).isTrue();
        assertThat(Strings.containsIgnoreCase("", "hello")).isFalse();
        
        // Test case insensitive contains
        assertThat(Strings.containsIgnoreCase("Hello World", "hello")).isTrue();
        assertThat(Strings.containsIgnoreCase("Hello World", "WORLD")).isTrue();
        assertThat(Strings.containsIgnoreCase("Hello World", "lo wo")).isTrue();
        
        // Test not contains
        assertThat(Strings.containsIgnoreCase("Hello World", "xyz")).isFalse();
        
        // Test exact match
        assertThat(Strings.containsIgnoreCase("hello", "hello")).isTrue();
        assertThat(Strings.containsIgnoreCase("hello", "HELLO")).isTrue();
    }
}
