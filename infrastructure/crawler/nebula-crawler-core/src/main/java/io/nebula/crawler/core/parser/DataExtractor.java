package io.nebula.crawler.core.parser;

import java.util.List;
import java.util.Optional;

/**
 * 数据提取器接口
 * <p>
 * 从内容中提取指定路径的数据
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface DataExtractor {

    /**
     * 提取单个值
     *
     * @param content 内容（HTML或JSON字符串）
     * @param path    提取路径（CSS选择器或JsonPath）
     * @return 提取的值
     */
    Optional<String> extract(String content, String path);

    /**
     * 提取多个值
     *
     * @param content 内容
     * @param path    提取路径
     * @return 提取的值列表
     */
    List<String> extractAll(String content, String path);

    /**
     * 提取并转换为指定类型
     *
     * @param content 内容
     * @param path    提取路径
     * @param type    目标类型
     * @param <T>     类型参数
     * @return 转换后的值
     */
    <T> Optional<T> extractAs(String content, String path, Class<T> type);

    /**
     * 检查路径是否存在
     *
     * @param content 内容
     * @param path    检查路径
     * @return true表示存在
     */
    boolean exists(String content, String path);

    /**
     * 获取提取器类型
     *
     * @return 类型标识（如 "html", "json"）
     */
    String getType();
}

