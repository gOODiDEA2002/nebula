package io.nebula.crawler.core.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HTML数据提取器
 * <p>
 * 使用Jsoup CSS选择器从HTML中提取数据
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Slf4j
public class HtmlDataExtractor implements DataExtractor {

    @Override
    public Optional<String> extract(String content, String path) {
        if (content == null || content.isEmpty() || path == null) {
            return Optional.empty();
        }

        try {
            Document doc = Jsoup.parse(content);
            Element element = doc.selectFirst(path);
            if (element != null) {
                return Optional.of(element.text().trim());
            }
        } catch (Exception e) {
            log.warn("HTML提取失败: path={}, error={}", path, e.getMessage());
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
            Document doc = Jsoup.parse(content);
            Elements elements = doc.select(path);
            for (Element element : elements) {
                results.add(element.text().trim());
            }
        } catch (Exception e) {
            log.warn("HTML批量提取失败: path={}, error={}", path, e.getMessage());
        }
        return results;
    }

    @Override
    public <T> Optional<T> extractAs(String content, String path, Class<T> type) {
        Optional<String> value = extract(content, path);
        if (value.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(convertTo(value.get(), type));
        } catch (Exception e) {
            log.warn("类型转换失败: value={}, type={}", value.get(), type.getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String content, String path) {
        if (content == null || content.isEmpty() || path == null) {
            return false;
        }

        try {
            Document doc = Jsoup.parse(content);
            return doc.selectFirst(path) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return "html";
    }

    /**
     * 提取元素属性
     *
     * @param content   HTML内容
     * @param path      CSS选择器
     * @param attribute 属性名
     * @return 属性值
     */
    public Optional<String> extractAttribute(String content, String path, String attribute) {
        if (content == null || content.isEmpty() || path == null || attribute == null) {
            return Optional.empty();
        }

        try {
            Document doc = Jsoup.parse(content);
            Element element = doc.selectFirst(path);
            if (element != null && element.hasAttr(attribute)) {
                return Optional.of(element.attr(attribute));
            }
        } catch (Exception e) {
            log.warn("HTML属性提取失败: path={}, attr={}", path, attribute);
        }
        return Optional.empty();
    }

    /**
     * 提取元素HTML内容
     *
     * @param content HTML内容
     * @param path    CSS选择器
     * @return 元素的HTML
     */
    public Optional<String> extractHtml(String content, String path) {
        if (content == null || content.isEmpty() || path == null) {
            return Optional.empty();
        }

        try {
            Document doc = Jsoup.parse(content);
            Element element = doc.selectFirst(path);
            if (element != null) {
                return Optional.of(element.html());
            }
        } catch (Exception e) {
            log.warn("HTML内容提取失败: path={}", path);
        }
        return Optional.empty();
    }

    /**
     * 提取所有链接
     *
     * @param content HTML内容
     * @return 链接列表
     */
    public List<String> extractLinks(String content) {
        List<String> links = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return links;
        }

        try {
            Document doc = Jsoup.parse(content);
            Elements elements = doc.select("a[href]");
            for (Element element : elements) {
                String href = element.attr("href");
                if (href != null && !href.isEmpty()) {
                    links.add(href);
                }
            }
        } catch (Exception e) {
            log.warn("链接提取失败: {}", e.getMessage());
        }
        return links;
    }

    @SuppressWarnings("unchecked")
    private <T> T convertTo(String value, Class<T> type) {
        if (type == String.class) {
            return (T) value;
        } else if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(value.replaceAll("[^\\d-]", ""));
        } else if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(value.replaceAll("[^\\d-]", ""));
        } else if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(value.replaceAll("[^\\d.-]", ""));
        } else if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.valueOf(value);
        }
        throw new IllegalArgumentException("不支持的类型: " + type.getSimpleName());
    }
}

