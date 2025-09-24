package io.nebula.ai.core.exception;

import io.nebula.core.common.exception.NebulaException;

/**
 * AI服务基础异常
 */
public class AIException extends NebulaException {

    public AIException(String message) {
        super("AI_ERROR", message);
    }

    public AIException(String message, Throwable cause) {
        super("AI_ERROR", message, cause);
    }

    public AIException(String message, String errorCode) {
        super(errorCode, message);
    }

    public AIException(String message, String errorCode, Throwable cause) {
        super(errorCode, message, cause);
    }
}
