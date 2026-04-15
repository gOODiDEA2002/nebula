package io.nebula.rpc.core.annotation;

import java.lang.annotation.*;

/**
 * 远程服务客户端注解
 * 用于标记 RPC 客户端接口，支持声明式服务调用。
 * <p>
 * 替代 {@link RpcClient}，避免与 {@link io.nebula.rpc.core.client.RpcClient} 接口命名冲突。
 * </p>
 *
 * @author Nebula Framework
 * @since 2.0.1
 * @see RpcClient
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemoteService {

    String value() default "";

    String name() default "";

    String url() default "";

    String contextId() default "";

    Class<?>[] configuration() default {};

    Class<?> fallback() default void.class;

    Class<?> fallbackFactory() default void.class;

    int connectTimeout() default 30000;

    int readTimeout() default 60000;
}
