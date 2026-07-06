package io.nebula.core.common.security;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 加密解密工具类
 * 提供常用的加密解密功能
 */
public final class CryptoUtils {
    
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String MD5_ALGORITHM = "MD5";
    private static final String SHA1_ALGORITHM = "SHA-1";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final int AES_KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    
    /**
     * 私有构造函数，防止实例化
     */
    private CryptoUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * MD5哈希
     * 
     * @param data 原始数据
     * @return MD5哈希值（十六进制字符串）
     */
    public static String md5(String data) {
        if (data == null) {
            return null;
        }
        return DigestUtils.md5Hex(data);
    }
    
    /**
     * MD5哈希
     * 
     * @param data 原始数据
     * @return MD5哈希值（十六进制字符串）
     */
    public static String md5(byte[] data) {
        if (data == null) {
            return null;
        }
        return DigestUtils.md5Hex(data);
    }
    
    /**
     * SHA1哈希
     * 
     * @param data 原始数据
     * @return SHA1哈希值（十六进制字符串）
     */
    public static String sha1(String data) {
        if (data == null) {
            return null;
        }
        return DigestUtils.sha1Hex(data);
    }
    
    /**
     * SHA1哈希
     * 
     * @param data 原始数据
     * @return SHA1哈希值（十六进制字符串）
     */
    public static String sha1(byte[] data) {
        if (data == null) {
            return null;
        }
        return DigestUtils.sha1Hex(data);
    }
    
    /**
     * SHA256哈希
     * 
     * @param data 原始数据
     * @return SHA256哈希值（十六进制字符串）
     */
    public static String sha256(String data) {
        if (data == null) {
            return null;
        }
        return DigestUtils.sha256Hex(data);
    }
    
    /**
     * SHA256哈希
     * 
     * @param data 原始数据
     * @return SHA256哈希值（十六进制字符串）
     */
    public static String sha256(byte[] data) {
        if (data == null) {
            return null;
        }
        return DigestUtils.sha256Hex(data);
    }
    
    /**
     * 带盐的哈希
     * 
     * @param data 原始数据
     * @param salt 盐值
     * @return 哈希值（十六进制字符串）
     */
    public static String sha256WithSalt(String data, String salt) {
        if (data == null || salt == null) {
            return null;
        }
        return sha256(data + salt);
    }
    
