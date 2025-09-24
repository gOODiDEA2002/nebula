package io.nebula.ai.core.exception;

/**
 * 向量存储服务异常
 */
public class VectorStoreException extends AIException {

    public VectorStoreException(String message) {
        super(message, "VECTOR_STORE_ERROR");
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, "VECTOR_STORE_ERROR", cause);
    }

    public VectorStoreException(String message, String errorCode) {
        super(message, errorCode);
    }

    public VectorStoreException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
