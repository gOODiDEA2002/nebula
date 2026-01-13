# Nebula Crawler Captcha 模块设计文档

## 文档信息
- 模块名称：nebula-crawler-captcha
- 版本：2.0.1-SNAPSHOT
- 创建日期：2026-01-06
- 所属项目：Nebula Framework

---

## 1. 模块概述

### 1.1 模块定位

`nebula-crawler-captcha` 是 Nebula 爬虫模块组的验证码识别模块，提供统一的验证码检测、识别和破解能力，支持多种验证码类型和多种识别策略。

### 1.2 核心能力

| 能力 | 说明 |
|------|------|
| **类型检测** | 自动检测验证码类型 |
| **多类型支持** | 图形、滑块、点击、手势、旋转、短信、reCAPTCHA |
| **多策略识别** | 本地OCR + 第三方打码平台 |
| **统一接口** | 抽象接口，便于扩展 |

### 1.3 与其他爬虫模块的关系

```
nebula-crawler-captcha
        │
        ├── 依赖 nebula-crawler-core (CrawlerResponse等核心类)
        │
        └── 被 nebula-crawler-browser 调用 (浏览器爬虫遇到验证码时)
```

---

## 2. 支持的验证码类型

| 验证码类型 | 枚举值 | 说明 | 成功率参考 |
|-----------|--------|------|-----------|
| **图形验证码** | `IMAGE` | 字符识别（数字、字母、算术） | 95%+ |
| **滑块验证码** | `SLIDER` | 拖动滑块至缺口位置 | 90%+ |
| **点击验证码** | `CLICK` | 点选特定文字/图片 | 85%+ |
| **手势验证码** | `GESTURE` | 九宫格轨迹绘制 | 80%+ |
| **旋转验证码** | `ROTATE` | 旋转图片至正确角度 | 75%+ |
| **短信验证码** | `SMS` | 手机短信验证 | 依赖接码平台 |
| **reCAPTCHA** | `RECAPTCHA` | Google reCAPTCHA | 90%+ |
| **hCaptcha** | `HCAPTCHA` | hCaptcha验证 | 90%+ |

---

## 3. 模块结构

```
nebula-crawler-captcha/
├── pom.xml
├── README.md
├── DESIGN.md                               # 本文档
└── src/main/java/io/nebula/crawler/captcha/
    ├── CaptchaSolver.java                  # 验证码解决器接口
    ├── CaptchaType.java                    # 验证码类型枚举
    ├── CaptchaRequest.java                 # 验证码请求
    ├── CaptchaResult.java                  # 验证码识别结果
    ├── CaptchaManager.java                 # 验证码管理器
    ├── exception/
    │   └── CaptchaException.java           # 验证码异常
    ├── detector/
    │   ├── CaptchaDetector.java            # 验证码检测器接口
    │   └── DefaultCaptchaDetector.java     # 默认检测器实现
    ├── solver/
    │   ├── ImageCaptchaSolver.java         # 图形验证码解决器
    │   ├── SliderCaptchaSolver.java        # 滑块验证码解决器
    │   ├── ClickCaptchaSolver.java         # 点击验证码解决器
    │   ├── GestureCaptchaSolver.java       # 手势验证码解决器
    │   ├── RotateCaptchaSolver.java        # 旋转验证码解决器
    │   ├── SmsCaptchaSolver.java           # 短信验证码解决器
    │   └── RecaptchaSolver.java            # reCAPTCHA解决器
    ├── ocr/
    │   ├── OcrEngine.java                  # OCR引擎接口
    │   ├── TesseractOcrEngine.java         # Tesseract OCR实现
    │   └── DdddOcrEngine.java              # ddddocr实现（调用Python服务）
    ├── provider/
    │   ├── CaptchaServiceProvider.java     # 第三方打码平台接口
    │   ├── TwoCaptchaProvider.java         # 2Captcha平台
    │   ├── AntiCaptchaProvider.java        # Anti-Captcha平台
    │   └── YunmaProvider.java              # 云码平台
    └── config/
        └── CaptchaProperties.java          # 验证码配置属性
```

---

## 4. 核心接口设计

### 4.1 CaptchaType（验证码类型枚举）

```java
package io.nebula.crawler.captcha;

/**
 * 验证码类型枚举
 */
public enum CaptchaType {
    
    IMAGE("image", "图形验证码"),
    SLIDER("slider", "滑块验证码"),
    CLICK("click", "点击验证码"),
    GESTURE("gesture", "手势验证码"),
    ROTATE("rotate", "旋转验证码"),
    SMS("sms", "短信验证码"),
    RECAPTCHA("recaptcha", "Google reCAPTCHA"),
    HCAPTCHA("hcaptcha", "hCaptcha"),
    UNKNOWN("unknown", "未知验证码");
    
    private final String code;
    private final String description;
    
    CaptchaType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
}
```

