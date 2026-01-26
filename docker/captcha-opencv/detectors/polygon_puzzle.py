"""
多边形拼图检测器

用于检测拼图类型的验证码（背景图上有两个相似形状，需要将右侧滑块拖动到左侧目标位置）
"""

import cv2
import numpy as np
from typing import List, Dict, Tuple
from .base import BaseDetector, DetectionResult


class PolygonPuzzleDetector(BaseDetector):
    """
    多边形拼图检测器
    
    这种验证码特征:
    1. 背景图上有两个多边形形状
    2. 左侧是目标位置（缺口或半透明边框）
    3. 右侧是滑块（带白色边框）
    4. 需要将滑块拖动到左侧目标位置
    
    检测策略:
    1. Otsu 阈值检测暗色填充区域
    2. 白色边框 + 边缘检测组合
    3. 综合判断选择左侧形状
    """
    
    SHAPE_TYPE = 'polygon_puzzle'
    DEFAULT_CONFIDENCE_THRESHOLD = 0.7
    
    def detect(self, background: np.ndarray) -> DetectionResult:
        """检测多边形拼图的目标位置"""
        try:
            h, w = background.shape[:2]
            gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
            
            # 方法1: Otsu 阈值检测暗色填充区域
            otsu_result = self._detect_by_otsu(gray, h, w)
            if otsu_result:
                return otsu_result
            
            # 方法2: 白色边框 + 边缘检测
            white_border_result = self._detect_by_white_border(background, gray, h, w)
            if white_border_result:
                return white_border_result
            
            # 方法3: Canny 边缘检测
            canny_result = self._detect_by_canny(gray, h, w)
            if canny_result:
                return canny_result
            
            return self.create_failed_result('no_candidates')
            
        except Exception as e:
            self.logger.error(f"多边形拼图检测失败: {e}")
            return self.create_failed_result('error')
    
    def _detect_by_otsu(self, gray: np.ndarray, h: int, w: int) -> DetectionResult:
        """使用 Otsu 阈值检测暗色填充区域"""
        try:
            _, otsu = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
            
            kernel = np.ones((3, 3), np.uint8)
            cleaned = cv2.morphologyEx(otsu, cv2.MORPH_CLOSE, kernel)
            cleaned = cv2.morphologyEx(cleaned, cv2.MORPH_OPEN, kernel)
            
            contours, _ = cv2.findContours(cleaned, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            shapes = []
            for contour in contours:
                area = cv2.contourArea(contour)
                x, y, cw, ch = cv2.boundingRect(contour)
                
                if not (1500 < area < 15000 and x > 20 and y > 15 and y + ch < h - 30):
                    continue
                
                aspect = float(cw) / ch if ch > 0 else 0
                if not (0.6 < aspect < 1.6 and 35 < cw < 150 and 35 < ch < 150):
                    continue
                
                hull = cv2.convexHull(contour)
                hull_area = cv2.contourArea(hull)
                solidity = area / hull_area if hull_area > 0 else 0
                
                epsilon = 0.02 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)
                
                if 4 <= len(approx) <= 10 and solidity > 0.7:
                    shapes.append({
                        'x': x, 'y': y, 'w': cw, 'h': ch,
                        'center_x': x + cw // 2,
                        'area': area,
                        'vertices': len(approx)
                    })
            
            shapes.sort(key=lambda s: s['x'])
            
            self.logger.info(f"Otsu 检测到 {len(shapes)} 个形状")
            
            if len(shapes) >= 2:
                left = shapes[0]
                right = shapes[-1]
                if right['x'] - left['x'] > 50:
                    return self.create_result(
                        offset=left['x'],
                        gap_center=left['center_x'],
                        gap_width=left['w'],
                        confidence=0.9,
                        method='otsu_puzzle'
                    )
            elif len(shapes) == 1 and shapes[0]['x'] < w * 0.55:
                return self.create_result(
                    offset=shapes[0]['x'],
                    gap_center=shapes[0]['center_x'],
                    gap_width=shapes[0]['w'],
                    confidence=0.8,
                    method='otsu_single'
                )
            
            return None
            
        except Exception as e:
            self.logger.warning(f"Otsu 检测失败: {e}")
            return None
    
    def _detect_by_white_border(self, background: np.ndarray, gray: np.ndarray,
                                 h: int, w: int) -> DetectionResult:
        """使用白色边框 + 边缘检测组合"""
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
            
            # 只保留下半部分（多边形通常在底部）
            combined[:int(h * 0.45), :] = 0
            
            # 形态学闭操作连接边框
            kernel = np.ones((11, 11), np.uint8)
            closed = cv2.morphologyEx(combined, cv2.MORPH_CLOSE, kernel)
            
            contours, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            shapes = []
            for contour in contours:
                area = cv2.contourArea(contour)
                x, y, cw, ch = cv2.boundingRect(contour)
                
                if area > 1000 and cw > 40 and ch > 40:
                    epsilon = 0.02 * cv2.arcLength(contour, True)
                    approx = cv2.approxPolyDP(contour, epsilon, True)
                    
                    aspect = float(cw) / ch
                    if 0.5 < aspect < 2.0 and 4 <= len(approx) <= 12:
                        shapes.append({
                            'x': x, 'center_x': x + cw // 2,
                            'w': cw, 'area': area
                        })
            
            shapes.sort(key=lambda s: s['x'])
            
            self.logger.info(f"白色边框检测到 {len(shapes)} 个形状")
            
            if len(shapes) >= 2:
                left = shapes[0]
                right = shapes[-1]
                if right['x'] - left['x'] > 100:
                    return self.create_result(
                        offset=left['x'],
                        gap_center=left['center_x'],
                        gap_width=left['w'],
                        confidence=0.85,
                        method='white_border_puzzle'
                    )
            elif len(shapes) == 1 and shapes[0]['x'] < w * 0.4:
                return self.create_result(
                    offset=shapes[0]['x'],
                    gap_center=shapes[0]['center_x'],
                    gap_width=shapes[0]['w'],
                    confidence=0.75,
                    method='white_border_single'
                )
            
            return None
            
        except Exception as e:
            self.logger.warning(f"白色边框检测失败: {e}")
            return None
    
    def _detect_by_canny(self, gray: np.ndarray, h: int, w: int) -> DetectionResult:
        """使用 Canny 边缘检测"""
        try:
            edges = cv2.Canny(gray, 50, 150)
            contours, _ = cv2.findContours(edges, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
            
            shapes = []
            for contour in contours:
                epsilon = 0.02 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)
                area = cv2.contourArea(contour)
                
                if 5 <= len(approx) <= 12 and 1500 < area < 10000:
                    x, y, cw, ch = cv2.boundingRect(approx)
                    if x > 20 and y > 15 and 40 < cw < 120 and 40 < ch < 120:
                        aspect = float(cw) / ch
                        if 0.6 < aspect < 1.5:
                            shapes.append({
                                'x': x, 'center_x': x + cw // 2,
                                'w': cw, 'area': area
                            })
            
            shapes.sort(key=lambda s: s['x'])
            
            if len(shapes) >= 2:
                left = shapes[0]
                if left['x'] < w * 0.5:
                    return self.create_result(
                        offset=left['x'],
                        gap_center=left['center_x'],
                        gap_width=left['w'],
                        confidence=0.75,
                        method='canny_puzzle'
                    )
            
            return None
            
        except Exception as e:
            self.logger.warning(f"Canny 检测失败: {e}")
            return None
