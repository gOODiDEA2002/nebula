package io.nebula.data.persistence.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.nebula.data.persistence.mapper.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 ServiceImpl 的按字段查询不再是假实现：
 * 修复前 findByField 返回全表、findOneByField 把字段值当主键。
 */
class ServiceImplQueryTest {

    interface DummyMapper extends BaseMapper<Object> {
    }

    static class DummyService extends ServiceImpl<DummyMapper, Object> {
    }

    private DummyMapper mapper;
    private DummyService service;

    @BeforeEach
    void setUp() {
        mapper = mock(DummyMapper.class);
        service = new DummyService();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        lenient().when(mapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void findByFieldBuildsConditionalQueryNotFullTable() {
        service.findByField("name", "alice");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Object>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        // 构建了带字段条件的查询(而非老实现的无条件 list())
        assertThat(captor.getValue().getSqlSegment()).contains("name");
    }

    @Test
    void findOneByFieldDoesNotTreatValueAsPrimaryKey() {
        service.findOneByField("email", "a@b.com");

        // 修复前会 selectByIdOpt(把 email 值当主键)；修复后走条件查询
        verify(mapper, never()).selectByIdOpt(any());
    }
}
