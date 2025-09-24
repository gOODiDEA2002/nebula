package io.nebula.data.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Nebula基础Mapper接口
 * 扩展MyBatis-Plus的BaseMapper，添加更多便捷方法
 * 
 * @param <T> 实体类型
 */
public interface BaseMapper<T> extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T> {
    
    /**
     * 根据条件查询单个实体（Optional包装）
     * 
     * @param queryWrapper 查询条件
     * @return Optional包装的实体
     */
    default Optional<T> selectOneOpt(@Param("ew") Wrapper<T> queryWrapper) {
        return Optional.ofNullable(selectOne(queryWrapper));
    }
    
    /**
     * 根据ID查询实体（Optional包装）
     * 
     * @param id 主键ID
     * @return Optional包装的实体
     */
    default Optional<T> selectByIdOpt(Serializable id) {
        return Optional.ofNullable(selectById(id));
    }
    
    // 注意：以下方法需要在具体的Mapper实现中根据需要添加
    // 这里仅保留接口定义，避免编译错误
    
    /**
     * 检查记录是否存在
     * 
     * @param queryWrapper 查询条件
     * @return 是否存在
     */
    default boolean exists(@Param("ew") Wrapper<T> queryWrapper) {
        return selectCount(queryWrapper) > 0;
    }
    
    /**
     * 检查ID是否存在
     * 
     * @param id 主键ID
     * @return 是否存在
     */
    default boolean existsById(Serializable id) {
        return selectById(id) != null;
    }
    
    /**
     * 分页查询（返回Page对象）
     * 
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    default Page<T> selectPageList(IPage<T> page, @Param("ew") Wrapper<T> queryWrapper) {
        return (Page<T>) selectPage(page, queryWrapper);
    }
    
    // 高级查询方法已简化，避免编译错误
    // 可在具体实现中根据需要添加
    
    /**
     * 统计指定条件的记录数
     * 
     * @param queryWrapper 查询条件
     * @return 记录数
     */
    default Long count(@Param("ew") Wrapper<T> queryWrapper) {
        return selectCount(queryWrapper);
    }
}
