package io.nebula.autoconfigure.data.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 测试用：继承 MyBatis-Plus 原生 BaseMapper(非 nebula BaseMapper)的 Mapper，
 * 用于验证 Nebula 的 MapperScannerConfigurer 能扫描到标准 MP Mapper。
 */
public interface StandardMybatisPlusMapper extends BaseMapper<Object> {
}
