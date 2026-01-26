#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OpenCV 验证码识别服务

提供以下功能：
1. 滑块验证码缺口检测 - /slider/detect
2. 旋转验证码角度识别 - /rotate/detect
3. 服务健康检查 - /ping
4. 测试样本获取 - /test/sample
5. 测试页面 - /test/page

作者: Nebula Team
版本: 2.0.0

更新日志:
- v2.0.0: 重构架构，支持多种形状的分类检测
- v1.1.0: 改进缺口检测算法，支持目标宽度缩放参数
"""

import base64
import io
import logging
import os
from typing import Optional, Tuple, Dict

import cv2
import numpy as np
from flask import Flask, jsonify, request, render_template_string
from PIL import Image

# 导入检测器模块
from detectors import (
    ShapeClassifier,
    RectangleDetector,
    TriangleDetector,
    HexagonDetector,
    PolygonPuzzleDetector,
    DetectionResult,
    DLDetector
)
from utils import ScalingUtils

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 初始化检测器
shape_classifier = ShapeClassifier()
dl_detector = DLDetector()  # 深度学习检测器
detectors = {
    'rectangle': RectangleDetector(),
    'triangle': TriangleDetector(),
    'hexagon': HexagonDetector(),
    'polygon_puzzle': PolygonPuzzleDetector(),
}

# 深度学习检测器可用性检查
DL_DETECTOR_ENABLED = dl_detector.is_available()
DL_CONFIDENCE_THRESHOLD = 0.8  # 深度学习结果置信度阈值
logger.info(f"深度学习检测器状态: {'可用' if DL_DETECTOR_ENABLED else '不可用'}")

app = Flask(__name__)


def detect_gap_modular(background: np.ndarray, target_width: Optional[int] = None,
                       slider_position: Optional[Dict] = None) -> dict:
    """
    模块化的缺口检测入口
    
    检测策略：
    1. 优先使用深度学习检测器（高置信度时）
    2. 回退到基于规则的分类检测
    
    Args:
        background: 背景图 (BGR格式)
        target_width: 目标显示宽度，用于计算缩放后的坐标
        slider_position: 滑块位置信息 {'x': int, 'y': int, 'w': int, 'h': int}
        
    Returns:
        dict: 检测结果
    """
    try:
        h, w = background.shape[:2]
        result = None
        
        # 策略1: 优先使用深度学习检测器
        if DL_DETECTOR_ENABLED:
            dl_result = dl_detector.detect(background)
            if dl_result.success and dl_result.confidence >= DL_CONFIDENCE_THRESHOLD:
                result = dl_result
                logger.info(f"深度学习检测成功: offset={result.offset}, confidence={result.confidence:.3f}")
            else:
                logger.info(f"深度学习检测置信度不足: {dl_result.confidence:.3f} < {DL_CONFIDENCE_THRESHOLD}")
        
        # 策略2: 回退到基于规则的分类检测
        if result is None:
            # 1. 分类验证码类型
            shape_type = shape_classifier.classify(background, slider_position)
            logger.info(f"验证码类型分类: {shape_type}")
            
            # 2. 选择对应的检测器
            detector = detectors.get(shape_type)
            
            if detector:
                result = detector.detect(background)
                logger.info(f"使用 {shape_type} 检测器: offset={result.offset}, confidence={result.confidence}")
            else:
                # 默认使用矩形检测器
                result = detectors['rectangle'].detect(background)
                logger.info(f"使用默认矩形检测器: offset={result.offset}")
            
            if not result.success:
                # 如果指定检测器失败，尝试其他检测器
                logger.info("主检测器失败，尝试其他检测器...")
                for name, det in detectors.items():
                    if name != shape_type:
                        result = det.detect(background)
                        if result.success and result.confidence > 0.6:
                            logger.info(f"备选检测器 {name} 成功: offset={result.offset}")
                            break
        
        if result is None or not result.success:
            raise ValueError("所有检测器均未能检测到缺口")
        
        # 3. 计算缩放
        scaling = ScalingUtils.calculate(
            original_offset=result.offset,
            gap_center=result.gap_center,
            gap_width=result.gap_width,
            original_width=w,
            target_width=target_width
        )
        
        return {
            'success': True,
            'offset': scaling.scaled_offset,
            'original_offset': scaling.original_offset,
            'original_width': scaling.original_width,
            'scale_ratio': scaling.scale_ratio,
            'gap_center': scaling.original_gap_center,
            'scaled_gap_center': scaling.scaled_gap_center,
            'gap_width': result.gap_width,
            'confidence': result.confidence,
            'method': result.method,
            'shape_type': result.shape_type,
        }
        
    except Exception as e:
        logger.error(f"模块化检测失败: {e}")
        # 回退到旧的检测方法
        logger.info("回退到传统检测方法...")
        return detect_gap_improved(background, target_width)


def decode_base64_image(base64_str: str) -> Optional[np.ndarray]:
    """
    解码 Base64 图片为 OpenCV 格式
    """
    try:
        # 移除可能的 data:image 前缀
        if ',' in base64_str:
            base64_str = base64_str.split(',')[1]
        
        # 解码 Base64
        image_data = base64.b64decode(base64_str)
        
        # 转换为 numpy 数组
        nparr = np.frombuffer(image_data, np.uint8)
        
        # 解码为 OpenCV 图片
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        return img
    except Exception as e:
        logger.error(f"解码 Base64 图片失败: {e}")
        return None


def detect_slider_gap(background: np.ndarray, slider: np.ndarray) -> Tuple[int, float]:
    """
    检测滑块验证码缺口位置
    支持多种匹配算法，选择最佳结果
    """
    try:
        # 转换为灰度图
        bg_gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
        slider_gray = cv2.cvtColor(slider, cv2.COLOR_BGR2GRAY)
        
        results = []
        
        # 方法1: 边缘检测 + 模板匹配
        bg_edges = cv2.Canny(bg_gray, 100, 200)
        slider_edges = cv2.Canny(slider_gray, 100, 200)
        result1 = cv2.matchTemplate(bg_edges, slider_edges, cv2.TM_CCOEFF_NORMED)
        _, max_val1, _, max_loc1 = cv2.minMaxLoc(result1)
        results.append(('edge_ccoeff', max_loc1[0], max_val1))
        
        # 方法2: 直接灰度图模板匹配
        result2 = cv2.matchTemplate(bg_gray, slider_gray, cv2.TM_CCOEFF_NORMED)
        _, max_val2, _, max_loc2 = cv2.minMaxLoc(result2)
        results.append(('gray_ccoeff', max_loc2[0], max_val2))
        
        # 方法3: 使用 TM_SQDIFF_NORMED (最小差异匹配)
        result3 = cv2.matchTemplate(bg_gray, slider_gray, cv2.TM_SQDIFF_NORMED)
        min_val3, _, min_loc3, _ = cv2.minMaxLoc(result3)
        # TM_SQDIFF 越小越好，转换为置信度
        confidence3 = 1.0 - min_val3
        results.append(('gray_sqdiff', min_loc3[0], confidence3))
        
        # 方法4: 调整 Canny 参数后的边缘匹配
        bg_edges4 = cv2.Canny(bg_gray, 50, 150)
        slider_edges4 = cv2.Canny(slider_gray, 50, 150)
        result4 = cv2.matchTemplate(bg_edges4, slider_edges4, cv2.TM_CCOEFF_NORMED)
        _, max_val4, _, max_loc4 = cv2.minMaxLoc(result4)
        results.append(('edge_ccoeff_50', max_loc4[0], max_val4))
        
        # 方法5: 高斯模糊后匹配
        bg_blur = cv2.GaussianBlur(bg_gray, (5, 5), 0)
        slider_blur = cv2.GaussianBlur(slider_gray, (5, 5), 0)
        result5 = cv2.matchTemplate(bg_blur, slider_blur, cv2.TM_CCOEFF_NORMED)
        _, max_val5, _, max_loc5 = cv2.minMaxLoc(result5)
        results.append(('blur_ccoeff', max_loc5[0], max_val5))
        
        # 选择置信度最高的结果
        best = max(results, key=lambda x: x[2])
        method, offset, confidence = best
        
        # 记录所有结果用于调试
        logger.info(f"滑块检测结果:")
        for r in results:
            logger.info(f"  {r[0]}: offset={r[1]}, confidence={r[2]:.4f}")
        logger.info(f"选择最佳: {method}, offset={offset}, confidence={confidence:.4f}")
        
        return offset, float(confidence)
        
    except Exception as e:
        logger.error(f"滑块缺口检测失败: {e}")
        raise


def detect_slider_gap_contour(background: np.ndarray) -> Tuple[int, float]:
    """
    使用轮廓检测方法检测滑块缺口
    """
    try:
        # 转换为灰度图
        gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
        
        # 高斯模糊
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        
        # 边缘检测
        edges = cv2.Canny(blurred, 50, 150)
        
        # 形态学操作 - 闭运算填充边缘
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
        closed = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)
        
        # 查找轮廓
        contours, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        # 过滤轮廓 - 找到可能是缺口的区域
        candidates = []
        img_height, img_width = background.shape[:2]
        
        for contour in contours:
            x, y, w, h = cv2.boundingRect(contour)
            area = cv2.contourArea(contour)
            
            # 过滤条件
            if 500 < area < 10000:
                aspect_ratio = float(w) / h if h > 0 else 0
                if 0.7 < aspect_ratio < 1.5:
                    if 20 < x < img_width - 50:
                        candidates.append((x, area))
        
        if candidates:
            # 按 x 坐标排序，取第一个（最左边的缺口）
            candidates.sort(key=lambda c: c[0])
            offset = candidates[0][0]
            confidence = 0.8  # 轮廓检测置信度略低
            
            logger.info(f"轮廓检测缺口: offset={offset}, confidence={confidence}")
            return offset, confidence
        
        raise ValueError("未检测到缺口")
        
    except Exception as e:
        logger.error(f"轮廓检测失败: {e}")
        raise


def detect_slider_gap_variance(background: np.ndarray, window_size: int = 60) -> Tuple[int, float]:
    """
    使用方差/标准差方法检测滑块缺口
    通过查找标准差峰值来定位缺口
    """
    try:
        gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
        h, w = gray.shape
        
        # 计算每列的标准差
        col_stds = np.std(gray, axis=0)
        
        # 平滑曲线
        kernel = np.ones(5) / 5
        col_stds_smooth = np.convolve(col_stds, kernel, mode='same')
        
        # 方法1: 找到标准差最高点，然后向左搜索缺口起始位置
        # 缺口区域的特征是标准差从低到高急剧上升
        peak_x = int(np.argmax(col_stds_smooth[20:-20]) + 20)  # 排除边缘
        
        # 从峰值向左搜索，找到标准差开始上升的位置（缺口左边缘）
        threshold = col_stds_smooth[peak_x] * 0.8  # 80% 阈值
        gap_left = peak_x
        for x in range(peak_x, 20, -1):
            if col_stds_smooth[x] < threshold:
                gap_left = x
                break
        
        # 方法2: 使用梯度找缺口边缘
        gradient = np.gradient(col_stds_smooth)
        # 在合理范围内找最大梯度（缺口左边缘）
        search_range = gradient[30:w-50]
        max_grad_idx = int(np.argmax(search_range) + 30)
        
        # 选择更可靠的结果
        # 如果两种方法结果接近，使用梯度方法
        if abs(gap_left - max_grad_idx) < window_size:
            offset = max_grad_idx
        else:
            # 使用峰值位置减去半个滑块宽度
            offset = peak_x - window_size // 2
        
        # 计算置信度
        peak_std = col_stds_smooth[peak_x]
        mean_std = np.mean(col_stds_smooth)
        std_std = np.std(col_stds_smooth)
        confidence = min(1.0, (peak_std - mean_std) / (2 * std_std)) if std_std > 0 else 0.5
        
        logger.info(f"方差检测: peak_x={peak_x}, gap_left={gap_left}, gradient_max={max_grad_idx}")
        logger.info(f"方差检测缺口: offset={offset}, confidence={confidence:.4f}")
        
        return offset, confidence
        
    except Exception as e:
        logger.error(f"方差检测失败: {e}")
        raise


def detect_polygon_captcha(background: np.ndarray) -> Tuple[int, float, str]:
    """
    检测菱形/六边形拼图验证码的目标位置
    
    这种验证码特征:
    1. 背景图上有两个多边形形状
    2. 左侧是目标位置（暗色填充的缺口）
    3. 右侧是滑块（带白色边框，内部是背景图案）
    4. 需要将滑块拖动到左侧目标位置
    
    Returns:
        Tuple: (offset, confidence, captcha_type)
            - offset: 目标位置的X坐标（缺口中心）
            - confidence: 置信度
            - captcha_type: 'polygon' 或 'unknown'
    """
    try:
        h, w = background.shape[:2]
        gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
        
        # 方法1: 使用 Otsu 阈值检测暗色填充区域
        _, otsu = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
        
        # 形态学操作
        kernel = np.ones((3, 3), np.uint8)
        cleaned = cv2.morphologyEx(otsu, cv2.MORPH_CLOSE, kernel)
        cleaned = cv2.morphologyEx(cleaned, cv2.MORPH_OPEN, kernel)
        
        # 查找轮廓
        contours, _ = cv2.findContours(cleaned, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        # 过滤和分析形状 - 寻找六边形或多边形
        shapes = []
        for contour in contours:
            area = cv2.contourArea(contour)
            x, y, cw, ch = cv2.boundingRect(contour)
            
            # 过滤条件：
            # 1. 面积在合理范围 (1500-15000 像素)
            # 2. 不在边缘 (x > 20)
            # 3. 宽高接近正方形 (0.6-1.6)
            # 4. 尺寸合理 (35-150 像素)
            if 1500 < area < 15000 and x > 20 and y > 15 and y + ch < h - 30:
                aspect = float(cw) / ch if ch > 0 else 0
                if 0.6 < aspect < 1.6 and 35 < cw < 150 and 35 < ch < 150:
                    # 凸包分析
                    hull = cv2.convexHull(contour)
                    hull_area = cv2.contourArea(hull)
                    solidity = area / hull_area if hull_area > 0 else 0
                    
                    # 多边形近似
                    epsilon = 0.02 * cv2.arcLength(contour, True)
                    approx = cv2.approxPolyDP(contour, epsilon, True)
                    
                    # 六边形通常有 5-8 个顶点，填充率 > 0.7
                    if 4 <= len(approx) <= 10 and solidity > 0.7:
                        shapes.append({
                            'x': x, 'y': y, 'w': cw, 'h': ch,
                            'center_x': x + cw // 2,
                            'center_y': y + ch // 2,
                            'area': area,
                            'vertices': len(approx),
                            'solidity': solidity
                        })
        
        # 按 x 坐标排序
        shapes.sort(key=lambda s: s['x'])
        
        logger.info(f"多边形验证码检测: 找到 {len(shapes)} 个多边形形状")
        for i, s in enumerate(shapes):
            logger.info(f"  形状{i+1}: x={s['x']}, center_x={s['center_x']}, "
                       f"面积={s['area']:.0f}, 顶点={s['vertices']}, 填充率={s['solidity']:.2f}")
        
        if len(shapes) >= 2:
            # 多个形状：最左侧是目标位置
            left_shape = shapes[0]
            right_shape = shapes[-1]
            
            # 验证：两个形状应该在不同的位置（距离 > 50 像素）
            if right_shape['x'] - left_shape['x'] > 50:
                offset = left_shape['center_x']
                confidence = 0.9
                
                logger.info(f"多边形验证码: 检测到拼图类型")
                logger.info(f"  左侧(目标): center_x={left_shape['center_x']}")
                logger.info(f"  右侧(滑块): center_x={right_shape['center_x']}")
                logger.info(f"  目标位置: {offset}")
                return offset, confidence, 'polygon'
        
        elif len(shapes) == 1:
            shape = shapes[0]
            
            # 如果只有一个形状且在左半部分或中间偏左（x < w * 0.55），可能是目标位置
            # 这种情况通常是只检测到了目标六边形（暗色填充），滑块因为是透明背景未被检测
            if shape['x'] < w * 0.55:
                offset = shape['center_x']
                confidence = 0.8
                
                logger.info(f"多边形验证码: 单一目标位置 x={offset}")
                return offset, confidence, 'polygon'
            else:
                logger.info(f"多边形验证码: 只检测到一个形状在右侧 x={shape['x']}，可能不是拼图类型")
        
        # 备选方法1: Canny 边缘检测
        edges = cv2.Canny(gray, 50, 150)
        contours, _ = cv2.findContours(edges, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
        
        edge_shapes = []
        for contour in contours:
            epsilon = 0.02 * cv2.arcLength(contour, True)
            approx = cv2.approxPolyDP(contour, epsilon, True)
            area = cv2.contourArea(contour)
            
            if 5 <= len(approx) <= 12 and 1500 < area < 10000:
                x, y, cw, ch = cv2.boundingRect(approx)
                if x > 20 and y > 15 and 40 < cw < 120 and 40 < ch < 120:
                    aspect = float(cw) / ch
                    if 0.6 < aspect < 1.5:
                        edge_shapes.append({
                            'x': x, 'center_x': x + cw // 2, 'area': area
                        })
        
        edge_shapes.sort(key=lambda s: s['x'])
        
        if len(edge_shapes) >= 2:
            left = edge_shapes[0]
            if left['x'] < w * 0.5:
                logger.info(f"多边形验证码 (Canny边缘): 目标位置 x={left['center_x']}")
                return left['center_x'], 0.75, 'polygon'
        
        # 备选方法2: 白色边框 + 边缘检测组合
        # 对于半透明边框的六边形/八边形（内部显示背景图案）
        try:
            hsv = cv2.cvtColor(background, cv2.COLOR_BGR2HSV)
            v_channel = hsv[:, :, 2]
            s_channel = hsv[:, :, 1]
            
            # 边缘检测
            edges = cv2.Canny(gray, 100, 200)
            
            # 白色区域（高亮度，低饱和度）
            white_mask = ((v_channel > 160) & (s_channel < 80)).astype(np.uint8) * 255
            white_dilated = cv2.dilate(white_mask, np.ones((15, 15), np.uint8))
            
            # 边缘 AND 白色附近
            combined = cv2.bitwise_and(edges, white_dilated)
            
            # 只保留下半部分（六边形通常在底部）
            combined[:int(h * 0.45), :] = 0
            
            # 形态学闭操作连接边框
            kernel_close = np.ones((11, 11), np.uint8)
            closed = cv2.morphologyEx(combined, cv2.MORPH_CLOSE, kernel_close)
            
            # 查找轮廓
            contours_white, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            white_shapes = []
            for contour in contours_white:
                area = cv2.contourArea(contour)
                x, y, cw, ch = cv2.boundingRect(contour)
                
                if area > 1000 and cw > 40 and ch > 40:
                    epsilon = 0.02 * cv2.arcLength(contour, True)
                    approx = cv2.approxPolyDP(contour, epsilon, True)
                    
                    aspect = float(cw) / ch
                    if 0.5 < aspect < 2.0 and 4 <= len(approx) <= 12:
                        white_shapes.append({
                            'x': x, 'center_x': x + cw // 2,
                            'area': area, 'vertices': len(approx)
                        })
            
            white_shapes.sort(key=lambda s: s['x'])
            
            logger.info(f"白色边框检测: 找到 {len(white_shapes)} 个形状")
            for s in white_shapes:
                logger.info(f"  形状: x={s['x']}, center={s['center_x']}, vertices={s['vertices']}")
            
            if len(white_shapes) >= 2:
                left = white_shapes[0]
                right = white_shapes[-1]
                if right['x'] - left['x'] > 100:  # 两个形状距离足够远
                    logger.info(f"多边形验证码 (白色边框): 目标位置 x={left['center_x']}")
                    return left['center_x'], 0.85, 'polygon'
            elif len(white_shapes) == 1 and white_shapes[0]['x'] < w * 0.4:
                logger.info(f"多边形验证码 (白色边框单一): 目标位置 x={white_shapes[0]['center_x']}")
                return white_shapes[0]['center_x'], 0.75, 'polygon'
                
        except Exception as e:
            logger.warning(f"白色边框检测失败: {e}")
        
        # 备选方法3: Sobel 垂直边缘分析
        # 对于半透明八边形（只有白色边框），使用 Sobel 检测垂直边缘
        try:
            sobel_x = cv2.Sobel(gray, cv2.CV_64F, 1, 0, ksize=3)
            sobel_x = np.abs(sobel_x)
            
            # 在八边形可能存在的垂直范围内（y: 15% - 55%）求和
            y_start = int(h * 0.15)
            y_end = int(h * 0.55)
            col_sum = np.sum(sobel_x[y_start:y_end, :], axis=0)
            
            # 平滑处理
            kernel_size = 5
            col_sum_smooth = np.convolve(col_sum, np.ones(kernel_size)/kernel_size, mode='same')
            
            # 找峰值（边缘位置）
            threshold = np.percentile(col_sum_smooth, 70)
            peaks = []
            in_peak = False
            peak_start = 0
            
            for i, val in enumerate(col_sum_smooth):
                if val > threshold and not in_peak:
                    in_peak = True
                    peak_start = i
                elif val <= threshold and in_peak:
                    in_peak = False
                    peak_center = (peak_start + i) // 2
                    if 40 < peak_center < w - 40:  # 排除边缘噪声
                        peaks.append(peak_center)
            
            logger.info(f"Sobel边缘峰值: {peaks}")
            
            # 按距离分组峰值
            if len(peaks) >= 2:
                groups = []
                current_group = [peaks[0]]
                for p in peaks[1:]:
                    if p - current_group[-1] < 40:  # 同一形状的边缘
                        current_group.append(p)
                    else:
                        groups.append(current_group)
                        current_group = [p]
                groups.append(current_group)
                
                logger.info(f"Sobel边缘分组: {len(groups)} 组")
                
                # 计算每组的中心和跨度
                shape_centers = []
                for g in groups:
                    center = sum(g) // len(g)
                    span = max(g) - min(g) if len(g) > 1 else 0
                    # 有效的多边形边缘组应该有一定跨度（40-120像素）
                    # 太小的跨度可能是背景噪声（山石边缘等）
                    if 40 <= span <= 120:
                        shape_centers.append({'center': center, 'span': span, 'peaks': g})
                        logger.info(f"  形状候选: center={center}, span={span}")
                
                # 按中心位置排序
                shape_centers.sort(key=lambda x: x['center'])
                
                # Sobel 方法只用于检测拼图类型（两个相距足够远的形状）
                # 不处理单个形状，避免对三角形缺口等类型的误检测
                if len(shape_centers) >= 2:
                    left = shape_centers[0]
                    right = shape_centers[-1]
                    if right['center'] - left['center'] > 150:  # 两个形状距离足够远
                        logger.info(f"多边形验证码 (Sobel边缘): 目标位置 x={left['center']}")
                        return left['center'], 0.7, 'polygon'
                
                # 注意：删除了单个形状返回逻辑，避免对复杂背景的误检测
        except Exception as e:
            logger.warning(f"Sobel边缘分析失败: {e}")
        
        return 0, 0, 'unknown'
        
    except Exception as e:
        logger.error(f"多边形验证码检测失败: {e}")
        return 0, 0, 'unknown'


def detect_gap_improved(background: np.ndarray, target_width: Optional[int] = None) -> dict:
    """
    改进的缺口检测算法
    针对腾讯验证码特征优化，使用多种方法综合判断

    Args:
        background: 背景图 (BGR格式)
        target_width: 目标显示宽度，用于计算缩放后的坐标

    Returns:
        dict: 包含检测结果的字典
            - offset: 偏移量 (如提供target_width则是缩放后的值)
            - original_offset: 原始图片上的偏移量
            - original_width: 原始图片宽度
            - scale_ratio: 缩放比例 (如有缩放)
            - confidence: 置信度
            - method: 使用的方法
    """
    try:
        gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
        h, w = gray.shape

        results = []

        # 方法0: 先尝试检测多边形验证码（菱形/三角形拼图）
        polygon_offset, polygon_confidence, captcha_type = detect_polygon_captcha(background)
        if captcha_type == 'polygon' and polygon_offset > 0 and polygon_confidence > 0.7:
            logger.info(f"检测到多边形验证码，使用多边形检测结果: offset={polygon_offset}")
            results.append({
                'method': 'polygon',
                'x': polygon_offset,
                'confidence': polygon_confidence
            })

        # 方法1: 多边形轮廓检测 (支持矩形、六边形等多种形状)
        for canny_thresh in [(50, 100), (80, 160), (100, 200)]:
            edges = cv2.Canny(gray, canny_thresh[0], canny_thresh[1])

            # 闭运算连接边缘
            kernel = np.ones((3, 3), np.uint8)
            closed = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)

            # 使用 RETR_LIST 获取所有轮廓
            contours, _ = cv2.findContours(closed, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)

            for contour in contours:
                # 多边形近似
                epsilon = 0.02 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)

                # 寻找4-10边形 (支持矩形、六边形等)
                if 4 <= len(approx) <= 10:
                    x, y, cw, ch = cv2.boundingRect(approx)
                    area = cv2.contourArea(contour)

                    # 腾讯验证码缺口特征:
                    # 1. 在图片右半部分 (x > w * 0.3)
                    # 2. 接近正方形 (0.7 < aspect < 1.4)
                    # 3. 大小适中 (50-160 像素)
                    # 4. 面积在合理范围内
                    if x > w * 0.3 and 50 < cw < 160 and 50 < ch < 160 and area > 3000:
                        aspect = float(cw) / ch
                        if 0.7 < aspect < 1.4:
                            # 计算边缘密度
                            roi_edges = edges[y:y + ch, x:x + cw]
                            edge_density = np.sum(roi_edges > 0) / (cw * ch) if cw * ch > 0 else 0

                            # 检测形状内部是否是暗色（缺口内部通常是黑色/暗色）
                            roi_gray = gray[y:y + ch, x:x + cw]
                            mean_brightness = np.mean(roi_gray)
                            img_mean = np.mean(gray)
                            
                            # 如果内部比整体图片暗很多，更可能是缺口
                            is_dark_inside = mean_brightness < img_mean * 0.6
                            dark_bonus = 0.3 if is_dark_inside else 0
                            
                            # 根据顶点数调整置信度
                            vertex_bonus = 0.1 if len(approx) in [4, 6] else 0
                            base_confidence = min(0.8, edge_density * 4)
                            confidence = min(1.0, base_confidence + vertex_bonus + dark_bonus)

                            if edge_density > 0.04:
                                shape_type = 'rect' if len(approx) == 4 else f'poly{len(approx)}'
                                results.append({
                                    'method': f'contour_{canny_thresh}_{shape_type}',
                                    'x': x,
                                    'y': y,
                                    'width': cw,
                                    'height': ch,
                                    'vertices': len(approx),
                                    'area': area,
                                    'mean_brightness': mean_brightness,
                                    'is_dark': is_dark_inside,
                                    'confidence': confidence
                                })

        # 方法2: 垂直边缘检测 (霍夫变换)
        edges = cv2.Canny(gray, 100, 200)
        lines = cv2.HoughLinesP(edges, 1, np.pi / 180, 50, minLineLength=50, maxLineGap=5)

        vertical_lines_x = []
        if lines is not None:
            for line in lines:
                x1, y1, x2, y2 = line[0]
                if abs(x1 - x2) < 5:  # 垂直线
                    avg_x = (x1 + x2) // 2
                    length = abs(y2 - y1)
                    if length > 60 and avg_x > w * 0.4:
                        vertical_lines_x.append(int(avg_x))

        if vertical_lines_x:
            vertical_lines_x.sort()
            for i in range(len(vertical_lines_x) - 1):
                gap = vertical_lines_x[i + 1] - vertical_lines_x[i]
                # 缺口宽度大约 60-110 像素
                if 60 < gap < 110:
                    results.append({
                        'method': 'vertical_edges',
                        'x': vertical_lines_x[i],
                        'width': gap,
                        'confidence': 0.9
                    })

        # 方法3: 梯度分析 (作为辅助)
        col_stds = np.std(gray, axis=0)
        kernel_smooth = np.ones(5) / 5
        col_stds_smooth = np.convolve(col_stds, kernel_smooth, mode='same')
        gradient = np.gradient(col_stds_smooth)

        search_start = int(w * 0.3)
        search_end = w - 50
        if search_end > search_start:
            gradient_max_idx = int(np.argmax(gradient[search_start:search_end]) + search_start)
            results.append({
                'method': 'gradient',
                'x': gradient_max_idx,
                'confidence': 0.7
            })

        # 方法4: 暗色区域检测 (对六边形等缺口有效)
        try:
            hsv = cv2.cvtColor(background, cv2.COLOR_BGR2HSV)
            v_channel = hsv[:, :, 2]
            
            # 计算右半部分的亮度统计
            right_half = v_channel[:, w // 3:]
            mean_v = np.mean(right_half)
            std_v = np.std(right_half)
            
            # 检测明显暗于周围的区域
            dark_threshold = mean_v - std_v * 0.8
            dark_mask = (v_channel < dark_threshold).astype(np.uint8) * 255
            
            # 只分析右半部分
            dark_mask[:, :w // 3] = 0
            
            # 形态学操作
            kernel = np.ones((5, 5), np.uint8)
            dark_mask = cv2.morphologyEx(dark_mask, cv2.MORPH_CLOSE, kernel)
            dark_mask = cv2.morphologyEx(dark_mask, cv2.MORPH_OPEN, kernel)
            
            # 查找轮廓
            contours, _ = cv2.findContours(dark_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            for contour in contours:
                area = cv2.contourArea(contour)
                x, y, cw, ch = cv2.boundingRect(contour)
                
                # 过滤：面积合理、形状接近正方形、位置在右侧
                if area > 4000 and x > w * 0.4 and 60 < cw < 150 and 60 < ch < 150:
                    aspect = float(cw) / ch
                    if 0.7 < aspect < 1.4:
                        # 计算填充率
                        hull = cv2.convexHull(contour)
                        hull_area = cv2.contourArea(hull)
                        solidity = area / hull_area if hull_area > 0 else 0
                        
                        if solidity > 0.6:  # 形状比较饱满
                            results.append({
                                'method': 'dark_region',
                                'x': x,
                                'y': y,
                                'width': cw,
                                'height': ch,
                                'area': area,
                                'confidence': 0.85
                            })
                            logger.info(f"暗色区域检测: x={x}, 宽={cw}, 高={ch}, 面积={area}, 填充率={solidity:.2f}")
        except Exception as e:
            logger.warning(f"暗色区域检测失败: {e}")

        if not results:
            raise ValueError("未检测到缺口")

        # 记录所有检测结果
        logger.info(f"检测到 {len(results)} 个候选区域:")
        for r in results:
            logger.info(f"  {r['method']}: x={r['x']}, conf={r['confidence']:.2f}")

        # 综合判断: 优先选择右侧、暗色内部、高置信度的结果
        # 计算综合得分
        for r in results:
            # 基础分 = 置信度
            score = r['confidence']
            
            # 位置加分: 越靠右越好（缺口通常在图片右侧）
            x_ratio = r['x'] / w
            if x_ratio > 0.65:
                score += 0.3  # 右侧区域大加分
            elif x_ratio > 0.55:
                score += 0.15  # 中右区域小加分
            
            # 暗色内部加分: 缺口内部是黑色/暗色
            if r.get('is_dark', False):
                score += 0.25
            
            # 面积加分: 如果有面积信息，面积大的加分
            if 'area' in r and r['area'] > 5000:
                score += 0.1
            
            # 形状类型加分: 暗色区域检测通常更可靠
            if r['method'] == 'dark_region':
                score += 0.15
            if r['method'] == 'polygon':
                score += 0.1
            
            r['score'] = score
            logger.debug(f"  {r['method']}: x={r['x']}, conf={r['confidence']:.2f}, score={score:.2f}, dark={r.get('is_dark', 'N/A')}")

        # 按综合得分排序
        results.sort(key=lambda r: r['score'], reverse=True)
        
        # 选择得分最高的结果
        best_result = results[0]
        final_x = best_result['x']
        final_confidence = best_result['confidence']
        best_method = best_result['method']
        
        logger.info(f"最终选择: x={final_x}, score={best_result['score']:.2f}, method={best_method}")

        # 处理缩放
        scale_ratio = None
        scaled_offset = final_x
        if target_width and target_width > 0 and target_width != w:
            scale_ratio = target_width / w
            scaled_offset = int(final_x * scale_ratio)

        logger.info(f"改进算法检测结果: final_x={final_x}, scaled={scaled_offset}, "
                    f"confidence={final_confidence:.4f}, method={best_method}")

        # 获取缺口宽度（如果有）
        gap_width = best_result.get('width', 80)
        gap_center = final_x + gap_width // 2
        scaled_gap_center = int(gap_center * scale_ratio) if scale_ratio else gap_center

        # 生成并保存调试图片
        debug_images = {}
        try:
            import time
            timestamp = int(time.time())
            
            # 1. 保存原始背景图
            cv2.imwrite('/tmp/captcha_1_background.png', background)
            _, bg_buffer = cv2.imencode('.png', background)
            debug_images['background'] = base64.b64encode(bg_buffer).decode('utf-8')
            
            # 2. 带滑块初始位置的背景图
            slider_init_img = background.copy()
            # 滑块初始位置（左边缘约 50 像素处，缩放前）
            slider_init_x = int(50 / (340 / w)) if target_width else 50
            slider_width = int(65 / (340 / w)) if target_width else 65  # 滑块宽度约 65 像素
            slider_center = slider_init_x + slider_width // 2
            # 画滑块初始位置区域（黄色）
            cv2.line(slider_init_img, (slider_init_x, 0), (slider_init_x, h), (0, 255, 255), 2)
            cv2.line(slider_init_img, (slider_center, 0), (slider_center, h), (0, 200, 255), 2)
            cv2.putText(slider_init_img, f"slider_init={slider_init_x}, center={slider_center}", (10, 30), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 255), 2)
            cv2.imwrite('/tmp/captcha_2_slider_initial.png', slider_init_img)
            
            # 3. 带检测结果的图（标记缺口位置）
            detection_img = background.copy()
            # 画缺口左边缘（红色）
            cv2.line(detection_img, (final_x, 0), (final_x, h), (0, 0, 255), 2)
            # 画缺口中心（绿色）
            cv2.line(detection_img, (gap_center, 0), (gap_center, h), (0, 255, 0), 2)
            # 画缺口区域矩形（蓝色）
            if 'width' in best_result and 'height' in best_result:
                y = best_result.get('y', h // 3)
                cv2.rectangle(detection_img, (final_x, y), (final_x + gap_width, y + best_result['height']), (255, 0, 0), 2)
            cv2.putText(detection_img, f"gap_left={final_x}, gap_center={gap_center}", (10, 30), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.imwrite('/tmp/captcha_3_gap_detection.png', detection_img)
            _, debug_buffer = cv2.imencode('.png', detection_img)
            debug_images['detection'] = base64.b64encode(debug_buffer).decode('utf-8')
            
            # 4. 最终效果图（滑块填充缺口后）
            final_img = background.copy()
            # 画滑块最终位置（应该与缺口中心对齐）
            slider_final_x = gap_center - slider_width // 2
            cv2.line(final_img, (slider_final_x, 0), (slider_final_x, h), (0, 255, 255), 2)
            cv2.line(final_img, (gap_center, 0), (gap_center, h), (0, 255, 0), 2)
            # 计算滑动距离
            slide_distance = gap_center - slider_center
            cv2.putText(final_img, f"slide_distance={slide_distance}", (10, 30), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.putText(final_img, f"({slider_center} -> {gap_center})", (10, 60), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.imwrite('/tmp/captcha_4_final_position.png', final_img)
            
            logger.info(f"调试图已保存: /tmp/captcha_1_background.png ~ captcha_4_final_position.png")
            logger.info(f"滑块: init={slider_init_x}, center={slider_center}, width={slider_width}")
            logger.info(f"缺口: left={final_x}, center={gap_center}, width={gap_width}")
            logger.info(f"计算滑动距离: {slide_distance} (缩放前), {int(slide_distance * scale_ratio) if scale_ratio else slide_distance} (缩放后)")
            
        except Exception as e:
            logger.warning(f"生成调试图片失败: {e}")

        return {
            'offset': scaled_offset,
            'original_offset': final_x,
            'gap_width': gap_width,
            'gap_center': gap_center,
            'scaled_gap_center': scaled_gap_center,
            'original_width': w,
            'original_height': h,
            'scale_ratio': scale_ratio,
            'confidence': float(final_confidence),
            'method': best_method,
            'all_results': [{'method': r['method'], 'x': r['x'], 'confidence': r['confidence']}
                            for r in results],
            'debug_images': debug_images
        }

    except Exception as e:
        logger.error(f"改进算法检测失败: {e}")
        raise


def detect_rotate_angle(image: np.ndarray) -> Tuple[int, float]:
    """
    检测旋转验证码的正确角度
    """
    try:
        # 转换为灰度图
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # 边缘检测
        edges = cv2.Canny(gray, 50, 150)
        
        # 霍夫直线变换检测主要线条方向
        lines = cv2.HoughLinesP(edges, 1, np.pi/180, 50, 
                                minLineLength=30, maxLineGap=10)
        
        if lines is None or len(lines) == 0:
            return 0, 0.5
        
        # 统计线条角度
        angles = []
        for line in lines:
            x1, y1, x2, y2 = line[0]
            angle = np.arctan2(y2 - y1, x2 - x1) * 180 / np.pi
            angles.append(angle)
        
        # 计算平均角度
        avg_angle = np.mean(angles)
        
        # 将角度转换为 0-360 范围
        if avg_angle < 0:
            avg_angle += 360
        
        # 计算需要旋转的角度（使其水平或垂直）
        rotate_angle = round(avg_angle / 90) * 90 - avg_angle
        rotate_angle = int(rotate_angle) % 360
        
        confidence = min(0.9, len(lines) / 50)  # 线条越多置信度越高
        
        logger.info(f"旋转角度检测: angle={rotate_angle}, confidence={confidence:.4f}")
        
        return rotate_angle, confidence
        
    except Exception as e:
        logger.error(f"旋转角度检测失败: {e}")
        raise


@app.route('/ping', methods=['GET'])
def ping():
    """健康检查"""
    return jsonify({
        'status': 'ok',
        'service': 'captcha-opencv',
        'version': '1.0.0'
    })


@app.route('/slider/detect', methods=['POST'])
def slider_detect():
    """
    滑块验证码缺口检测接口
    支持多种检测方法，自动选择最佳结果

    参数:
        background: Base64编码的背景图 (必填)
        slider: Base64编码的滑块图 (可选)
        target_width: 目标显示宽度 (可选，用于计算缩放后坐标)
        use_improved: 是否使用改进算法 (可选，默认true)

    返回:
        success: 是否成功
        offset: 偏移量 (如提供target_width则是缩放后的值)
        original_offset: 原始图片上的偏移量
        original_width: 原始图片宽度
        scale_ratio: 缩放比例 (如有缩放)
        confidence: 置信度
        method: 使用的方法
    """
    try:
        background_b64 = request.form.get('background')
        slider_b64 = request.form.get('slider')
        target_width_str = request.form.get('target_width')
        use_improved_str = request.form.get('use_improved', 'true')

        if not background_b64:
            return jsonify({
                'success': False,
                'error': '缺少 background 参数'
            }), 400

        # 解析 target_width
        target_width = None
        if target_width_str:
            try:
                target_width = int(target_width_str)
            except ValueError:
                logger.warning(f"无效的 target_width: {target_width_str}")

        # 解析 use_improved
        use_improved = use_improved_str.lower() in ('true', '1', 'yes')

        # 解码背景图
        background = decode_base64_image(background_b64)
        if background is None:
            return jsonify({
                'success': False,
                'error': '背景图解码失败'
            }), 400

        # 解析滑块位置（如果提供）
        slider_position = None
        slider_x = request.form.get('slider_x')
        slider_y = request.form.get('slider_y')
        slider_w = request.form.get('slider_w')
        slider_h = request.form.get('slider_h')
        if slider_x and slider_y:
            try:
                slider_position = {
                    'x': int(slider_x),
                    'y': int(slider_y),
                    'w': int(slider_w) if slider_w else 60,
                    'h': int(slider_h) if slider_h else 60
                }
            except ValueError:
                logger.warning("滑块位置参数解析失败")

        # 策略优先级：深度学习 > 模块化检测 > 模板匹配
        # 深度学习和模块化检测对腾讯验证码更准确
        
        h, w = background.shape[:2]
        slider = None
        if slider_b64:
            slider = decode_base64_image(slider_b64)
            logger.info(f"检测到滑块图，尺寸: {slider.shape[:2] if slider is not None else 'N/A'}")
        
        # 策略1: 优先使用深度学习检测器（即使有滑块图也优先使用）
        if DL_DETECTOR_ENABLED:
            dl_result = dl_detector.detect(background)
            if dl_result.success and dl_result.confidence >= DL_CONFIDENCE_THRESHOLD:
                logger.info(f"深度学习检测成功: offset={dl_result.offset}, confidence={dl_result.confidence:.3f}")
                
                # 计算缩放
                scaling = ScalingUtils.calculate(
                    original_offset=dl_result.offset,
                    gap_center=dl_result.gap_center,
                    gap_width=dl_result.gap_width,
                    original_width=w,
                    target_width=target_width
                )
                
                return jsonify({
                    'success': True,
                    'offset': scaling.scaled_offset,
                    'original_offset': scaling.original_offset,
                    'original_width': scaling.original_width,
                    'scale_ratio': scaling.scale_ratio,
                    'gap_center': scaling.original_gap_center,
                    'scaled_gap_center': scaling.scaled_gap_center,
                    'gap_width': dl_result.gap_width,
                    'confidence': dl_result.confidence,
                    'method': 'deep_learning',
                    'shape_type': dl_result.shape_type,
                })
            else:
                logger.info(f"深度学习检测置信度不足: {dl_result.confidence:.3f} < {DL_CONFIDENCE_THRESHOLD}")
        
        # 策略2: 使用模块化检测（v2.0）
        if use_improved:
            try:
                result = detect_gap_modular(background, target_width, slider_position)
                return jsonify({
                    'success': True,
                    'offset': result['offset'],
                    'original_offset': result['original_offset'],
                    'gap_width': result.get('gap_width', 0),
                    'gap_center': result.get('gap_center', 0),
                    'scaled_gap_center': result.get('scaled_gap_center', 0),
                    'original_width': result['original_width'],
                    'scale_ratio': result['scale_ratio'],
                    'confidence': result['confidence'],
                    'method': result['method'],
                    'shape_type': result.get('shape_type', 'unknown')
                })
            except Exception as e:
                logger.warning(f"模块化检测失败，回退到传统算法: {e}")

        # 回退到传统算法
        results = []
        img_width = background.shape[1]

        # 方法1: 方差检测
        try:
            window_size = int(img_width * 0.18)
            offset, confidence = detect_slider_gap_variance(background, window_size)
            results.append(('variance', offset, confidence))
        except Exception as e:
            logger.warning(f"方差检测失败: {e}")

        # 方法2: 如果有滑块图，尝试模板匹配（作为回退策略）
        if slider is not None:
            try:
                offset, confidence = detect_slider_gap(background, slider)
                results.append(('template', offset, confidence))
            except Exception as e:
                logger.warning(f"模板匹配失败: {e}")

        # 方法3: 轮廓检测
        try:
            offset, confidence = detect_slider_gap_contour(background)
            results.append(('contour', offset, confidence))
        except Exception as e:
            logger.warning(f"轮廓检测失败: {e}")

        if not results:
            return jsonify({
                'success': False,
                'error': '未检测到缺口'
            }), 500

        # 选择置信度最高的结果
        best = max(results, key=lambda x: x[2])
        method, offset, confidence = best

        # 记录所有结果
        logger.info(f"传统算法检测结果:")
        for r in results:
            logger.info(f"  {r[0]}: offset={r[1]}, confidence={r[2]:.4f}")
        logger.info(f"选择: {method}, offset={offset}, confidence={confidence:.4f}")

        # 处理缩放
        original_offset = offset
        scale_ratio = None
        if target_width and target_width > 0 and target_width != img_width:
            scale_ratio = target_width / img_width
            offset = int(offset * scale_ratio)

        return jsonify({
            'success': True,
            'offset': offset,
            'original_offset': original_offset,
            'original_width': img_width,
            'scale_ratio': scale_ratio,
            'confidence': confidence,
            'method': method
        })

    except Exception as e:
        logger.exception("滑块检测异常")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/rotate/detect', methods=['POST'])
def rotate_detect():
    """
    旋转验证码角度检测接口
    """
    try:
        image_b64 = request.form.get('image')
        
        if not image_b64:
            return jsonify({
                'success': False,
                'error': '缺少 image 参数'
            }), 400
        
        # 解码图片
        image = decode_base64_image(image_b64)
        if image is None:
            return jsonify({
                'success': False,
                'error': '图片解码失败'
            }), 400
        
        angle, confidence = detect_rotate_angle(image)
        
        return jsonify({
            'success': True,
            'angle': angle,
            'confidence': confidence
        })
        
    except Exception as e:
        logger.exception("旋转检测异常")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/image/info', methods=['POST'])
def image_info():
    """
    获取图片信息（用于调试）
    """
    try:
        image_b64 = request.form.get('image')
        
        if not image_b64:
            return jsonify({
                'success': False,
                'error': '缺少 image 参数'
            }), 400
        
        image = decode_base64_image(image_b64)
        if image is None:
            return jsonify({
                'success': False,
                'error': '图片解码失败'
            }), 400
        
        height, width = image.shape[:2]
        channels = image.shape[2] if len(image.shape) > 2 else 1
        
        return jsonify({
            'success': True,
            'width': width,
            'height': height,
            'channels': channels
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/test/sample', methods=['GET'])
def test_sample():
    """
    获取测试样本 (返回滑块检测所需的 Base64 图片)
    """
    try:
        # 创建一个纯色背景图 (灰色)
        bg = np.zeros((300, 500, 3), np.uint8)
        bg.fill(200) 
        
        # 绘制一个简单的矩形作为缺口 (黑色)
        # 缺口位置 x=200
        cv2.rectangle(bg, (200, 100), (250, 150), (0, 0, 0), -1)
        
        # 添加一些干扰线
        for _ in range(5):
            pt1 = (np.random.randint(0, 500), np.random.randint(0, 300))
            pt2 = (np.random.randint(0, 500), np.random.randint(0, 300))
            cv2.line(bg, pt1, pt2, (255, 255, 255), 1)
        
        # 编码为 Base64
        _, buffer = cv2.imencode('.jpg', bg)
        bg_b64 = base64.b64encode(buffer).decode('utf-8')
        
        return jsonify({
            'success': True,
            'background': bg_b64,
            'slider': None, # 模拟无滑块模式 (使用轮廓检测)
            'expected_offset_min': 190,
            'expected_offset_max': 210
        })
    except Exception as e:
        logger.exception("生成样本异常")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/test/page', methods=['GET'])
def test_page():
    """测试页面"""
    html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>OpenCV 滑块检测测试</title>
        <style>
            body { font-family: sans-serif; padding: 20px; }
            .container { max-width: 800px; margin: 0 auto; border: 1px solid #ccc; padding: 20px; border-radius: 8px; }
            .img-container { position: relative; display: inline-block; }
            img { border: 1px solid #ddd; max-width: 100%; }
            .gap-marker {
                position: absolute;
                top: 0;
                bottom: 0;
                width: 2px;
                background-color: red;
                z-index: 10;
                display: none;
            }
            button { padding: 10px 20px; cursor: pointer; background-color: #28a745; color: white; border: none; border-radius: 4px; margin-right: 10px; }
            button:hover { background-color: #218838; }
            .result { margin-top: 20px; font-weight: bold; }
            pre { background: #f8f9fa; padding: 10px; border-radius: 4px; }
        </style>
    </head>
    <body>
        <div class="container">
            <h2>OpenCV 滑块缺口检测测试</h2>
            
            <div>
                <h3>1. 获取样本 (模拟图片)</h3>
                <div class="img-container">
                    <img id="bg-img" src="" alt="背景图" style="display:none;">
                    <div id="gap-marker" class="gap-marker"></div>
                </div>
                <br><br>
                <button onclick="getSample()">生成新样本</button>
            </div>
            
            <div>
                <h3>2. 检测缺口</h3>
                <button onclick="detectGap()">检测缺口位置</button>
                <div class="result" id="result-area"></div>
            </div>
            
            <div>
                <h3>API 响应</h3>
                <pre id="api-response"></pre>
            </div>
        </div>

        <script>
            let currentBgBase64 = '';

            async function getSample() {
                try {
                    document.getElementById('gap-marker').style.display = 'none';
                    const res = await fetch('/test/sample');
                    const data = await res.json();
                    if (data.success) {
                        currentBgBase64 = data.background;
                        const img = document.getElementById('bg-img');
                        img.src = 'data:image/jpeg;base64,' + data.background;
                        img.style.display = 'block';
                        document.getElementById('result-area').innerText = '预期范围: ' + data.expected_offset_min + ' - ' + data.expected_offset_max;
                        document.getElementById('api-response').innerText = '样本获取成功';
                    } else {
                        alert('获取失败: ' + data.error);
                    }
                } catch (e) {
                    alert('请求异常: ' + e);
                }
            }

            async function detectGap() {
                if (!currentBgBase64) {
                    alert('请先获取样本');
                    return;
                }
                
                try {
                    document.getElementById('result-area').innerText += ' | 检测中...';
                    
                    const formData = new FormData();
                    formData.append('background', currentBgBase64);
                    
                    const res = await fetch('/slider/detect', {
                        method: 'POST',
                        body: formData
                    });
                    const data = await res.json();
                    document.getElementById('api-response').innerText = JSON.stringify(data, null, 2);
                    
                    if (data.success) {
                        document.getElementById('result-area').innerText += ' | 检测结果: offset=' + data.offset + ', confidence=' + data.confidence;
                        
                        // 在图片上标记位置
                        const marker = document.getElementById('gap-marker');
                        marker.style.left = data.offset + 'px';
                        marker.style.display = 'block';
                    } else {
                        document.getElementById('result-area').innerText += ' | 检测失败';
                    }
                } catch (e) {
                    alert('请求异常: ' + e);
                }
            }
            
            // 初始化
            getSample();
        </script>
    </body>
    </html>
    """
    return render_template_string(html)


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8080))
    debug = os.environ.get('DEBUG', 'false').lower() == 'true'
    
    logger.info(f"启动 OpenCV 验证码识别服务: port={port}, debug={debug}")
    
    app.run(host='0.0.0.0', port=port, debug=debug)
