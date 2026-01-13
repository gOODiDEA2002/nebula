package io.nebula.crawler.core.parser;

import io.nebula.core.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON数据提取器
 * <p>
 * 使用简化的JsonPath语法从JSON中提取数据。
 * 支持的路径格式:
 * - $.field - 根级字段
 * - $.parent.child - 嵌套字段
 * - $.array[0] - 数组索引
 * - $.array[*] - 数组所有元素
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class JsonDataExtractor implements DataExtractor {

    private static final Pattern ARRAY_PATTERN = Pattern.compile("(.+)\\[(\\d+|\\*)]$");

    @Override
    public Optional<String> extract(String content, String path) {
        if (content == null || content.isEmpty() || path == null) {
            return Optional.empty();
        }

        try {
            Object value = extractValue(content, path);
            if (value != null) {
                return Optional.of(String.valueOf(value));
            }
        } catch (Exception e) {
            log.warn("JSON提取失败: path={}, error={}", path, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<String> extractAll(String content, String path) {
        List<String> results = new ArrayList<>();
        if (content == null || content.isEmpty() || path == null) {
            return results;
        }

        try {
            Object value = extractValue(content, path);
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    results.add(String.valueOf(item));
                }
            } else if (value != null) {
                results.add(String.valueOf(value));
            }
        } catch (Exception e) {
            log.warn("JSON批量提取失败: path={}, error={}", path, e.getMessage());
        }
        return results;
    }

    @Override
    public <T> Optional<T> extractAs(String content, String path, Class<T> type) {
        if (content == null || content.isEmpty() || path == null) {
            return Optional.empty();
        }

        try {
            Object value = extractValue(content, path);
            if (value != null) {
                return Optional.of(convertTo(value, type));
            }
        } catch (Exception e) {
            log.warn("JSON类型转换失败: path={}, type={}", path, type.getSimpleName());
        }
        return Optional.empty();
    }

    @Override
    public boolean exists(String content, String path) {
        if (content == null || content.isEmpty() || path == null) {
            return false;
        }

        try {
            Object value = extractValue(content, path);
            return value != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return "json";
    }

    /**
     * 提取JSON对象
     *
     * @param content JSON内容
     * @param path    JsonPath
     * @return 提取的子对象（作为JSON字符串）
     */
    public Optional<String> extractObject(String content, String path) {
        if (content == null || content.isEmpty() || path == null) {
            return Optional.empty();
        }

        try {
            Object value = extractValue(content, path);
            if (value instanceof Map || value instanceof List) {
                return Optional.of(JsonUtils.toJson(value));
            }
        } catch (Exception e) {
            log.warn("JSON对象提取失败: path={}", path);
        }
        return Optional.empty();
    }

    /**
     * 提取JSON数组
     *
     * @param content JSON内容
     * @param path    JsonPath（指向数组）
     * @param type    数组元素类型
     * @param <T>     类型参数
     * @return 元素列表
     */
    public <T> List<T> extractList(String content, String path, Class<T> type) {
        List<T> results = new ArrayList<>();
        if (content == null || content.isEmpty() || path == null) {
            return results;
        }

        try {
            Object value = extractValue(content, path);
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    results.add(convertTo(item, type));
                }
            }
        } catch (Exception e) {
            log.warn("JSON列表提取失败: path={}, type={}", path, type.getSimpleName());
        }
        return results;
    }

    /**
     * 核心提取逻辑
     */
    @SuppressWarnings("unchecked")
    private Object extractValue(String content, String path) {
        // 移除开头的 $.
        String normalizedPath = path.startsWith("$.") ? path.substring(2) : 
                               path.startsWith("$") ? path.substring(1) : path;

        if (normalizedPath.isEmpty()) {
            return JsonUtils.toMap(content);
        }

        Map<String, Object> json = JsonUtils.toMap(content);
        String[] parts = normalizedPath.split("\\.");
        Object current = json;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            // 检查是否有数组索引
            Matcher matcher = ARRAY_PATTERN.matcher(part);
            if (matcher.matches()) {
                String fieldName = matcher.group(1);
                String indexStr = matcher.group(2);

                // 先获取字段
                if (!fieldName.isEmpty() && current instanceof Map<?, ?> map) {
                    current = map.get(fieldName);
                }

                // 然后处理数组索引
                if (current instanceof List<?> list) {
                    if ("*".equals(indexStr)) {
                        // 返回所有元素
                        current = list;
                    } else {
                        int index = Integer.parseInt(indexStr);
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                // 普通字段访问
                if (current instanceof Map<?, ?> map) {
                    current = map.get(part);
                } else {
                    return null;
                }
            }
        }

        return current;
    }

    @SuppressWarnings("unchecked")
    private <T> T convertTo(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return (T) value;
        }

        String strValue = String.valueOf(value);

        if (type == String.class) {
            return (T) strValue;
        } else if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(strValue);
        } else if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(strValue);
        } else if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(strValue);
        } else if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.valueOf(strValue);
        }

        // 尝试JSON反序列化
        if (value instanceof Map || value instanceof List) {
            return JsonUtils.fromJson(JsonUtils.toJson(value), type);
        }

        throw new IllegalArgumentException("不支持的类型转换: " + type.getSimpleName());
    }
}