### 4.2 CaptchaRequest（验证码请求）

```java
package io.nebula.crawler.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 验证码请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaRequest {
    
    /** 任务ID（用于追踪） */
    private String taskId;
    
    /** 验证码类型 */
    private CaptchaType type;
    
    /** 验证码图片（Base64编码） */
    private String imageBase64;
    
    /** 验证码图片URL */
    private String imageUrl;
    
    /** 目标网站URL（用于reCAPTCHA等） */
    private String siteUrl;
    
    /** 站点密钥（用于reCAPTCHA等） */
    private String siteKey;
    
    /** 滑块验证码的背景图（Base64） */
    private String backgroundImage;
    
    /** 滑块验证码的滑块图（Base64） */
    private String sliderImage;
    
    /** 手势验证码的轨迹提示 */
    private String gestureHint;
    
    /** 超时时间（毫秒） */
    @Builder.Default
    private int timeout = 60000;
    
    /** 扩展参数 */
    private Map<String, Object> extras;
    
    /**
     * 创建图形验证码请求
     */
    public static CaptchaRequest image(String imageBase64) {
        return CaptchaRequest.builder()
            .type(CaptchaType.IMAGE)
            .imageBase64(imageBase64)
            .build();
    }
    
    /**
     * 创建滑块验证码请求
     */
    public static CaptchaRequest slider(String backgroundImage, String sliderImage) {
        return CaptchaRequest.builder()
            .type(CaptchaType.SLIDER)
            .backgroundImage(backgroundImage)
            .sliderImage(sliderImage)
            .build();
    }
    
    /**
     * 创建reCAPTCHA请求
     */
    public static CaptchaRequest recaptcha(String siteUrl, String siteKey) {
        return CaptchaRequest.builder()
            .type(CaptchaType.RECAPTCHA)
            .siteUrl(siteUrl)
            .siteKey(siteKey)
            .build();
    }
}
```

### 4.3 CaptchaResult（验证码识别结果）

```java
package io.nebula.crawler.captcha;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 验证码识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResult {
    
    /** 任务ID */
    private String taskId;
    
    /** 是否成功 */
    private boolean success;
    
    /** 验证码类型 */
    private CaptchaType type;
    
    /** 识别结果文本（图形验证码） */
    private String text;
    
    /** 滑块偏移量（滑块验证码） */
    private Integer sliderOffset;
    
    /** 点击坐标列表（点击验证码）- [x, y] */
    private List<int[]> clickPoints;
    
    /** 手势轨迹点（手势验证码）- [x, y] */
    private List<int[]> gestureTrack;
    
    /** 旋转角度（旋转验证码） */
    private Integer rotateAngle;
    
    /** reCAPTCHA Token */
    private String recaptchaToken;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 识别耗时（毫秒） */
    private long costTime;
    
    /** 置信度（0-1） */
    private double confidence;
    
    /** 使用的识别方式 */
    private String solverName;
    
    /**
     * 创建成功结果
     */
    public static CaptchaResult success(CaptchaType type) {
        return CaptchaResult.builder()
            .success(true)
            .type(type)
            .build();
    }
    
    /**
     * 创建失败结果
     */
    public static CaptchaResult fail(String errorMessage) {
        return CaptchaResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
```

### 4.4 CaptchaSolver（验证码解决器接口）

```java
package io.nebula.crawler.captcha;

import java.util.concurrent.CompletableFuture;

/**
 * 验证码解决器接口
 * 定义验证码识别的通用能力
 */
public interface CaptchaSolver {
    
    /**
     * 获取解决器名称
     */
    String getName();
    
    /**
     * 获取支持的验证码类型
     */
    CaptchaType getSupportedType();
    
    /**
     * 解决验证码
     * @param request 验证码请求
     * @return 识别结果
     * @throws CaptchaException 识别异常
     */
    CaptchaResult solve(CaptchaRequest request) throws CaptchaException;
    
    /**
     * 异步解决验证码
     */
    default CompletableFuture<CaptchaResult> solveAsync(CaptchaRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return solve(request);
            } catch (CaptchaException e) {
                return CaptchaResult.fail(e.getMessage());
            }
        });
    }
    
    /**
     * 报告识别结果（用于反馈优化）
     */
    default void reportResult(String taskId, boolean success) {
        // 默认空实现
    }
    
    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取优先级（数字越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }
}
```

