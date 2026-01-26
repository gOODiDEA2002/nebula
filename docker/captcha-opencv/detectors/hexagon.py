"""
六边形/八边形缺口检测器

用于检测六边形、八边形等多边形形状的滑块验证码缺口
"""

import cv2
import numpy as np
from typing import List, Dict
from .base import BaseDetector, DetectionResult


class HexagonDetector(BaseDetector):
    """
    六边形/八边形缺口检测器
    
    检测策略:
    1. 边缘检测 + 多边形轮廓 (5-10 边)
    2. 暗色区域检测
    3. 梯度分析
    """
    
    SHAPE_TYPE = 'hexagon'
    DEFAULT_CONFIDENCE_THRESHOLD = 0.6
    
    # 支持的顶点数范围
    MIN_VERTICES = 5
    MAX_VERTICES = 10
    
    def detect(self, background: np.ndarray) -> DetectionResult:
        """检测六边形/八边形缺口位置"""
        try:
            gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
            h, w = gray.shape
            
            candidates = []
            
            # 方法1: 边缘检测 + 多边形轮廓
            contour_results = self._detect_by_contour(gray, h, w)
            candidates.extend(contour_results)
            
            # 方法2: 暗色区域检测
            dark_results = self._detect_by_dark_region(background, h, w)
            candidates.extend(dark_results)
            
            # 方法3: 梯度分析作为兜底
            if len(candidates) < 2:
                gradient_result = self._detect_by_gradient(gray, h, w)
                if gradient_result:
                    candidates.append(gradient_result)
            
            if not candidates:
                return self.create_failed_result('no_candidates')
            
            # 选择最佳结果
            best = self._select_best_candidate(candidates, w)
            
            return self.create_result(
                offset=best['x'],
                gap_center=best['x'] + best.get('width', 80) // 2,
                gap_width=best.get('width', 80),
                confidence=best['confidence'],
                method=best['method']
            )
            
        except Exception as e:
            self.logger.error(f"六边形检测失败: {e}")
            return self.create_failed_result('error')
    
    def _detect_by_contour(self, gray: np.ndarray, h: int, w: int) -> List[Dict]:
        """使用轮廓检测多边形"""
        results = []
        
        for canny_thresh in [(50, 100), (80, 160), (100, 200)]:
            edges = cv2.Canny(gray, canny_thresh[0], canny_thresh[1])
            
            kernel = np.ones((3, 3), np.uint8)
            closed = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)
            
            contours, _ = cv2.findContours(closed, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
            
            for contour in contours:
                epsilon = 0.02 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)
                
                # 检测 5-10 边形
                if not (self.MIN_VERTICES <= len(approx) <= self.MAX_VERTICES):
                    continue
                
                x, y, cw, ch = cv2.boundingRect(approx)
                area = cv2.contourArea(contour)
                
                # 过滤条件
                if not (x > w * 0.3 and 50 < cw < 160 and 50 < ch < 160 and area > 3000):
                    continue
                
                aspect = float(cw) / ch
                if not (0.7 < aspect < 1.4):
                    continue
                
                # 计算边缘密度
                roi_edges = edges[y:y+ch, x:x+cw]
                edge_density = np.sum(roi_edges > 0) / (cw * ch) if cw * ch > 0 else 0
                
                # 检测内部亮度
                roi_gray = gray[y:y+ch, x:x+cw]
                mean_brightness = np.mean(roi_gray)
                img_mean = np.mean(gray)
                is_dark = mean_brightness < img_mean * 0.6
                
                base_confidence = min(0.8, edge_density * 4)
                vertex_bonus = 0.1 if len(approx) in [6, 8] else 0.05
                dark_bonus = 0.3 if is_dark else 0
                confidence = min(1.0, base_confidence + vertex_bonus + dark_bonus)
                
                if edge_density > 0.04:
                    results.append({
                        'method': f'contour_poly{len(approx)}',
                        'x': x,
                        'y': y,
                        'width': cw,
                        'height': ch,
                        'vertices': len(approx),
                        'area': area,
                        'is_dark': is_dark,
                        'confidence': confidence
                    })
        
        return results
    
    def _detect_by_dark_region(self, background: np.ndarray, h: int, w: int) -> List[Dict]:
        """使用暗色区域检测"""
        results = []
        
        try:
            hsv = cv2.cvtColor(background, cv2.COLOR_BGR2HSV)
            v_channel = hsv[:, :, 2]
            
            right_half = v_channel[:, w // 3:]
            mean_v = np.mean(right_half)
            std_v = np.std(right_half)
            
            dark_threshold = mean_v - std_v * 0.8
            dark_mask = (v_channel < dark_threshold).astype(np.uint8) * 255
            dark_mask[:, :w // 3] = 0
            
            kernel = np.ones((5, 5), np.uint8)
            dark_mask = cv2.morphologyEx(dark_mask, cv2.MORPH_CLOSE, kernel)
            dark_mask = cv2.morphologyEx(dark_mask, cv2.MORPH_OPEN, kernel)
            
            contours, _ = cv2.findContours(dark_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            for contour in contours:
                epsilon = 0.02 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)
                
                area = cv2.contourArea(contour)
                x, y, cw, ch = cv2.boundingRect(contour)
                
                # 过滤：形状接近正方形
                if area > 4000 and x > w * 0.4 and 60 < cw < 150 and 60 < ch < 150:
                    aspect = float(cw) / ch
                    if 0.7 < aspect < 1.4:
                        hull = cv2.convexHull(contour)
                        hull_area = cv2.contourArea(hull)
                        solidity = area / hull_area if hull_area > 0 else 0
                        
                        if solidity > 0.6:
                            results.append({
                                'method': 'dark_region',
                                'x': x,
                                'y': y,
                                'width': cw,
                                'height': ch,
                                'area': area,
                                'is_dark': True,
                                'confidence': 0.85
                            })
                            
        except Exception as e:
            self.logger.warning(f"暗色区域检测失败: {e}")
        
        return results
    
    def _detect_by_gradient(self, gray: np.ndarray, h: int, w: int) -> Dict:
        """使用梯度分析"""
        col_stds = np.std(gray, axis=0)
        kernel_smooth = np.ones(5) / 5
        col_stds_smooth = np.convolve(col_stds, kernel_smooth, mode='same')
        gradient = np.gradient(col_stds_smooth)
        
        search_start = int(w * 0.3)
        search_end = w - 50
        
        if search_end <= search_start:
            return None
        
        gradient_max_idx = int(np.argmax(gradient[search_start:search_end]) + search_start)
        
        return {
            'method': 'gradient',
            'x': gradient_max_idx,
            'width': 80,
            'confidence': 0.7
        }
    
    def _select_best_candidate(self, candidates: List[Dict], w: int) -> Dict:
        """选择最佳候选结果"""
        for c in candidates:
            score = c['confidence']
            
            x_ratio = c['x'] / w
            if x_ratio > 0.65:
                score += 0.3
            elif x_ratio > 0.55:
                score += 0.15
            
            if c.get('is_dark', False):
                score += 0.25
            
            if c.get('area', 0) > 5000:
                score += 0.1
            
            if c['method'] == 'dark_region':
                score += 0.15
            
            c['score'] = score
        
        candidates.sort(key=lambda x: x['score'], reverse=True)
        return candidates[0]
