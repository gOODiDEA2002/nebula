package io.nebula.ai.rag.index;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 内容哈希工具：SHA-256
 * <p>
 * 索引差分靠它判断文档内容是否变了。{@link DocumentSource} 未提供 {@code contentHash} 时，
 * 管线用本工具现算。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public final class Sha256ContentHash {

    private Sha256ContentHash() {
    }

    /**
     * 计算内容的十六进制 SHA-256；null 视为空串
     */
    public static String of(String content) {
        String text = content != null ? content : "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必备算法，走到这里说明运行环境异常
            throw new IllegalStateException("当前运行环境缺少 SHA-256 算法", e);
        }
    }
}
