package io.nebula.data.persistence.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Nebula基础服务接口
 * 扩展MyBatis-Plus的IService，添加更多便捷方法
 * 
 * @param <T> 实体类型
 */
public interface IService<T> extends com.baomidou.mybatisplus.extension.service.IService<T> {
    
    /**
     * 根据ID查询实体（Optional包装）
     * 
     * @param id 主键ID
     * @return Optional包装的实体
     */
    Optional<T> findById(Serializable id);
    
    /**
     * 根据条件查询单个实体（Optional包装）
     * 
     * @param queryWrapper 查询条件
     * @return Optional包装的实体
     */
    Optional<T> findOne(Wrapper<T> queryWrapper);
    
    /**
     * 根据字段值查询
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 结果列表
     */
    List<T> findByField(String field, Object value);
    
    /**
     * 根据字段值查询单个实体
     * 
     * @param field 字段名
     * @param value 字段值
     * @return Optional包装的实体
     */
    Optional<T> findOneByField(String field, Object value);
    
    /**
     * 根据多个字段值查询
     * 
     * @param fieldValues 字段值映射
     * @return 结果列表
     */
    List<T> findByFields(java.util.Map<String, Object> fieldValues);
    
    /**
     * 分页查询（返回Page对象）
     * 
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    Page<T> findPage(IPage<T> page, Wrapper<T> queryWrapper);
    
    /**
     * 简单分页查询
     * 
     * @param current  当前页码
     * @param size     每页大小
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    Page<T> findPage(long current, long size, Wrapper<T> queryWrapper);
    
    /**
     * 查询前N条记录
     * 
     * @param queryWrapper 查询条件
     * @param limit        限制数量
     * @return 结果列表
     */
    List<T> findTopN(Wrapper<T> queryWrapper, int limit);
    
    /**
     * 随机查询N条记录
     * 
     * @param limit 限制数量
     * @return 结果列表
     */
    List<T> findRandomN(int limit);
    
    /**
     * 检查记录是否存在
     * 
     * @param queryWrapper 查询条件
     * @return 是否存在
     */
    boolean exists(Wrapper<T> queryWrapper);
    
    /**
     * 检查ID是否存在
     * 
     * @param id 主键ID
     * @return 是否存在
     */
    boolean existsById(Serializable id);
    
    /**
     * 统计记录数
     * 
     * @param queryWrapper 查询条件
     * @return 记录数
     */
    long countByCondition(Wrapper<T> queryWrapper);
    
    /**
     * 保存或更新实体
     * 如果实体有ID且数据库中存在，则更新；否则插入
     * 
     * @param entity 实体对象
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean saveOrUpdate(T entity);
    
    /**
     * 批量保存或更新
     * 
     * @param entityList 实体列表
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean saveOrUpdateBatch(Collection<T> entityList);
    
    /**
     * 批量插入（忽略已存在的记录）
     * 
     * @param entityList 实体列表
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean saveBatchIgnore(Collection<T> entityList);
    
    /**
     * 批量更新
     * 
     * @param entityList 实体列表
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean updateBatchById(Collection<T> entityList);
    
    /**
     * 根据条件更新
     * 
     * @param entity       更新的实体
     * @param updateWrapper 更新条件
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean updateByCondition(T entity, Wrapper<T> updateWrapper);
    
    /**
     * 物理删除（真正删除数据）
     * 
     * @param id 主键ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean removeByIdPhysical(Serializable id);
    
    /**
     * 根据条件物理删除
     * 
     * @param queryWrapper 删除条件
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    boolean removePhysical(Wrapper<T> queryWrapper);
    
    /**
     * 在事务中执行操作
     * 
     * @param action 操作函数
     * @param <R>    返回值类型
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    <R> R executeInTransaction(Function<IService<T>, R> action);
    
    /**
     * 批量操作（大数据量时自动分批处理）
     * 
     * @param entityList  实体列表
     * @param batchSize   批次大小
     * @param processor   处理函数
     * @param <R>         返回值类型
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    <R> R batchProcess(Collection<T> entityList, int batchSize, Function<Collection<T>, R> processor);
    
    /**
     * 获取实体类型
     * 
     * @return 实体类型
     */
    Class<T> getEntityClass();
    
    /**
     * 获取表名
     * 
     * @return 表名
     */
    String getTableName();
    
    /**
     * 刷新缓存（如果启用了缓存）
     */
    void refreshCache();
    
    /**
     * 清空缓存（如果启用了缓存）
     */
    void clearCache();
}
