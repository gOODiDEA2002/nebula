package io.nebula.ai.rag.pipeline;

import io.nebula.ai.rag.retriever.RetrievalResult;

import java.util.List;

/**
 * 上下文拼接器
 * <p>
 * 把检索结果拼成带编号的文档列表，并控制总长度不超过预算。
 * 预算是按「加上这一篇会不会超」判断后整篇跳过，而不是截断到一半 ——
 * 半截文档喂给模型比少一篇更容易生成错误结论。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ContextAssembler {

    private final int maxLength;
    private final String documentTemplate;

    /**
     * @param maxLength        上下文最大字符数
     * @param documentTemplate 单篇模板，两个占位符依次为序号与内容
     */
    public ContextAssembler(int maxLength, String documentTemplate) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength 必须为正数");
        }
        if (documentTemplate == null || documentTemplate.isBlank()) {
            throw new IllegalArgumentException("documentTemplate 不能为空");
        }
        this.maxLength = maxLength;
        this.documentTemplate = documentTemplate;
    }

    /**
     * 拼接上下文
     *
     * @param results 已排序的检索结果
     * @return 拼接后的上下文文本；无结果时返回空串
     */
    public String assemble(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int currentLength = 0;
        for (int i = 0; i < results.size(); i++) {
            String docContent = String.format(documentTemplate, i + 1, results.get(i).getContent());
            if (currentLength + docContent.length() > maxLength) {
                break;
            }
            sb.append(docContent);
            currentLength += docContent.length();
        }
        return sb.toString();
    }

    public int getMaxLength() {
        return maxLength;
    }
}
