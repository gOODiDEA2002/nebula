package io.nebula.lock;

/**
 * 锁获取异常
 * 
 * 当无法获取锁时抛出
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public class LockAcquisitionException extends LockException {
    
    public LockAcquisitionException(String message) {
        super(message);
    }
    
    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}

