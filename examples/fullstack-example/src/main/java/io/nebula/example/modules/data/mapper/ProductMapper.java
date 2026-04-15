package io.nebula.example.modules.data.mapper;

import io.nebula.example.modules.data.entity.dos.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import io.nebula.data.persistence.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
 * 产品Mapper接口
 * 演示 Nebula 数据访问层的 MyBatis-Plus 集成
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    
    @Select("SELECT category, COUNT(id) as count, AVG(price) as avgPrice FROM t_products GROUP BY category")
    List<Map<String, Object>> getCategoryStatistics();
}
