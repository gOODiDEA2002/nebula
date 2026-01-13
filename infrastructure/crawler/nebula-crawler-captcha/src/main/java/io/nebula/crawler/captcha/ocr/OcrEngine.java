package io.nebula.crawler.captcha.ocr;

/**
 * OCR引擎接口
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface OcrEngine {

    /**
     * 识别图片中的文字
     *
     * @param imageData 图片字节数据
     * @return 识别结果
     * @throws Exception 识别异常
     */
    String recognize(byte[] imageData) throws Exception;

    /**
     * 获取引擎名称
     *
     * @return 引擎名称
     */
    String getName();

    /**
     * 检查引擎是否可用
     *
     * @return true如果可用
     */
    boolean isAvailable();
}
