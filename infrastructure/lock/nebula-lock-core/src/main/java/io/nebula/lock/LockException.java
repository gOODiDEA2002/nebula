package io.nebula.lock;

/**
 * 锁异常基类
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class LockException extends RuntimeException {
    
    public LockException(String message) {
        super(message);
    }
    
    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LockException(Throwable cause) {
        super(cause);
    }
}