### 4.5 CaptchaManager（验证码管理器）

```java
package io.nebula.crawler.captcha;

import io.nebula.crawler.captcha.detector.CaptchaDetector;
import io.nebula.crawler.captcha.exception.CaptchaException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 验证码管理器
 * 统一管理不同类型的验证码解决器
 */
@Slf4j
public class CaptchaManager {
    
    private final Map<CaptchaType, List<CaptchaSolver>> solvers;
    private final CaptchaDetector detector;
    
    public CaptchaManager(List<CaptchaSolver> solverList, CaptchaDetector detector) {
        this.detector = detector;
        this.solvers = solverList.stream()
            .collect(Collectors.groupingBy(
                CaptchaSolver::getSupportedType,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> {
                        list.sort(Comparator.comparingInt(CaptchaSolver::getPriority));
                        return list;
                    }
                )
            ));
    }
    
    /**
     * 解决验证码
     */
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        CaptchaType type = request.getType();
        
        // 如果未指定类型，尝试自动检测
        if (type == null || type == CaptchaType.UNKNOWN) {
            type = detector.detect(request.getImageBase64());
            request.setType(type);
            log.info("自动检测验证码类型: {}", type);
        }
        
        List<CaptchaSolver> typeSolvers = solvers.get(type);
        if (typeSolvers == null || typeSolvers.isEmpty()) {
            throw new CaptchaException("不支持的验证码类型: " + type);
        }
        
        // 按优先级尝试各个解决器
        CaptchaException lastException = null;
        for (CaptchaSolver solver : typeSolvers) {
            if (!solver.isAvailable()) {
                log.debug("解决器 {} 不可用，跳过", solver.getName());
                continue;
            }
            
            try {
                log.info("尝试使用 {} 识别验证码", solver.getName());
                CaptchaResult result = solver.solve(request);
                if (result.isSuccess()) {
                    result.setSolverName(solver.getName());
                    return result;
                }
                log.warn("解决器 {} 识别失败: {}", solver.getName(), result.getErrorMessage());
            } catch (CaptchaException e) {
                log.warn("解决器 {} 抛出异常: {}", solver.getName(), e.getMessage());
                lastException = e;
            }
        }
        
        if (lastException != null) {
            throw lastException;
        }
        throw new CaptchaException("所有解决器均无法识别验证码");
    }
    
    /**
     * 异步解决验证码
     */
    public CompletableFuture<CaptchaResult> solveAsync(CaptchaRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return solve(request);
            } catch (CaptchaException e) {
                return CaptchaResult.fail(e.getMessage());
            }
        });
    }
    
    /**
     * 报告识别结果
     */
    public void reportResult(CaptchaType type, String taskId, boolean success) {
        List<CaptchaSolver> typeSolvers = solvers.get(type);
        if (typeSolvers != null) {
            typeSolvers.forEach(s -> s.reportResult(taskId, success));
        }
    }
    
    /**
     * 检查指定类型的解决器是否可用
     */
    public boolean isAvailable(CaptchaType type) {
        List<CaptchaSolver> typeSolvers = solvers.get(type);
        return typeSolvers != null && typeSolvers.stream().anyMatch(CaptchaSolver::isAvailable);
    }
    
    /**
     * 获取所有支持的验证码类型
     */
    public Set<CaptchaType> getSupportedTypes() {
        return solvers.keySet();
    }
}
```

---

## 5. 解决器实现

### 5.1 图形验证码解决器

