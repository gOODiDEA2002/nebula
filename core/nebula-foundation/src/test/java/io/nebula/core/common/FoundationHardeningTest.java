package io.nebula.core.common;

import io.nebula.core.common.exception.ValidationException;
import io.nebula.core.common.security.CryptoUtils;
import io.nebula.core.common.security.JwtUtils;
import io.nebula.core.common.util.IdGenerator;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 覆盖 T-A3-7 修复的 Foundation 五个运行期缺陷。
 */
class FoundationHardeningTest {

    @Test
    void refreshTokenDoesNotThrowOnImmutableClaims() {
        // 修复前对 jjwt 0.12 不可变 Claims 调 remove() 会抛 UnsupportedOperationException
        SecretKey key = JwtUtils.generateKey();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "admin");
        String token = JwtUtils.generateToken("user-1", claims, key);

        String refreshed = JwtUtils.refreshToken(token, key, Duration.ofHours(1));

        assertThat(refreshed).isNotNull();
        JwtUtils.JwtParseResult parsed = JwtUtils.parseToken(refreshed, key);
        assertThat(parsed.isValid()).isTrue();
        assertThat(parsed.getSubject()).isEqualTo("user-1");
    }

    @Test
    void validationExceptionAllowsAddFieldErrorAfterSingleFieldConstructor() {
        // 修复前单字段构造器用 List.of(...)，addFieldError 会抛 UnsupportedOperationException
        assertThatCode(() ->
                new ValidationException("field1", "msg1", "v1")
                        .addFieldError("field2", "msg2", "v2"))
                .doesNotThrowAnyException();
    }

    @Test
    void sequenceGeneratorCyclesWithinRangeAtomically() {
        // 修复前 check-then-CAS 并发下可能重号/越界；修复后 getAndUpdate 原子回绕
        IdGenerator.SequenceGenerator seq = new IdGenerator.SequenceGenerator(0, 3);
        long[] values = new long[7];
        for (int i = 0; i < values.length; i++) {
            values[i] = seq.nextValue();
        }
        assertThat(values).containsExactly(0L, 1L, 2L, 0L, 1L, 2L, 0L);
    }

    @Test
    void defaultSnowflakeProducesUniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            ids.add(IdGenerator.snowflakeId());
        }
        assertThat(ids).hasSize(2000);
    }

    @Test
    void hashPasswordRoundTripsAndUsesRandomSalt() {
        String hash = CryptoUtils.hashPassword("s3cret");

        assertThat(hash).startsWith("pbkdf2$");
        assertThat(CryptoUtils.matchesPassword("s3cret", hash)).isTrue();
        assertThat(CryptoUtils.matchesPassword("wrong", hash)).isFalse();
        // 每次盐不同 -> 同一密码两次哈希不相等
        assertThat(hash).isNotEqualTo(CryptoUtils.hashPassword("s3cret"));
    }
}
