package io.nebula.web.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注可进行响应缓存的接口方法（白名单 opt-in）。
 * <p>
 * 响应缓存改为默认关闭 + 仅缓存显式标注本注解的接口，避免此前"所有 GET 自动缓存 + 缓存键不含身份"
 * 导致的跨用户数据串号。请只在返回内容与用户身份无关的公共只读接口上使用。
 *
 * @author Nebula Framework
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseCacheable {
}