```java
package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.config.CaptchaProperties;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.ocr.OcrEngine;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;

/**
 * 图形验证码解决器
 * 支持本地OCR和第三方打码平台
 */
@Slf4j
public class ImageCaptchaSolver implements CaptchaSolver {
    
    private final CaptchaProperties properties;
    private final OcrEngine ocrEngine;
    private final List<CaptchaServiceProvider> providers;
    
    public ImageCaptchaSolver(CaptchaProperties properties,
                             OcrEngine ocrEngine,
                             List<CaptchaServiceProvider> providers) {
        this.properties = properties;
        this.ocrEngine = ocrEngine;
        this.providers = providers;
    }
    
    @Override
    public String getName() {
        return "ImageCaptchaSolver";
    }
    
    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.IMAGE;
    }
    
    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();
        
        try {
            byte[] imageData = getImageData(request);
            
            // 1. 优先使用本地OCR
            if (properties.isLocalOcrEnabled() && ocrEngine != null) {
                try {
                    String text = ocrEngine.recognize(imageData);
                    if (isValidResult(text)) {
                        return CaptchaResult.builder()
                            .success(true)
                            .type(CaptchaType.IMAGE)
                            .text(text)
                            .costTime(System.currentTimeMillis() - startTime)
                            .confidence(0.8)
                            .solverName("LocalOCR")
                            .build();
                    }
                } catch (Exception e) {
                    log.warn("本地OCR识别失败: {}", e.getMessage());
                }
            }
            
            // 2. 使用第三方打码平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.isAvailable()) continue;
                
                try {
                    CaptchaResult result = provider.solveImage(imageData, request.getTimeout());
                    if (result.isSuccess()) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName(provider.getName());
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别失败: {}", provider.getName(), e.getMessage());
                }
            }
            
            throw new CaptchaException("所有识别方式均失败");
            
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("验证码识别异常", e);
        }
    }
    
    private byte[] getImageData(CaptchaRequest request) throws CaptchaException {
        if (request.getImageBase64() != null) {
            return Base64.getDecoder().decode(request.getImageBase64());
        }
        if (request.getImageUrl() != null) {
            // 下载图片
            return downloadImage(request.getImageUrl());
        }
        throw new CaptchaException("未提供验证码图片");
    }
    
    private byte[] downloadImage(String url) throws CaptchaException {
        // 使用OkHttp下载图片
        // 实现略
        return new byte[0];
    }
    
    private boolean isValidResult(String text) {
        return text != null && 
               text.length() >= properties.getMinLength() && 
               text.length() <= properties.getMaxLength();
    }
    
    @Override
    public boolean isAvailable() {
        return (ocrEngine != null && properties.isLocalOcrEnabled()) || 
               providers.stream().anyMatch(CaptchaServiceProvider::isAvailable);
    }
    
    @Override
    public int getPriority() {
        return 10;
    }
}
```

### 5.2 滑块验证码解决器

```java
package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.config.CaptchaProperties;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 滑块验证码解决器
 * 支持缺口检测和第三方识别
 */
@Slf4j
public class SliderCaptchaSolver implements CaptchaSolver {
    
    private final CaptchaProperties properties;
    private final List<CaptchaServiceProvider> providers;
    
    @Override
    public String getName() {
        return "SliderCaptchaSolver";
    }
    
    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.SLIDER;
    }
    
    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 本地缺口检测（基于OpenCV）
            if (properties.isLocalSliderEnabled()) {
                Integer offset = detectGapOffset(
                    request.getBackgroundImage(),
                    request.getSliderImage()
                );
                
                if (offset != null) {
                    return CaptchaResult.builder()
                        .success(true)
                        .type(CaptchaType.SLIDER)
                        .sliderOffset(offset)
                        .costTime(System.currentTimeMillis() - startTime)
                        .solverName("LocalGapDetection")
                        .build();
                }
            }
            
            // 2. 使用第三方平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.supportsType(CaptchaType.SLIDER)) continue;
                if (!provider.isAvailable()) continue;
                
                try {
                    CaptchaResult result = provider.solveSlider(
                        request.getBackgroundImage(),
                        request.getSliderImage(),
                        request.getTimeout()
                    );
                    if (result.isSuccess()) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName(provider.getName());
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别滑块失败: {}", provider.getName(), e.getMessage());
                }
            }
            
            throw new CaptchaException("滑块验证码识别失败");
            
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("滑块验证码识别异常", e);
        }
    }
    
    /**
     * 使用模板匹配算法检测缺口位置
     * 需要引入 OpenCV 依赖
     */
    private Integer detectGapOffset(String backgroundBase64, String sliderBase64) {
        // 基于OpenCV的模板匹配实现
        // 1. 解码图片
        // 2. 边缘检测
        // 3. 模板匹配
        // 4. 返回最佳匹配位置的X坐标
        return null; // 需要OpenCV实现
    }
    
    @Override
    public boolean isAvailable() {
        return properties.isLocalSliderEnabled() || 
               providers.stream().anyMatch(p -> p.supportsType(CaptchaType.SLIDER));
    }
    
    @Override
    public int getPriority() {
        return 20;
    }
}
```

### 5.3 手势验证码解决器

