package io.nebula.data.access.repository.impl;

import io.nebula.data.access.repository.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Repository的抽象基础实现
 * 提供通用的CRUD操作实现
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public abstract class AbstractRepository<T, ID> implements Repository<T, ID> {
    
    /**
     * 获取实体的ID
     * 子类必须实现此方法来从实体中提取ID
     */
    protected abstract ID getEntityId(T entity);
    
    /**
     * 设置实体的ID
     * 子类必须实现此方法来为实体设置ID
     */
    protected abstract void setEntityId(T entity, ID id);
    
    /**
     * 生成新的ID
     * 子类可以重写此方法来自定义ID生成策略
     */
    protected abstract ID generateId();
    
    /**
     * 实际的数据存储
     * 子类应该提供具体的数据存储实现
     */
    protected abstract Map<ID, T> getDataStore();
    
    /**
     * 创建实体副本
     * 用于避免返回原始对象引用
     */
    protected abstract T cloneEntity(T entity);
    
    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            log.warn("Cannot find entity with null id");
            return Optional.empty();
        }
        
        try {
            T entity = getDataStore().get(id);
            return entity != null ? Optional.of(cloneEntity(entity)) : Optional.empty();
        } catch (Exception e) {
            log.error("Error finding entity by id: {}", id, e);
            throw new RuntimeException("Failed to find entity by id", e);
        }
    }
    
    @Override
    public List<T> findAll() {
        try {
            return getDataStore().values().stream()
                    .map(this::cloneEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding all entities", e);
            throw new RuntimeException("Failed to find all entities", e);
        }
    }
    
    @Override
    public Page<T> findAll(Pageable pageable) {
        try {
            List<T> allEntities = findAll();
            
            int start = (int) (pageable.getPageNumber() * pageable.getPageSize());
            int end = Math.min(start + pageable.getPageSize(), allEntities.size());
            
            if (start >= allEntities.size()) {
                return new PageImpl<>(Collections.emptyList(), pageable, allEntities.size());
            }
            
            List<T> pageData = allEntities.subList(start, end);
            return new PageImpl<>(pageData, pageable, allEntities.size());
        } catch (Exception e) {
            log.error("Error finding all entities with pagination", e);
            throw new RuntimeException("Failed to find all entities with pagination", e);
        }
    }
    
    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        
        try {
            List<T> result = new ArrayList<>();
            for (ID id : ids) {
                findById(id).ifPresent(result::add);
            }
            return result;
        } catch (Exception e) {
            log.error("Error finding entities by ids", e);
            throw new RuntimeException("Failed to find entities by ids", e);
        }
    }
    
    @Override
    public <S extends T> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        try {
            ID id = getEntityId(entity);
            if (id == null) {
                id = generateId();
                setEntityId(entity, id);
                log.debug("Generated new id {} for entity", id);
            }
            
            S clonedEntity = (S) cloneEntity(entity);
            getDataStore().put(id, clonedEntity);
            
            log.debug("Saved entity with id: {}", id);
            return clonedEntity;
        } catch (Exception e) {
            log.error("Error saving entity", e);
            throw new RuntimeException("Failed to save entity", e);
        }
    }
    
    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        
        try {
            List<S> result = new ArrayList<>();
            for (S entity : entities) {
                result.add(save(entity));
            }
            return result;
        } catch (Exception e) {
            log.error("Error saving entities", e);
            throw new RuntimeException("Failed to save entities", e);
        }
    }
    
    @Override
    public <S extends T> S saveAndFlush(S entity) {
        S savedEntity = save(entity);
        flush();
        return savedEntity;
    }
    
    @Override
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
        List<S> savedEntities = saveAll(entities);
        flush();
        return savedEntities;
    }
    
    @Override
    public void deleteById(ID id) {
        if (id == null) {
            log.warn("Cannot delete entity with null id");
            return;
        }
        
        try {
            T removed = getDataStore().remove(id);
            if (removed != null) {
                log.debug("Deleted entity with id: {}", id);
            } else {
                log.warn("Entity with id {} not found for deletion", id);
            }
        } catch (Exception e) {
            log.error("Error deleting entity by id: {}", id, e);
            throw new RuntimeException("Failed to delete entity by id", e);
        }
    }
    
    @Override
    public void delete(T entity) {
        if (entity == null) {
            log.warn("Cannot delete null entity");
            return;
        }
        
        ID id = getEntityId(entity);
        deleteById(id);
    }
    
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        if (entities == null) {
            return;
        }
        
        try {
            for (T entity : entities) {
                delete(entity);
            }
        } catch (Exception e) {
            log.error("Error deleting entities", e);
            throw new RuntimeException("Failed to delete entities", e);
        }
    }
    
    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        if (ids == null) {
            return;
        }
        
        try {
            for (ID id : ids) {
                deleteById(id);
            }
        } catch (Exception e) {
            log.error("Error deleting entities by ids", e);
            throw new RuntimeException("Failed to delete entities by ids", e);
        }
    }
    
    @Override
    public void deleteAll() {
        try {
            getDataStore().clear();
            log.debug("Deleted all entities");
        } catch (Exception e) {
            log.error("Error deleting all entities", e);
            throw new RuntimeException("Failed to delete all entities", e);
        }
    }
    
    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }
        
        try {
            return getDataStore().containsKey(id);
        } catch (Exception e) {
            log.error("Error checking existence of entity by id: {}", id, e);
            throw new RuntimeException("Failed to check entity existence", e);
        }
    }
    
    @Override
    public long count() {
        try {
            return getDataStore().size();
        } catch (Exception e) {
            log.error("Error counting entities", e);
            throw new RuntimeException("Failed to count entities", e);
        }
    }
    
    @Override
    public void flush() {
        // 默认实现为空，子类可以重写以提供实际的flush逻辑
        log.debug("Flush called (default implementation does nothing)");
    }
    
    @Override
    public void deleteInBatch(Iterable<T> entities) {
        // 批量删除的默认实现，子类可以重写以提供更高效的实现
        deleteAll(entities);
    }
    
    @Override
    public void deleteAllInBatch() {
        // 批量删除所有的默认实现，子类可以重写以提供更高效的实现
        deleteAll();
    }
    
    @Override
    public void deleteAllByIdInBatch(Iterable<ID> ids) {
        // 批量按ID删除的默认实现，子类可以重写以提供更高效的实现
        deleteAllById(ids);
    }
    
    @Override
    public T getReference(ID id) {
        // 默认实现返回实际对象，子类可以重写以提供懒加载实现
        return findById(id).orElse(null);
    }
    
    @Override
    public T getReferenceById(ID id) {
        return getReference(id);
    }
    
    /**
     * 异步保存实体
     */
    public CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> save(entity));
    }
    
    /**
     * 异步查找实体
     */
    public CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> findById(id));
    }
    
    /**
     * 异步删除实体
     */
    public CompletableFuture<Void> deleteByIdAsync(ID id) {
        return CompletableFuture.runAsync(() -> deleteById(id));
    }
    
    /**
     * 批量操作处理器
     */
    protected <R> R processBatch(Iterable<T> entities, int batchSize, Function<List<T>, R> processor) {
        List<T> batch = new ArrayList<>();
        List<R> results = new ArrayList<>();
        
        for (T entity : entities) {
            batch.add(entity);
            if (batch.size() >= batchSize) {
                results.add(processor.apply(new ArrayList<>(batch)));
                batch.clear();
            }
        }
        
        // 处理剩余的批次
        if (!batch.isEmpty()) {
            results.add(processor.apply(batch));
        }
        
        // 返回第一个结果，子类可以重写以自定义结果合并逻辑
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * 获取实体类型
     */
    protected abstract Class<T> getEntityType();
    
    /**
     * 获取ID类型
     */
    protected abstract Class<ID> getIdType();
    
    /**
     * 验证实体
     */
    protected void validateEntity(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        // 子类可以重写以添加更多验证逻辑
    }
    
    /**
     * 验证ID
     */
    protected void validateId(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        // 子类可以重写以添加更多验证逻辑
    }
}