    /**
     * 生成随机盐值
     * 
     * @param length 长度
     * @return 随机盐值
     */
    public static String generateSalt(int length) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }
    
    /**
     * 生成AES密钥
     * 
     * @return Base64编码的AES密钥
     */
    public static String generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.encodeBase64String(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }
    
    /**
     * AES加密
     * 
     * @param plainText 明文
     * @param key       Base64编码的密钥
     * @return Base64编码的密文
     */
    public static String aesEncrypt(String plainText, String key) {
        if (plainText == null || key == null) {
            return null;
        }
        
        try {
            byte[] keyBytes = Base64.decodeBase64(key);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            // 生成随机IV
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // 将IV和密文拼接
            byte[] result = new byte[IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, result, IV_LENGTH, encryptedBytes.length);
            
            return Base64.encodeBase64String(result);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }
    
    /**
     * AES解密
     * 
     * @param cipherText Base64编码的密文
     * @param key        Base64编码的密钥
     * @return 明文
     */
    public static String aesDecrypt(String cipherText, String key) {
        if (cipherText == null || key == null) {
            return null;
        }
        
        try {
            byte[] keyBytes = Base64.decodeBase64(key);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            byte[] encryptedData = Base64.decodeBase64(cipherText);
            
            // 提取IV和密文
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_LENGTH);
            byte[] encryptedBytes = Arrays.copyOfRange(encryptedData, IV_LENGTH, encryptedData.length);
            
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }
    
    /**
     * Base64编码
     * 
     * @param data 原始数据
     * @return Base64编码后的字符串
     */
    public static String base64Encode(String data) {
        if (data == null) {
            return null;
        }
        return Base64.encodeBase64String(data.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Base64编码
     * 
     * @param data 原始数据
     * @return Base64编码后的字符串
     */
    public static String base64Encode(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.encodeBase64String(data);
    }
    
    /**
     * Base64解码
     * 
     * @param encodedData Base64编码的数据
     * @return 解码后的字符串
     */
    public static String base64Decode(String encodedData) {
        if (encodedData == null) {
            return null;
        }
        byte[] decodedBytes = Base64.decodeBase64(encodedData);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Base64解码为字节数组
     * 
     * @param encodedData Base64编码的数据
     * @return 解码后的字节数组
     */
    public static byte[] base64DecodeToBytes(String encodedData) {
        if (encodedData == null) {
            return null;
        }
        return Base64.decodeBase64(encodedData);
    }
    
    /**
     * URL安全的Base64编码
     * 
     * @param data 原始数据
     * @return URL安全的Base64编码字符串
     */
    public static String base64UrlEncode(String data) {
        if (data == null) {
            return null;
        }
        return Base64.encodeBase64URLSafeString(data.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * URL安全的Base64解码
     * 
     * @param encodedData URL安全的Base64编码数据
     * @return 解码后的字符串
     */
    public static String base64UrlDecode(String encodedData) {
        if (encodedData == null) {
            return null;
        }
        byte[] decodedBytes = Base64.decodeBase64(encodedData);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 生成随机字符串
     * 
     * @param length 长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.encodeBase64URLSafeString(bytes).substring(
                0, Math.min(length, Base64.encodeBase64URLSafeString(bytes).length()));
    }
    
    /**
     * 验证密码强度
     * 
     * @param password 密码
     * @return 是否为强密码
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * 安全比较两个字符串（防止时序攻击）
     * 
     * @param a 字符串a
     * @param b 字符串b
     * @return 是否相等
     */
    public static boolean secureEquals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        return MessageDigest.isEqual(aBytes, bBytes);
    }
    
    /**
     * 密码加密（使用SHA256加盐）
     *
     * @param password 原始密码
     * @return 加密后的密码
     * @deprecated 单轮 SHA-256 加盐对抗 GPU 暴力破解强度不足。请改用 {@link #hashPassword(String)}
     * （PBKDF2，加盐+多轮迭代）。本方法仅为兼容历史哈希保留，切勿用于新的密码存储。
     */
    @Deprecated
    public static String encrypt(String password) {
        if (password == null) {
            return null;
        }
        String salt = generateSalt(16);
        String hashedPassword = sha256WithSalt(password, salt);
        return salt + ":" + hashedPassword;
    }

    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PBKDF2_PREFIX = "pbkdf2";

    /**
     * 使用 PBKDF2(HmacSHA256, 加盐 + 多轮迭代) 计算密码哈希，用于安全存储密码。
     * 返回格式: {@code pbkdf2$<iterations>$<base64Salt>$<base64Hash>}。
     *
     * @param password 原始密码
     * @return 可存储的哈希字符串；password 为 null 时返回 null
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
        return PBKDF2_PREFIX + "$" + PBKDF2_ITERATIONS + "$"
                + Base64.encodeBase64String(salt) + "$" + Base64.encodeBase64String(hash);
    }

    /**
     * 校验密码是否与 {@link #hashPassword(String)} 产生的哈希匹配（恒定时间比较）。
     *
     * @param password    原始密码
     * @param storedHash  存储的哈希字符串
     * @return 是否匹配
     */
    public static boolean matchesPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PBKDF2_PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decodeBase64(parts[2]);
            byte[] expected = Base64.decodeBase64(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 密码哈希计算失败", e);
        }
    }
    
    /**
     * 验证密码
     * 
     * @param password 原始密码
     * @param encryptedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String password, String encryptedPassword) {
        if (password == null || encryptedPassword == null) {
            return false;
        }
        
        String[] parts = encryptedPassword.split(":");
        if (parts.length != 2) {
            return false;
        }
        
        String salt = parts[0];
        String hashedPassword = parts[1];
        String computedHash = sha256WithSalt(password, salt);
        
        return secureEquals(hashedPassword, computedHash);
    }
}