```java
package io.nebula.crawler.captcha.solver;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.exception.CaptchaException;
import io.nebula.crawler.captcha.provider.CaptchaServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 手势验证码解决器
 * 处理九宫格轨迹验证码
 */
@Slf4j
public class GestureCaptchaSolver implements CaptchaSolver {
    
    private final List<CaptchaServiceProvider> providers;
    
    // 九宫格坐标映射（以3x3为例，实际坐标根据图片大小调整）
    private static final int[][] GRID_POINTS = {
        {50, 50},   {150, 50},  {250, 50},   // 0, 1, 2
        {50, 150},  {150, 150}, {250, 150},  // 3, 4, 5
        {50, 250},  {150, 250}, {250, 250}   // 6, 7, 8
    };
    
    // 常见图案映射
    private static final Map<String, int[]> PATTERNS = Map.of(
        "Z", new int[]{0, 1, 2, 4, 6, 7, 8},
        "N", new int[]{6, 3, 0, 4, 8, 5, 2},
        "L", new int[]{0, 3, 6, 7, 8},
        "7", new int[]{0, 1, 2, 4, 6},
        "M", new int[]{6, 3, 0, 4, 2, 5, 8},
        "S", new int[]{2, 1, 0, 4, 8, 7, 6},
        "C", new int[]{2, 1, 0, 3, 6, 7, 8}
    );
    
    @Override
    public String getName() {
        return "GestureCaptchaSolver";
    }
    
    @Override
    public CaptchaType getSupportedType() {
        return CaptchaType.GESTURE;
    }
    
    @Override
    public CaptchaResult solve(CaptchaRequest request) throws CaptchaException {
        long startTime = System.currentTimeMillis();
        
        try {
            String hint = request.getGestureHint();
            
            // 1. 根据提示匹配已知图案
            List<int[]> track = recognizePattern(hint);
            if (track != null && !track.isEmpty()) {
                return CaptchaResult.builder()
                    .success(true)
                    .type(CaptchaType.GESTURE)
                    .gestureTrack(track)
                    .costTime(System.currentTimeMillis() - startTime)
                    .solverName("PatternMatch")
                    .build();
            }
            
            // 2. 使用第三方平台
            for (CaptchaServiceProvider provider : providers) {
                if (!provider.supportsType(CaptchaType.GESTURE)) continue;
                if (!provider.isAvailable()) continue;
                
                try {
                    CaptchaResult result = provider.solveGesture(
                        request.getImageBase64(),
                        hint,
                        request.getTimeout()
                    );
                    if (result.isSuccess()) {
                        result.setCostTime(System.currentTimeMillis() - startTime);
                        result.setSolverName(provider.getName());
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("打码平台 {} 识别手势失败: {}", provider.getName(), e.getMessage());
                }
            }
            
            throw new CaptchaException("手势验证码识别失败");
            
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("手势验证码识别异常", e);
        }
    }
    
    /**
     * 根据提示匹配图案并生成轨迹
     */
    private List<int[]> recognizePattern(String hint) {
        if (hint == null) return null;
        
        for (Map.Entry<String, int[]> entry : PATTERNS.entrySet()) {
            if (hint.toUpperCase().contains(entry.getKey())) {
                return Arrays.stream(entry.getValue())
                    .mapToObj(i -> GRID_POINTS[i])
                    .collect(Collectors.toList());
            }
        }
        return null;
    }
    
    @Override
    public boolean isAvailable() {
        return true; // 图案匹配始终可用
    }
    
    @Override
    public int getPriority() {
        return 30;
    }
}
```

---

## 6. OCR引擎实现

### 6.1 OcrEngine接口

```java
package io.nebula.crawler.captcha.ocr;

/**
 * OCR引擎接口
 */
public interface OcrEngine {
    
    /**
     * 识别图片中的文字
     * @param imageData 图片字节数据
     * @return 识别结果
     */
    String recognize(byte[] imageData) throws Exception;
    
    /**
     * 获取引擎名称
     */
    String getName();
    
    /**
     * 检查引擎是否可用
     */
    boolean isAvailable();
}
```

### 6.2 DdddOcrEngine（推荐）

```java
package io.nebula.crawler.captcha.ocr;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * ddddocr OCR引擎
 * 调用Python ddddocr服务
 * 
 * Python服务启动命令:
 * pip install ddddocr
 * python -m ddddocr server -p 8866
 */
@Slf4j
public class DdddOcrEngine implements OcrEngine {
    
    private final String serverUrl;
    private final OkHttpClient httpClient;
    
    public DdddOcrEngine(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    @Override
    public String recognize(byte[] imageData) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(imageData);
        
        RequestBody body = new FormBody.Builder()
            .add("image", base64)
            .build();
        
        Request request = new Request.Builder()
            .url(serverUrl + "/ocr/b64/text")
            .post(body)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("OCR服务响应错误: " + response.code());
            }
            
            String result = response.body().string();
            log.debug("ddddocr识别结果: {}", result);
            return result.trim();
        }
    }
    
    @Override
    public String getName() {
        return "ddddocr";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                .url(serverUrl + "/ping")
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## 7. 第三方打码平台接口

### 7.1 CaptchaServiceProvider接口

```java
package io.nebula.crawler.captcha.provider;

