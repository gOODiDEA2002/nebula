package io.nebula.data.access.transaction;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 统一事务管理器接口
 * 提供声明式和编程式事务支持
 */
public interface TransactionManager {
    
    /**
     * 在事务中执行操作
     * 
     * @param callback 事务回调
     * @param <T>      返回值类型
     * @return 操作结果
     */
    <T> T executeInTransaction(TransactionCallback<T> callback);
    
    /**
     * 在事务中执行操作（无返回值）
     * 
     * @param callback 事务回调
     */
    void executeInTransaction(TransactionCallbackWithoutResult callback);
    
    /**
     * 在只读事务中执行操作
     * 
     * @param callback 事务回调
     * @param <T>      返回值类型
     * @return 操作结果
     */
    <T> T executeInReadOnlyTransaction(TransactionCallback<T> callback);
    
    /**
     * 在指定传播行为的事务中执行操作
     * 
     * @param callback            事务回调
     * @param propagationBehavior 事务传播行为
     * @param <T>                 返回值类型
     * @return 操作结果
     */
    <T> T executeInTransaction(TransactionCallback<T> callback, int propagationBehavior);
    
    /**
     * 在指定隔离级别的事务中执行操作
     * 
     * @param callback        事务回调
     * @param isolationLevel  隔离级别
     * @param <T>             返回值类型
     * @return 操作结果
     */
    <T> T executeInTransactionWithIsolation(TransactionCallback<T> callback, int isolationLevel);
    
    /**
     * 在指定超时时间的事务中执行操作
     * 
     * @param callback 事务回调
     * @param timeout  超时时间（秒）
     * @param <T>      返回值类型
     * @return 操作结果
     */
    <T> T executeInTransactionWithTimeout(TransactionCallback<T> callback, int timeout);
    
    /**
     * 在自定义事务定义中执行操作
     * 
     * @param callback           事务回调
     * @param transactionDef     事务定义
     * @param <T>                返回值类型
     * @return 操作结果
     */
    <T> T executeInTransaction(TransactionCallback<T> callback, TransactionDefinition transactionDef);
    
    /**
     * 异步执行事务操作
     * 
     * @param callback 事务回调
     * @param <T>      返回值类型
     * @return CompletableFuture包装的结果
     */
    <T> CompletableFuture<T> executeInTransactionAsync(TransactionCallback<T> callback);
    
    /**
     * 开始新事务
     * 
     * @return 事务状态
     */
    TransactionStatus beginTransaction();
    
    /**
     * 开始指定定义的事务
     * 
     * @param definition 事务定义
     * @return 事务状态
     */
    TransactionStatus beginTransaction(TransactionDefinition definition);
    
    /**
     * 提交事务
     * 
     * @param status 事务状态
     */
    void commit(TransactionStatus status);
    
    /**
     * 回滚事务
     * 
     * @param status 事务状态
     */
    void rollback(TransactionStatus status);
    
    /**
     * 获取当前事务状态
     * 
     * @return 事务状态，如果没有事务则返回null
     */
    TransactionStatus getCurrentTransactionStatus();
    
    /**
     * 检查是否在事务中
     * 
     * @return 是否在事务中
     */
    boolean isInTransaction();
    
    /**
     * 设置当前事务为只回滚
     */
    void setRollbackOnly();
    
    /**
     * 检查当前事务是否为只回滚
     * 
     * @return 是否只回滚
     */
    boolean isRollbackOnly();
    
    /**
     * 创建保存点
     * 
     * @param name 保存点名称
     * @return 保存点对象
     */
    Object createSavepoint(String name);
    
    /**
     * 回滚到保存点
     * 
     * @param savepoint 保存点对象
     */
    void rollbackToSavepoint(Object savepoint);
    
    /**
     * 释放保存点
     * 
     * @param savepoint 保存点对象
     */
    void releaseSavepoint(Object savepoint);
    
    /**
     * 事务回调接口
     * 
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface TransactionCallback<T> {
        /**
         * 在事务中执行的操作
         * 
         * @param status 事务状态
         * @return 操作结果
         * @throws Exception 可能的异常
         */
        T doInTransaction(TransactionStatus status) throws Exception;
    }
    
    /**
     * 无返回值的事务回调接口
     */
    @FunctionalInterface
    interface TransactionCallbackWithoutResult {
        /**
         * 在事务中执行的操作
         * 
         * @param status 事务状态
         * @throws Exception 可能的异常
         */
        void doInTransaction(TransactionStatus status) throws Exception;
    }
    
    /**
     * 函数式事务回调接口
     * 
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface TransactionSupplier<T> extends Supplier<T> {
        /**
         * 获取结果
         * 
         * @return 操作结果
         */
        T get();
    }
}
