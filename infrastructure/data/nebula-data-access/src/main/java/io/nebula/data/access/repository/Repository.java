package io.nebula.data.access.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 通用仓储接口
 * 提供基本的CRUD操作
 * 
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface Repository<T, ID> {
    
    /**
     * 根据ID查找实体
     * 
     * @param id 主键ID
     * @return 实体，如果不存在则返回空Optional
     */
    Optional<T> findById(ID id);
    
    /**
     * 查找所有实体
     * 
     * @return 实体列表
     */
    List<T> findAll();
    
    /**
     * 分页查找实体
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<T> findAll(Pageable pageable);
    
    /**
     * 根据ID列表查找实体
     * 
     * @param ids ID列表
     * @return 实体列表
     */
    List<T> findAllById(Iterable<ID> ids);
    
    /**
     * 保存实体
     * 
     * @param entity 实体对象
     * @param <S>    实体类型（继承自T）
     * @return 保存后的实体
     */
    <S extends T> S save(S entity);
    
    /**
     * 批量保存实体
     * 
     * @param entities 实体列表
     * @param <S>      实体类型（继承自T）
     * @return 保存后的实体列表
     */
    <S extends T> List<S> saveAll(Iterable<S> entities);
    
    /**
     * 保存并刷新
     * 
     * @param entity 实体对象
     * @param <S>    实体类型（继承自T）
     * @return 保存后的实体
     */
    <S extends T> S saveAndFlush(S entity);
    
    /**
     * 批量保存并刷新
     * 
     * @param entities 实体列表
     * @param <S>      实体类型（继承自T）
     * @return 保存后的实体列表
     */
    <S extends T> List<S> saveAllAndFlush(Iterable<S> entities);
    
    /**
     * 根据ID删除实体
     * 
     * @param id 主键ID
     */
    void deleteById(ID id);
    
    /**
     * 删除实体
     * 
     * @param entity 实体对象
     */
    void delete(T entity);
    
    /**
     * 批量删除实体
     * 
     * @param entities 实体列表
     */
    void deleteAll(Iterable<? extends T> entities);
    
    /**
     * 根据ID列表删除实体
     * 
     * @param ids ID列表
     */
    void deleteAllById(Iterable<? extends ID> ids);
    
    /**
     * 删除所有实体
     */
    void deleteAll();
    
    /**
     * 检查实体是否存在
     * 
     * @param id 主键ID
     * @return 是否存在
     */
    boolean existsById(ID id);
    
    /**
     * 统计实体数量
     * 
     * @return 实体数量
     */
    long count();
    
    /**
     * 刷新持久化上下文
     */
    void flush();
    
    /**
     * 删除并刷新
     * 
     * @param entity 实体对象
     */
    void deleteInBatch(Iterable<T> entity);
    
    /**
     * 删除所有实体并刷新
     */
    void deleteAllInBatch();
    
    /**
     * 根据ID删除并刷新
     * 
     * @param ids ID列表
     */
    void deleteAllByIdInBatch(Iterable<ID> ids);
    
    /**
     * 获取实体引用（延迟加载）
     * 
     * @param id 主键ID
     * @return 实体引用
     */
    T getReference(ID id);
    
    /**
     * 获取实体引用（延迟加载，如果不存在则返回null）
     * 
     * @param id 主键ID
     * @return 实体引用，如果不存在则返回null
     */
    T getReferenceById(ID id);
}