import io.nebula.crawler.captcha.CaptchaResult;
import io.nebula.crawler.captcha.CaptchaType;
import io.nebula.crawler.captcha.exception.CaptchaException;

/**
 * 第三方打码平台接口
 */
public interface CaptchaServiceProvider {
    
    /** 获取平台名称 */
    String getName();
    
    /** 是否支持指定验证码类型 */
    boolean supportsType(CaptchaType type);
    
    /** 识别图形验证码 */
    CaptchaResult solveImage(byte[] imageData, int timeout) throws CaptchaException;
    
    /** 识别滑块验证码 */
    CaptchaResult solveSlider(String backgroundBase64, String sliderBase64, int timeout) throws CaptchaException;
    
    /** 识别点击验证码 */
    CaptchaResult solveClick(String imageBase64, String hint, int timeout) throws CaptchaException;
    
    /** 识别手势验证码 */
    CaptchaResult solveGesture(String imageBase64, String hint, int timeout) throws CaptchaException;
    
    /** 识别reCAPTCHA */
    CaptchaResult solveRecaptcha(String siteUrl, String siteKey, int timeout) throws CaptchaException;
    
    /** 报告识别结果 */
    void reportResult(String taskId, boolean success);
    
    /** 检查服务是否可用 */
    boolean isAvailable();
    
    /** 获取账户余额 */
    double getBalance();
}
```

### 7.2 TwoCaptchaProvider实现

```java
package io.nebula.crawler.captcha.provider;

import io.nebula.crawler.captcha.CaptchaResult;
import io.nebula.crawler.captcha.CaptchaType;
import io.nebula.crawler.captcha.exception.CaptchaException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 2Captcha平台实现
 * API文档: https://2captcha.com/api-docs
 */
@Slf4j
public class TwoCaptchaProvider implements CaptchaServiceProvider {
    
    private static final String API_BASE = "https://2captcha.com";
    
    private final String apiKey;
    private final OkHttpClient httpClient;
    
    public TwoCaptchaProvider(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
    }
    
    @Override
    public String getName() {
        return "2Captcha";
    }
    
    @Override
    public boolean supportsType(CaptchaType type) {
        return type == CaptchaType.IMAGE || 
               type == CaptchaType.RECAPTCHA || 
               type == CaptchaType.SLIDER ||
               type == CaptchaType.CLICK ||
               type == CaptchaType.HCAPTCHA;
    }
    
    @Override
    public CaptchaResult solveImage(byte[] imageData, int timeout) throws CaptchaException {
        try {
            // 1. 提交验证码
            String base64 = Base64.getEncoder().encodeToString(imageData);
            
            FormBody submitBody = new FormBody.Builder()
                .add("key", apiKey)
                .add("method", "base64")
                .add("body", base64)
                .add("json", "1")
                .build();
            
            Request submitRequest = new Request.Builder()
                .url(API_BASE + "/in.php")
                .post(submitBody)
                .build();
            
            String taskId = submitTask(submitRequest);
            
            // 2. 轮询结果
            return pollResult(taskId, timeout, CaptchaType.IMAGE);
            
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("2Captcha调用异常", e);
        }
    }
    
    @Override
    public CaptchaResult solveRecaptcha(String siteUrl, String siteKey, int timeout) throws CaptchaException {
        try {
            FormBody submitBody = new FormBody.Builder()
                .add("key", apiKey)
                .add("method", "userrecaptcha")
                .add("googlekey", siteKey)
                .add("pageurl", siteUrl)
                .add("json", "1")
                .build();
            
            Request submitRequest = new Request.Builder()
                .url(API_BASE + "/in.php")
                .post(submitBody)
                .build();
            
            String taskId = submitTask(submitRequest);
            CaptchaResult result = pollResult(taskId, timeout, CaptchaType.RECAPTCHA);
            result.setRecaptchaToken(result.getText());
            return result;
            
        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            throw new CaptchaException("2Captcha reCAPTCHA识别异常", e);
        }
    }
    
