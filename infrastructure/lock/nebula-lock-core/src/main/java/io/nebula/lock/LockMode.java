package io.nebula.lock;

/**
 * 锁模式枚举
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public enum LockMode {
    
    /**
     * 独占模式
     * 同一时间只有一个线程可以持有锁
     */
    EXCLUSIVE("独占模式"),
    
    /**
     * 共享模式
     * 多个线程可以同时持有锁(如读锁)
     */
    SHARED("共享模式");
    
    private final String description;
    
    LockMode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

