package io.nebula.data.access.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * 查询构建器接口
 * 提供链式查询构建功能
 * 
 * @param <T> 实体类型
 */
public interface QueryBuilder<T> {
    
    /**
     * 添加等值条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> eq(String field, Object value);
    
    /**
     * 添加不等值条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> ne(String field, Object value);
    
    /**
     * 添加大于条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> gt(String field, Object value);
    
    /**
     * 添加大于等于条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> gte(String field, Object value);
    
    /**
     * 添加小于条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> lt(String field, Object value);
    
    /**
     * 添加小于等于条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> lte(String field, Object value);
    
    /**
     * 添加模糊匹配条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> like(String field, Object value);
    
    /**
     * 添加左模糊匹配条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> likeLeft(String field, Object value);
    
    /**
     * 添加右模糊匹配条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @return 查询构建器
     */
    QueryBuilder<T> likeRight(String field, Object value);
    
    /**
     * 添加IN条件
     * 
     * @param field  字段名
     * @param values 字段值列表
     * @return 查询构建器
     */
    QueryBuilder<T> in(String field, Object... values);
    
    /**
     * 添加IN条件
     * 
     * @param field  字段名
     * @param values 字段值集合
     * @return 查询构建器
     */
    QueryBuilder<T> in(String field, Iterable<?> values);
    
    /**
     * 添加NOT IN条件
     * 
     * @param field  字段名
     * @param values 字段值列表
     * @return 查询构建器
     */
    QueryBuilder<T> notIn(String field, Object... values);
    
    /**
     * 添加NOT IN条件
     * 
     * @param field  字段名
     * @param values 字段值集合
     * @return 查询构建器
     */
    QueryBuilder<T> notIn(String field, Iterable<?> values);
    
    /**
     * 添加BETWEEN条件
     * 
     * @param field 字段名
     * @param start 开始值
     * @param end   结束值
     * @return 查询构建器
     */
    QueryBuilder<T> between(String field, Object start, Object end);
    
    /**
     * 添加NOT BETWEEN条件
     * 
     * @param field 字段名
     * @param start 开始值
     * @param end   结束值
     * @return 查询构建器
     */
    QueryBuilder<T> notBetween(String field, Object start, Object end);
    
    /**
     * 添加IS NULL条件
     * 
     * @param field 字段名
     * @return 查询构建器
     */
    QueryBuilder<T> isNull(String field);
    
    /**
     * 添加IS NOT NULL条件
     * 
     * @param field 字段名
     * @return 查询构建器
     */
    QueryBuilder<T> isNotNull(String field);
    
    /**
     * 开始AND分组
     * 
     * @return 查询构建器
     */
    QueryBuilder<T> and();
    
    /**
     * 开始OR分组
     * 
     * @return 查询构建器
     */
    QueryBuilder<T> or();
    
    /**
     * 开始分组
     * 
     * @return 查询构建器
     */
    QueryBuilder<T> groupStart();
    
    /**
     * 结束分组
     * 
     * @return 查询构建器
     */
    QueryBuilder<T> groupEnd();
    
    /**
     * 添加排序条件（升序）
     * 
     * @param field 字段名
     * @return 查询构建器
     */
    QueryBuilder<T> orderByAsc(String field);
    
    /**
     * 添加排序条件（降序）
     * 
     * @param field 字段名
     * @return 查询构建器
     */
    QueryBuilder<T> orderByDesc(String field);
    
    /**
     * 添加排序条件
     * 
     * @param sort 排序对象
     * @return 查询构建器
     */
    QueryBuilder<T> orderBy(Sort sort);
    
    /**
     * 设置查询限制数量
     * 
     * @param limit 限制数量
     * @return 查询构建器
     */
    QueryBuilder<T> limit(int limit);
    
    /**
     * 设置查询偏移量
     * 
     * @param offset 偏移量
     * @return 查询构建器
     */
    QueryBuilder<T> offset(int offset);
    
    /**
     * 分页查询
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<T> page(Pageable pageable);
    
    /**
     * 查询列表
     * 
     * @return 结果列表
     */
    List<T> list();
    
    /**
     * 查询第一个结果
     * 
     * @return 第一个结果，如果不存在则返回空Optional
     */
    Optional<T> first();
    
    /**
     * 查询单个结果
     * 如果结果数量不为1，抛出异常
     * 
     * @return 单个结果
     */
    T one();
    
    /**
     * 查询单个结果
     * 如果结果数量大于1，抛出异常；如果为0，返回空Optional
     * 
     * @return 单个结果，如果不存在则返回空Optional
     */
    Optional<T> oneOrEmpty();
    
    /**
     * 统计数量
     * 
     * @return 结果数量
     */
    long count();
    
    /**
     * 检查是否存在
     * 
     * @return 是否存在匹配的记录
     */
    boolean exists();
    
    /**
     * 清空所有条件
     * 
     * @return 查询构建器
     */
    QueryBuilder<T> clear();
    
    /**
     * 克隆查询构建器
     * 
     * @return 新的查询构建器副本
     */
    QueryBuilder<T> clone();
}
