package io.nebula.core.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * CryptoUtils单元测试
 */
class CryptoUtilsTest {
    
    // ====================
    // 哈希测试
    // ====================
    
    @Test
    void testMd5() {
        String data = "test data";
        String hash = CryptoUtils.md5(data);
        
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(32);  // MD5哈希长度
        assertThat(hash).matches("^[a-f0-9]{32}$");
    }
    
    @Test
    void testMd5Null() {
        String hash = CryptoUtils.md5((String) null);
        
        assertThat(hash).isNull();
    }
    
    @Test
    void testMd5Consistency() {
        String data = "test data";
        String hash1 = CryptoUtils.md5(data);
        String hash2 = CryptoUtils.md5(data);
        
        // 相同输入应产生相同哈希
        assertThat(hash1).isEqualTo(hash2);
    }
    
    @Test
    void testSha1() {
        String data = "test data";
        String hash = CryptoUtils.sha1(data);
        
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(40);  // SHA-1哈希长度
        assertThat(hash).matches("^[a-f0-9]{40}$");
    }
    
    @Test
    void testSha256() {
        String data = "test data";
        String hash = CryptoUtils.sha256(data);
        
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);  // SHA-256哈希长度
        assertThat(hash).matches("^[a-f0-9]{64}$");
    }
    
    @Test
    void testSha256WithSalt() {
        String data = "password";
        String salt = "randomsalt";
        
        String hash = CryptoUtils.sha256WithSalt(data, salt);
        
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
    }
    
    @Test
    void testSha256WithSaltDifferentSaltsDifferentHashes() {
        String data = "password";
        String salt1 = "salt1";
        String salt2 = "salt2";
        
        String hash1 = CryptoUtils.sha256WithSalt(data, salt1);
        String hash2 = CryptoUtils.sha256WithSalt(data, salt2);
        
        // 不同的盐应产生不同的哈希
        assertThat(hash1).isNotEqualTo(hash2);
    }
    
    // ====================
    // 密码加密测试
    // ====================
    
    @Test
    void testEncrypt() {
        String password = "MyPassword123";
        String encrypted = CryptoUtils.encrypt(password);
        
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).contains(":");  // 格式：salt:hash
        
        String[] parts = encrypted.split(":");
        assertThat(parts).hasSize(2);
        assertThat(parts[0]).isNotEmpty();  // salt
        assertThat(parts[1]).hasSize(64);   // SHA-256哈希
    }
    
    @Test
    void testMatches() {
        String password = "MyPassword123";
        String encrypted = CryptoUtils.encrypt(password);
        
        boolean matches = CryptoUtils.matches(password, encrypted);
        
        assertThat(matches).isTrue();
    }
    
    @Test
    void testMatchesWrongPassword() {
        String password = "MyPassword123";
        String wrongPassword = "WrongPassword456";
        String encrypted = CryptoUtils.encrypt(password);
        
        boolean matches = CryptoUtils.matches(wrongPassword, encrypted);
        
        assertThat(matches).isFalse();
    }
    
    @Test
    void testMatchesNullPassword() {
        String encrypted = CryptoUtils.encrypt("password");
        
        boolean matches = CryptoUtils.matches(null, encrypted);
        
        assertThat(matches).isFalse();
    }
    
    @Test
    void testMatchesInvalidFormat() {
        boolean matches = CryptoUtils.matches("password", "invalid-format");
        
        assertThat(matches).isFalse();
    }
    
    // ====================
    // 密码强度测试
    // ====================
    
    @Test
    void testIsStrongPassword() {
        String strongPassword = "MyP@ssw0rd!";
        
        boolean isStrong = CryptoUtils.isStrongPassword(strongPassword);
        
        assertThat(isStrong).isTrue();
    }
    
    @Test
    void testIsStrongPasswordTooShort() {
        String shortPassword = "Abc1!";
        
        boolean isStrong = CryptoUtils.isStrongPassword(shortPassword);
        
        assertThat(isStrong).isFalse();
    }
    
    @Test
    void testIsStrongPasswordNoUppercase() {
        String password = "mypassword1!";
        
        boolean isStrong = CryptoUtils.isStrongPassword(password);
        
        assertThat(isStrong).isFalse();
    }
    
    @Test
    void testIsStrongPasswordNoLowercase() {
        String password = "MYPASSWORD1!";
        
        boolean isStrong = CryptoUtils.isStrongPassword(password);
        
        assertThat(isStrong).isFalse();
    }
    
    @Test
    void testIsStrongPasswordNoDigit() {
        String password = "MyPassword!";
        
        boolean isStrong = CryptoUtils.isStrongPassword(password);
        
        assertThat(isStrong).isFalse();
    }
    
    @Test
    void testIsStrongPasswordNoSpecial() {
        String password = "MyPassword123";
        
        boolean isStrong = CryptoUtils.isStrongPassword(password);
        
        assertThat(isStrong).isFalse();
    }
    
    // ====================
    // AES加密测试
    // ====================
    
    @Test
    void testGenerateAESKey() {
        String key = CryptoUtils.generateAESKey();
        
        assertThat(key).isNotNull();
        assertThat(key).isNotEmpty();
        assertThat(key).isBase64();
    }
    
    @Test
    void testAesEncrypt() {
        String plainText = "This is a secret message";
        String key = CryptoUtils.generateAESKey();
        
        String cipherText = CryptoUtils.aesEncrypt(plainText, key);
        
        assertThat(cipherText).isNotNull();
        assertThat(cipherText).isNotEqualTo(plainText);
        assertThat(cipherText).isBase64();
    }
    
    @Test
    void testAesDecrypt() {
        String plainText = "This is a secret message";
        String key = CryptoUtils.generateAESKey();
        
        String cipherText = CryptoUtils.aesEncrypt(plainText, key);
        String decryptedText = CryptoUtils.aesDecrypt(cipherText, key);
        
        assertThat(decryptedText).isEqualTo(plainText);
    }
    
    @Test
    void testAesEncryptDecryptWithChineseCharacters() {
        String plainText = "这是一段中文测试数据";
        String key = CryptoUtils.generateAESKey();
        
        String cipherText = CryptoUtils.aesEncrypt(plainText, key);
        String decryptedText = CryptoUtils.aesDecrypt(cipherText, key);
        
        assertThat(decryptedText).isEqualTo(plainText);
    }
    
    @Test
    void testAesEncryptNull() {
        String key = CryptoUtils.generateAESKey();
        String encrypted = CryptoUtils.aesEncrypt(null, key);
        
        assertThat(encrypted).isNull();
    }
    
    // ====================
    // Base64编码测试
    // ====================
    
    @Test
    void testBase64Encode() {
        String data = "test data";
        String encoded = CryptoUtils.base64Encode(data);
        
        assertThat(encoded).isNotNull();
        assertThat(encoded).isBase64();
    }
    
    @Test
    void testBase64Decode() {
        String data = "test data";
        String encoded = CryptoUtils.base64Encode(data);
        String decoded = CryptoUtils.base64Decode(encoded);
        
        assertThat(decoded).isEqualTo(data);
    }
    
    @Test
    void testBase64EncodeDecodeWithSpecialCharacters() {
        String data = "Hello!@#$%^&*()_+{}|:<>?";
        String encoded = CryptoUtils.base64Encode(data);
        String decoded = CryptoUtils.base64Decode(encoded);
        
        assertThat(decoded).isEqualTo(data);
    }
    
    @Test
    void testBase64UrlEncode() {
        String data = "test data with special chars !@#$%";
        String encoded = CryptoUtils.base64UrlEncode(data);
        
        assertThat(encoded).isNotNull();
        assertThat(encoded).doesNotContain("+", "/", "=");  // URL安全的Base64不包含这些字符
    }
    
    @Test
    void testBase64UrlDecode() {
        String data = "test data";
        String encoded = CryptoUtils.base64UrlEncode(data);
        String decoded = CryptoUtils.base64UrlDecode(encoded);
        
        assertThat(decoded).isEqualTo(data);
    }
    
    // ====================
    // 随机字符串和盐值测试
    // ====================
    
    @Test
    void testGenerateRandomString() {
        String random = CryptoUtils.generateRandomString(16);
        
        assertThat(random).isNotNull();
        assertThat(random.length()).isLessThanOrEqualTo(16);
    }
    
    @Test
    void testGenerateRandomStringUniqueness() {
        String random1 = CryptoUtils.generateRandomString(16);
        String random2 = CryptoUtils.generateRandomString(16);
        
        // 两次生成的随机字符串应该不同
        assertThat(random1).isNotEqualTo(random2);
    }
    
    @Test
    void testGenerateSalt() {
        String salt = CryptoUtils.generateSalt(16);
        
        assertThat(salt).isNotNull();
        assertThat(salt).isBase64();
    }
    
    @Test
    void testGenerateSaltUniqueness() {
        String salt1 = CryptoUtils.generateSalt(16);
        String salt2 = CryptoUtils.generateSalt(16);
        
        // 两次生成的盐值应该不同
        assertThat(salt1).isNotEqualTo(salt2);
    }
    
    // ====================
    // 安全比较测试
    // ====================
    
    @Test
    void testSecureEquals() {
        String str1 = "test string";
        String str2 = "test string";
        
        boolean equals = CryptoUtils.secureEquals(str1, str2);
        
        assertThat(equals).isTrue();
    }
    
    @Test
    void testSecureEqualsNotEqual() {
        String str1 = "test string 1";
        String str2 = "test string 2";
        
        boolean equals = CryptoUtils.secureEquals(str1, str2);
        
        assertThat(equals).isFalse();
    }
    
    @Test
    void testSecureEqualsBothNull() {
        boolean equals = CryptoUtils.secureEquals(null, null);
        
        assertThat(equals).isTrue();
    }
    
    @Test
    void testSecureEqualsOneNull() {
        boolean equals = CryptoUtils.secureEquals("test", null);
        
        assertThat(equals).isFalse();
    }
}

