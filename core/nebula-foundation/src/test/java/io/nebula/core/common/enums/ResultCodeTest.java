package io.nebula.core.common.enums;

import io.nebula.core.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResultCode 枚举与 Result.of(ResultCode) 工厂的收敛测试
 */
class ResultCodeTest {

    /**
     * 全枚举 getByCode(getCode()) 回查一致——证明数字码体系内回查正确
     */
    @Test
    void getByCode_allEnumsRoundTrip() {
        for (ResultCode rc : ResultCode.values()) {
            assertThat(ResultCode.getByCode(rc.getCode()))
                    .as("getByCode(\"%s\") 应返回 %s", rc.getCode(), rc.name())
                    .isSameAs(rc);
        }
    }

    /**
     * getByName 全枚举回查一致
     */
    @Test
    void getByName_allEnumsRoundTrip() {
        for (ResultCode rc : ResultCode.values()) {
            assertThat(ResultCode.getByName(rc.name()))
                    .as("getByName(\"%s\") 应返回 %s", rc.name(), rc.name())
                    .isSameAs(rc);
        }
    }

    @Test
    void getByName_returnsNullForUnknown() {
        assertThat(ResultCode.getByName("NON_EXISTENT")).isNull();
        assertThat(ResultCode.getByName(null)).isNull();
    }

    @Test
    void getByCode_returnsNullForUnknown() {
        assertThat(ResultCode.getByCode("9999")).isNull();
        assertThat(ResultCode.getByCode(null)).isNull();
    }

    /**
     * Result.of(ResultCode.SUCCESS) 的 code 取枚举名 "SUCCESS"
     * 与 Result.success().getCode() 风格一致
     */
    @Test
    void resultOf_success_codeMatchesExistingStyle() {
        Result<Void> fromOf = Result.of(ResultCode.SUCCESS);
        Result<Void> fromSuccess = Result.success();

        assertThat(fromOf.getCode()).isEqualTo("SUCCESS");
        assertThat(fromOf.getCode()).isEqualTo(fromSuccess.getCode());
        assertThat(fromOf.isSuccess()).isTrue();
        assertThat(fromOf.getMessage()).isEqualTo(ResultCode.SUCCESS.getDescription());
    }

    /**
     * Result.of(ResultCode.XXX) 错误码 code 取枚举名
     */
    @Test
    void resultOf_error_codeIsEnumName() {
        Result<Void> result = Result.of(ResultCode.SYSTEM_ERROR);

        assertThat(result.getCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("系统内部错误");
    }

    /**
     * Result.of(ResultCode, data) 带数据
     */
    @Test
    void resultOf_withData() {
        Result<String> result = Result.of(ResultCode.SUCCESS, "hello");

        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("hello");
    }

    /**
     * 既有 Result 工厂方法的码值回归——确保没被改动
     */
    @Test
    void existingResultFactories_codeValuesUnchanged() {
        assertThat(Result.success().getCode()).isEqualTo("SUCCESS");
        assertThat(Result.success("data").getCode()).isEqualTo("SUCCESS");
        assertThat(Result.success("data", "msg").getCode()).isEqualTo("SUCCESS");
        assertThat(Result.systemError().getCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(Result.systemError("msg").getCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(Result.businessError("msg").getCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(Result.validationError("msg").getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(Result.unauthorized().getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(Result.unauthorized("msg").getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(Result.forbidden().getCode()).isEqualTo("FORBIDDEN");
        assertThat(Result.forbidden("msg").getCode()).isEqualTo("FORBIDDEN");
        assertThat(Result.notFound().getCode()).isEqualTo("NOT_FOUND");
        assertThat(Result.notFound("msg").getCode()).isEqualTo("NOT_FOUND");
    }

    /**
     * ResultCode 的 isSuccess/isError 语义正确
     */
    @Test
    void isSuccess_isError_semantics() {
        assertThat(ResultCode.SUCCESS.isSuccess()).isTrue();
        assertThat(ResultCode.SUCCESS.isError()).isFalse();

        for (ResultCode rc : ResultCode.values()) {
            if (rc != ResultCode.SUCCESS) {
                assertThat(rc.isSuccess()).as("%s should not be success", rc).isFalse();
                assertThat(rc.isError()).as("%s should be error", rc).isTrue();
            }
        }
    }
}
