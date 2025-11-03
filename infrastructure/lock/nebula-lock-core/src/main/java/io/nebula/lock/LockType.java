package io.nebula.lock;

/**
 * 锁类型枚举
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public enum LockType {
    
    /**
     * 可重入锁
     * 同一线程可以多次获取同一把锁
     */
    REENTRANT("可重入锁"),
    
    /**
     * 公平锁
     * 按照请求锁的顺序获取锁(FIFO)
     */
    FAIR("公平锁"),
    
    /**
     * 读写锁
     * 支持多个读锁,但写锁互斥
     */
    READ_WRITE("读写锁"),
    
    /**
     * 联锁/红锁(Redlock)
     * 多个Redis实例同时获取锁,防止单点故障
     */
    REDLOCK("红锁"),
    
    /**
     * 信号量
     * 限制同时访问资源的线程数
     */
    SEMAPHORE("信号量");
    
    private final String description;
    
    LockType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

