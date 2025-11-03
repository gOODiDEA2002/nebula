package io.nebula.lock;

/**
 * 锁释放异常
 * 
 * 当释放锁失败时抛出
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class LockReleaseException extends LockException {
    
    public LockReleaseException(String message) {
        super(message);
    }
    
    public LockReleaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