    private String submitTask(Request request) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body().string();
            // 解析JSON获取taskId
            // { "status": 1, "request": "taskId" }
            if (body.contains("\"status\":1")) {
                return body.replaceAll(".*\"request\":\"([^\"]+)\".*", "$1");
            }
            throw new CaptchaException("提交失败: " + body);
        }
    }
    
    private CaptchaResult pollResult(String taskId, int timeout, CaptchaType type) throws Exception {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(5000);
            
            Request request = new Request.Builder()
                .url(API_BASE + "/res.php?key=" + apiKey + "&action=get&id=" + taskId + "&json=1")
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String body = response.body().string();
                
                if (body.contains("CAPCHA_NOT_READY")) {
                    continue;
                }
                
                if (body.contains("\"status\":1")) {
                    String text = body.replaceAll(".*\"request\":\"([^\"]+)\".*", "$1");
                    return CaptchaResult.builder()
                        .success(true)
                        .type(type)
                        .text(text)
                        .build();
                }
                
                throw new CaptchaException("识别失败: " + body);
            }
        }
        
        throw new CaptchaException("识别超时");
    }
    
    @Override
    public void reportResult(String taskId, boolean success) {
        try {
            String action = success ? "reportgood" : "reportbad";
            Request request = new Request.Builder()
                .url(API_BASE + "/res.php?key=" + apiKey + "&action=" + action + "&id=" + taskId)
                .get()
                .build();
            httpClient.newCall(request).execute().close();
        } catch (Exception e) {
            log.warn("报告结果失败", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return getBalance() > 0;
    }
    
    @Override
    public double getBalance() {
        try {
            Request request = new Request.Builder()
                .url(API_BASE + "/res.php?key=" + apiKey + "&action=getbalance")
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return Double.parseDouble(response.body().string());
            }
        } catch (Exception e) {
            log.error("获取余额失败", e);
            return 0;
        }
    }
    
    // 其他方法实现...
    @Override
    public CaptchaResult solveSlider(String bg, String slider, int timeout) throws CaptchaException {
        throw new CaptchaException("2Captcha滑块识别暂未实现");
    }
    
    @Override
    public CaptchaResult solveClick(String image, String hint, int timeout) throws CaptchaException {
        throw new CaptchaException("2Captcha点击识别暂未实现");
    }
    
    @Override
    public CaptchaResult solveGesture(String image, String hint, int timeout) throws CaptchaException {
        throw new CaptchaException("2Captcha手势识别暂未实现");
    }
}
```

---

## 8. 配置属性

```java
package io.nebula.crawler.captcha.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 验证码配置属性
 */
@Data
@ConfigurationProperties(prefix = "nebula.crawler.captcha")
public class CaptchaProperties {
    
    /** 是否启用验证码模块 */
    private boolean enabled = true;
    
    /** 是否启用本地OCR */
    private boolean localOcrEnabled = true;
    
    /** 本地OCR引擎类型（tesseract/ddddocr） */
    private String ocrEngine = "ddddocr";
    
    /** ddddocr服务地址 */
    private String ddddocrUrl = "http://localhost:8866";
    
    /** 是否启用本地滑块检测 */
    private boolean localSliderEnabled = true;
    
    /** 验证码最小长度 */
    private int minLength = 4;
    
    /** 验证码最大长度 */
    private int maxLength = 6;
    
    /** 默认超时时间（毫秒） */
    private int defaultTimeout = 60000;
    
    /** 第三方平台配置 */
    private List<ProviderConfig> providers;
    
    @Data
    public static class ProviderConfig {
        /** 平台名称 */
        private String name;
        /** API Key */
        private String apiKey;
        /** 是否启用 */
        private boolean enabled = true;
        /** 优先级（数字越小优先级越高） */
        private int priority = 1;
    }
}
```

---

## 9. 自动配置

在 `nebula-autoconfigure` 模块中添加：

```java
package io.nebula.autoconfigure.crawler;

import io.nebula.crawler.captcha.*;
import io.nebula.crawler.captcha.config.CaptchaProperties;
import io.nebula.crawler.captcha.detector.*;
import io.nebula.crawler.captcha.ocr.*;
import io.nebula.crawler.captcha.provider.*;
import io.nebula.crawler.captcha.solver.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

import java.util.*;

