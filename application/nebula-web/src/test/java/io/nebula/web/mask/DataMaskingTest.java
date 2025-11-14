package io.nebula.web.mask;

import io.nebula.web.mask.strategies.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据脱敏测试
 */
class DataMaskingTest {
    
    @Test
    void testMaskPhone() {
        PhoneMaskingStrategy strategy = new PhoneMaskingStrategy();
        
        // 测试中国大陆手机号
        String phone = "13812345678";
        String masked = strategy.mask(phone);
        assertThat(masked).isEqualTo("138****5678");
        
        // 测试其他格式
        phone = "18666667777";
        masked = strategy.mask(phone);
        assertThat(masked).isEqualTo("186****7777");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
        assertThat(strategy.mask("")).isEmpty();
    }
    
    @Test
    void testMaskEmail() {
        EmailMaskingStrategy strategy = new EmailMaskingStrategy();
        
        // 测试邮箱脱敏
        String email = "test@example.com";
        String masked = strategy.mask(email);
        assertThat(masked).matches(".*\\*+.*@example\\.com");
        
        // 测试长邮箱
        email = "verylongemail@example.com";
        masked = strategy.mask(email);
        assertThat(masked).matches(".*\\*+.*@example\\.com");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
        assertThat(strategy.mask("")).isEmpty();
    }
    
    @Test
    void testMaskIdCard() {
        IdCardMaskingStrategy strategy = new IdCardMaskingStrategy();
        
        // 测试身份证脱敏（前3后4）
        String idCard = "110101199001011234";
        String masked = strategy.mask(idCard);
        assertThat(masked).startsWith("110");
        assertThat(masked).endsWith("1234");
        assertThat(masked).contains("*");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
        assertThat(strategy.mask("")).isEmpty();
    }
    
    @Test
    void testMaskPassword() {
        PasswordMaskingStrategy strategy = new PasswordMaskingStrategy();
        
        // 测试密码完全脱敏
        String password = "mySecretPassword123";
        String masked = strategy.mask(password);
        assertThat(masked).matches("\\*+");
        assertThat(masked).doesNotContain("mySecret");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
        assertThat(strategy.mask("")).isEmpty();
    }
    
    @Test
    void testMaskName() {
        NameMaskingStrategy strategy = new NameMaskingStrategy();
        
        // 测试姓名脱敏
        String name = "张三";
        String masked = strategy.mask(name);
        assertThat(masked).matches("张\\*");
        
        // 测试长姓名
        name = "欧阳娜娜";
        masked = strategy.mask(name);
        assertThat(masked).contains("*");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
    }
    
    @Test
    void testMaskBankCard() {
        BankCardMaskingStrategy strategy = new BankCardMaskingStrategy();
        
        // 测试银行卡脱敏（前4后4）
        String bankCard = "6222020012345678901";
        String masked = strategy.mask(bankCard);
        assertThat(masked).startsWith("6222");
        assertThat(masked).endsWith("8901");
        assertThat(masked).contains("*");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
        assertThat(strategy.mask("")).isEmpty();
    }
    
    @Test
    void testMaskAddress() {
        AddressMaskingStrategy strategy = new AddressMaskingStrategy();
        
        // 测试地址脱敏
        String address = "北京市朝阳区某某街道123号";
        String masked = strategy.mask(address);
        assertThat(masked).startsWith("北京市朝");
        assertThat(masked).contains("*");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
    }
    
    @Test
    void testMaskIpAddress() {
        IpAddressMaskingStrategy strategy = new IpAddressMaskingStrategy();
        
        // 测试IP地址脱敏
        String ip = "192.168.1.100";
        String masked = strategy.mask(ip);
        assertThat(masked).matches("192\\.168\\.\\*\\.\\*");
        
        // 测试空值
        assertThat(strategy.mask(null)).isNull();
    }
    
    @Test
    void testMaskShortString() {
        PhoneMaskingStrategy strategy = new PhoneMaskingStrategy();
        
        // 测试短字符串
        String shortStr = "abc";
        String masked = strategy.mask(shortStr);
        assertThat(masked).isNotEmpty();
    }
    
    @Test
    void testMaskSpecialCharacters() {
        EmailMaskingStrategy strategy = new EmailMaskingStrategy();
        
        // 测试包含特殊字符的邮箱
        String email = "test+tag@example.com";
        String masked = strategy.mask(email);
        assertThat(masked).contains("@example.com");
        assertThat(masked).contains("*");
    }
}

