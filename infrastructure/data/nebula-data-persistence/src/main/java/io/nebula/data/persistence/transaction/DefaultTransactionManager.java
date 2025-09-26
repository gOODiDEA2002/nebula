package io.nebula.data.persistence.transaction;

import io.nebula.data.persistence.transaction.TransactionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 默认事务管理器实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultTransactionManager implements TransactionManager {
    
    private final PlatformTransactionManager platformTransactionManager;
    private final Executor taskExecutor;
    
    @Override
    public <T> T executeInTransaction(TransactionCallback<T> callback) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        return transactionTemplate.execute(status -> {
            try {
                return callback.doInTransaction(status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public void executeInTransaction(TransactionCallbackWithoutResult callback) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.execute(status -> {
            try {
                callback.doInTransaction(status);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public <T> T executeInReadOnlyTransaction(TransactionCallback<T> callback) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(status -> {
            try {
                return callback.doInTransaction(status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public <T> T executeInTransaction(TransactionCallback<T> callback, int propagationBehavior) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(propagationBehavior);
        return executeInTransaction(callback, definition);
    }
    
    @Override
    public <T> T executeInTransactionWithIsolation(TransactionCallback<T> callback, int isolationLevel) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setIsolationLevel(isolationLevel);
        return executeInTransaction(callback, definition);
    }
    
    @Override
    public <T> T executeInTransactionWithTimeout(TransactionCallback<T> callback, int timeout) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setTimeout(timeout);
        return executeInTransaction(callback, definition);
    }
    
    @Override
    public <T> T executeInTransaction(TransactionCallback<T> callback, TransactionDefinition transactionDef) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager, transactionDef);
        return transactionTemplate.execute(status -> {
            try {
                return callback.doInTransaction(status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public <T> CompletableFuture<T> executeInTransactionAsync(TransactionCallback<T> callback) {
        return CompletableFuture.supplyAsync(() -> executeInTransaction(callback), taskExecutor);
    }
    
    @Override
    public TransactionStatus beginTransaction() {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        return platformTransactionManager.getTransaction(definition);
    }
    
    @Override
    public TransactionStatus beginTransaction(TransactionDefinition definition) {
        return platformTransactionManager.getTransaction(definition);
    }
    
    @Override
    public void commit(TransactionStatus status) {
        try {
            platformTransactionManager.commit(status);
            log.debug("事务提交成功");
        } catch (Exception e) {
            log.error("事务提交失败", e);
            throw e;
        }
    }
    
    @Override
    public void rollback(TransactionStatus status) {
        try {
            platformTransactionManager.rollback(status);
            log.debug("事务回滚成功");
        } catch (Exception e) {
            log.error("事务回滚失败", e);
            throw e;
        }
    }
    
    @Override
    public TransactionStatus getCurrentTransactionStatus() {
        if (!isInTransaction()) {
            return null;
        }
        // 这里需要通过其他方式获取当前事务状态
        // Spring的TransactionSynchronizationManager不直接提供TransactionStatus
        return null;
    }
    
    @Override
    public boolean isInTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }
    
    @Override
    public void setRollbackOnly() {
        if (isInTransaction()) {
            // Spring 6.0以后，需要通过TransactionStatus来设置
            log.debug("设置当前事务为只回滚（需要在事务回调中使用TransactionStatus）");
        } else {
            log.warn("当前没有活动事务，无法设置只回滚");
        }
    }
    
    @Override
    public boolean isRollbackOnly() {
        // Spring 6.0以后，需要通过TransactionStatus来检查
        log.warn("检查回滚状态需要在事务回调中使用TransactionStatus");
        return false;
    }
    
    @Override
    public Object createSavepoint(String name) {
        // Spring的TransactionStatus提供保存点功能，但需要在事务中调用
        throw new UnsupportedOperationException("创建保存点需要在事务回调中使用TransactionStatus");
    }
    
    @Override
    public void rollbackToSavepoint(Object savepoint) {
        throw new UnsupportedOperationException("回滚到保存点需要在事务回调中使用TransactionStatus");
    }
    
    @Override
    public void releaseSavepoint(Object savepoint) {
        throw new UnsupportedOperationException("释放保存点需要在事务回调中使用TransactionStatus");
    }
}