@Configuration
@ConditionalOnClass(CaptchaSolver.class)
@ConditionalOnProperty(prefix = "nebula.crawler.captcha", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nebula.crawler.captcha", name = "ocr-engine", havingValue = "ddddocr", matchIfMissing = true)
    public OcrEngine ddddOcrEngine(CaptchaProperties properties) {
        return new DdddOcrEngine(properties.getDdddocrUrl());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public List<CaptchaServiceProvider> captchaServiceProviders(CaptchaProperties properties) {
        List<CaptchaServiceProvider> providers = new ArrayList<>();
        
        if (properties.getProviders() != null) {
            for (var config : properties.getProviders()) {
                if (!config.isEnabled()) continue;
                
                switch (config.getName().toLowerCase()) {
                    case "2captcha":
                        providers.add(new TwoCaptchaProvider(config.getApiKey()));
                        break;
                    case "anti-captcha":
                        providers.add(new AntiCaptchaProvider(config.getApiKey()));
                        break;
                }
            }
        }
        
        return providers;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CaptchaDetector captchaDetector() {
        return new DefaultCaptchaDetector();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ImageCaptchaSolver imageCaptchaSolver(
            CaptchaProperties properties,
            Optional<OcrEngine> ocrEngine,
            List<CaptchaServiceProvider> providers) {
        return new ImageCaptchaSolver(properties, ocrEngine.orElse(null), providers);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SliderCaptchaSolver sliderCaptchaSolver(
            CaptchaProperties properties,
            List<CaptchaServiceProvider> providers) {
        return new SliderCaptchaSolver(properties, providers);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public GestureCaptchaSolver gestureCaptchaSolver(List<CaptchaServiceProvider> providers) {
        return new GestureCaptchaSolver(providers);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CaptchaManager captchaManager(
            List<CaptchaSolver> solvers,
            CaptchaDetector detector) {
        return new CaptchaManager(solvers, detector);
    }
}
```

---

## 10. 使用示例

### 10.1 配置

```yaml
nebula:
  crawler:
    captcha:
      enabled: true
      local-ocr-enabled: true
      ocr-engine: ddddocr
      ddddocr-url: http://localhost:8866
      local-slider-enabled: true
      min-length: 4
      max-length: 6
      default-timeout: 60000
      providers:
        - name: 2captcha
          api-key: ${CAPTCHA_2CAPTCHA_KEY}
          enabled: true
          priority: 1
```

### 10.2 代码使用

```java
@Service
@RequiredArgsConstructor
public class CrawlerService {
    
    private final CaptchaManager captchaManager;
    
    public void crawlWithCaptcha(String url) {
        // ... 爬取过程中遇到验证码
        
        // 图形验证码
        CaptchaRequest request = CaptchaRequest.image(imageBase64);
        CaptchaResult result = captchaManager.solve(request);
        if (result.isSuccess()) {
            String text = result.getText();
            // 使用验证码结果继续爬取
        }
        
        // 滑块验证码
        CaptchaRequest sliderRequest = CaptchaRequest.slider(bgImage, sliderImage);
        CaptchaResult sliderResult = captchaManager.solve(sliderRequest);
        if (sliderResult.isSuccess()) {
            int offset = sliderResult.getSliderOffset();
            // 模拟滑动
        }
        
        // reCAPTCHA
        CaptchaRequest recaptchaRequest = CaptchaRequest.recaptcha(siteUrl, siteKey);
        CaptchaResult recaptchaResult = captchaManager.solve(recaptchaRequest);
        if (recaptchaResult.isSuccess()) {
            String token = recaptchaResult.getRecaptchaToken();
            // 使用token
        }
    }
}
```

---

## 11. 第三方平台对比

| 平台 | 支持类型 | 单价参考 | 速度 | 准确率 | 官网 |
|------|---------|---------|------|--------|------|
| 2Captcha | 图形/reCAPTCHA/滑块/hCaptcha | $2.99/1000次 | 10-60秒 | 95%+ | https://2captcha.com |
| Anti-Captcha | 图形/reCAPTCHA/滑块 | $2.00/1000次 | 10-60秒 | 95%+ | https://anti-captcha.com |
| CapMonster | 图形/reCAPTCHA | 自部署 | <5秒 | 90%+ | https://capmonster.cloud |
| 云码 | 图形/滑块/点击 | 1-3分/次 | 5-30秒 | 90%+ | https://www.jfbym.com |
| 超级鹰 | 图形/点击 | 1-5分/次 | 3-30秒 | 95%+ | https://www.chaojiying.com |

---

## 12. 依赖清单

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Nebula Crawler Core -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-crawler-core</artifactId>
        <version>${nebula.version}</version>
    </dependency>
    
    <!-- OkHttp -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    
    <!-- OpenCV (可选，用于本地滑块检测) -->
    <dependency>
        <groupId>org.openpnp</groupId>
        <artifactId>opencv</artifactId>
        <version>4.8.0-0</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

**文档版本历史**

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| V1.0 | 2026-01-06 | AI Assistant | 初始设计 |

