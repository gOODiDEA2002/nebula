package io.nebula.data.persistence.service.impl;

import io.nebula.core.common.exception.ValidationException;
import io.nebula.data.persistence.mapper.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ServiceImpl 列名白名单校验测试。
 * 合法列名(字母/数字/下划线)放行, 含特殊字符的注入尝试抛 ValidationException。
 */
class ServiceImplColumnValidationTest {

    interface DummyMapper extends BaseMapper<Object> {}

    static class DummyService extends ServiceImpl<DummyMapper, Object> {}

    private DummyService service;

    @BeforeEach
    void setUp() {
        service = new DummyService();
        DummyMapper mapper = mock(DummyMapper.class);
        when(mapper.selectList(any())).thenReturn(java.util.List.of());
        when(mapper.selectOne(any())).thenReturn(null);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "user_name", "age", "column1", "STATUS", "field_2_x"})
    void legalColumnNamesPassValidation(String column) {
        assertThatCode(() -> service.findByField(column, "v")).doesNotThrowAnyException();
        assertThatCode(() -> service.findOneByField(column, "v")).doesNotThrowAnyException();
        assertThatCode(() -> service.findByFields(Map.of(column, "v"))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name; DROP TABLE x",
            "name)",
            "name' OR '1'='1",
            "name--",
            "col name",
            "col.name",
            "col,name"
    })
    void illegalColumnNamesRejected(String column) {
        assertValidationException(() -> service.findByField(column, "v"));
        assertValidationException(() -> service.findOneByField(column, "v"));
        assertValidationException(() -> service.findByFields(Map.of(column, "v")));
    }

    @Test
    void nullColumnNameRejected() {
        assertValidationException(() -> service.findByField(null, "v"));
    }

    @Test
    void emptyColumnNameRejected() {
        assertValidationException(() -> service.findByField("", "v"));
    }

    private static void assertValidationException(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException ve = (ValidationException) ex;
                    assertThat(ve.getFormattedMessage()).contains("非法列名");
                    assertThat(ve.getFieldErrors()).isNotEmpty();
                    assertThat(ve.getFieldErrors().get(0).getField()).isEqualTo("column");
                });
    }
}
