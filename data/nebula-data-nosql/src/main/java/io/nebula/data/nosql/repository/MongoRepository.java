package io.nebula.data.nosql.repository;

import io.nebula.data.access.repository.Repository;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB仓储接口
 * 扩展基础Repository，添加MongoDB特有的功能
 * 
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface MongoRepository<T, ID> extends Repository<T, ID> {
    
    /**
     * 根据字段查询
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 实体列表
     */
    List<T> findByField(String field, Object value);
    
    /**
     * 根据字段查询第一个
     * 
     * @param field 字段名
     * @param value 字段值
     * @return Optional包装的实体
     */
    Optional<T> findFirstByField(String field, Object value);
    
    /**
     * 根据多个字段查询
     * 
     * @param criteria 查询条件
     * @return 实体列表
     */
    List<T> findByCriteria(Criteria criteria);
    
    /**
     * 根据Query查询
     * 
     * @param query 查询对象
     * @return 实体列表
     */
    List<T> find(Query query);
    
    /**
     * 根据Query查询第一个
     * 
     * @param query 查询对象
     * @return Optional包装的实体
     */
    Optional<T> findFirst(Query query);
    
    /**
     * 根据Query查询单个（唯一结果）
     * 
     * @param query 查询对象
     * @return Optional包装的实体
     */
    Optional<T> findOne(Query query);
    
    /**
     * 根据正则表达式查询
     * 
     * @param field   字段名
     * @param pattern 正则表达式
     * @return 实体列表
     */
    List<T> findByRegex(String field, String pattern);
    
    /**
     * 根据文本搜索
     * 
     * @param searchText 搜索文本
     * @return 实体列表
     */
    List<T> findByText(String searchText);
    
    /**
     * 范围查询
     * 
     * @param field 字段名
     * @param start 开始值
     * @param end   结束值
     * @return 实体列表
     */
    List<T> findByRange(String field, Object start, Object end);
    
    /**
     * IN查询
     * 
     * @param field  字段名
     * @param values 值列表
     * @return 实体列表
     */
    List<T> findByIn(String field, List<?> values);
    
    /**
     * 排序查询
     * 
     * @param sort 排序对象
     * @return 实体列表
     */
    List<T> findAllSorted(Sort sort);
    
    /**
     * 限制数量查询
     * 
     * @param query 查询对象
     * @param limit 限制数量
     * @return 实体列表
     */
    List<T> findWithLimit(Query query, int limit);
    
    /**
     * 跳过和限制查询
     * 
     * @param query  查询对象
     * @param skip   跳过数量
     * @param limit  限制数量
     * @return 实体列表
     */
    List<T> findWithSkipAndLimit(Query query, int skip, int limit);
    
    /**
     * 聚合查询
     * 
     * @param pipeline 聚合管道
     * @param <R>      结果类型
     * @return 聚合结果
     */
    <R> List<R> aggregate(List<Object> pipeline, Class<R> resultClass);
    
    /**
     * 地理位置附近查询
     * 
     * @param point    中心点
     * @param distance 距离
     * @return 地理查询结果
     */
    GeoResults<T> findNear(Point point, Distance distance);
    
    /**
     * 地理位置范围内查询
     * 
     * @param point    中心点
     * @param distance 距离
     * @return 实体列表
     */
    List<T> findWithin(Point point, Distance distance);
    
    /**
     * 更新字段
     * 
     * @param query  查询条件
     * @param field  字段名
     * @param value  字段值
     * @return 更新数量
     */
    long updateField(Query query, String field, Object value);
    
    /**
     * 批量更新
     * 
     * @param query   查询条件
     * @param updates 更新映射
     * @return 更新数量
     */
    long updateFields(Query query, java.util.Map<String, Object> updates);
    
    /**
     * 递增字段值
     * 
     * @param query 查询条件
     * @param field 字段名
     * @param value 递增值
     * @return 更新数量
     */
    long incrementField(Query query, String field, Number value);
    
    /**
     * 添加到数组
     * 
     * @param query 查询条件
     * @param field 数组字段名
     * @param value 要添加的值
     * @return 更新数量
     */
    long addToArray(Query query, String field, Object value);
    
    /**
     * 从数组中移除
     * 
     * @param query 查询条件
     * @param field 数组字段名
     * @param value 要移除的值
     * @return 更新数量
     */
    long removeFromArray(Query query, String field, Object value);
    
    /**
     * Upsert操作（存在则更新，不存在则插入）
     * 
     * @param entity 实体对象
     * @return 保存后的实体
     */
    T upsert(T entity);
    
    /**
     * 批量Upsert操作
     * 
     * @param entities 实体列表
     * @return 保存后的实体列表
     */
    List<T> upsertAll(List<T> entities);
    
    /**
     * 根据条件删除
     * 
     * @param query 删除条件
     * @return 删除数量
     */
    long remove(Query query);
    
    /**
     * 根据字段删除
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 删除数量
     */
    long removeByField(String field, Object value);
    
    /**
     * 检查是否存在
     * 
     * @param query 查询条件
     * @return 是否存在
     */
    boolean exists(Query query);
    
    /**
     * 根据条件统计数量
     * 
     * @param query 查询条件
     * @return 数量
     */
    long count(Query query);
    
    /**
     * 获取集合名称
     * 
     * @return 集合名称
     */
    String getCollectionName();
    
    /**
     * 创建索引
     * 
     * @param field 字段名
     */
    void createIndex(String field);
    
    /**
     * 创建复合索引
     * 
     * @param fields 字段列表
     */
    void createCompoundIndex(String... fields);
    
    /**
     * 创建文本索引
     * 
     * @param fields 文本字段列表
     */
    void createTextIndex(String... fields);
    
    /**
     * 创建地理位置索引
     * 
     * @param field 地理位置字段名
     */
    void createGeoIndex(String field);
    
    /**
     * 删除索引
     * 
     * @param indexName 索引名称
     */
    void dropIndex(String indexName);
    
    /**
     * 获取所有索引信息
     * 
     * @return 索引信息列表
     */
    List<String> getIndexes();
}
