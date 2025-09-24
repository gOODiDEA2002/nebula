package io.nebula.ai.core.exception;

/**
 * 嵌入服务异常
 */
public class EmbeddingException extends AIException {

    public EmbeddingException(String message) {
        super(message, "EMBEDDING_ERROR");
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, "EMBEDDING_ERROR", cause);
    }

    public EmbeddingException(String message, String errorCode) {
        super(message, errorCode);
    }

    public EmbeddingException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
