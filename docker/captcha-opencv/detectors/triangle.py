"""
三角形缺口检测器

用于检测三角形形状的滑块验证码缺口
"""

import cv2
import numpy as np
from typing import List, Dict
from .base import BaseDetector, DetectionResult


class TriangleDetector(BaseDetector):
    """
    三角形缺口检测器
    
    检测策略:
    1. 边缘检测 + 三角形轮廓
    2. 暗色区域检测
    """
    
    SHAPE_TYPE = 'triangle'
    DEFAULT_CONFIDENCE_THRESHOLD = 0.6
    
    def _detect_by_contour(self, gray: np.ndarray, h: int, w: int) -> List[Dict]:
        """使用轮廓检测三角形"""
        results = []
        
        for canny_thresh in [(50, 100), (100, 200)]:
            edges = cv2.Canny(gray, canny_thresh[0], canny_thresh[1])
            
            # 使用较大的核进行闭操作，连接三角形边缘
            kernel = np.ones((5, 5), np.uint8)
            closed = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)
            
            contours, _ = cv2.findContours(closed, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
            
            for contour in contours:
                # 使用较大的 epsilon 来近似三角形
                epsilon = 0.03 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)
                
                # 检测三角形 (3 顶点) 或接近三角形 (3-4 顶点)
                if not (3 <= len(approx) <= 4):
                    continue
                
                x, y, cw, ch = cv2.boundingRect(approx)
                area = cv2.contourArea(contour)
                
                # 过滤条件：放宽位置和尺寸限制
                if not (x > w * 0.25 and 40 < cw < 180 and 40 < ch < 180 and area > 1500):
                    continue
                
                # 三角形的特征：宽高比接近等边三角形
                aspect = float(cw) / ch
                if not (0.6 < aspect < 1.5):
                    continue
                
                # 检测内部亮度
                roi_gray = gray[y:y+ch, x:x+cw]
                mean_brightness = np.mean(roi_gray)
                img_mean = np.mean(gray)
                is_dark = mean_brightness < img_mean * 0.75
                
                # 计算边缘密度
                roi_edges = edges[y:y+ch, x:x+cw]
                edge_density = np.sum(roi_edges > 0) / (cw * ch) if cw * ch > 0 else 0
                
                base_confidence = min(0.8, edge_density * 4)
                dark_bonus = 0.15 if is_dark else 0
                vertex_bonus = 0.1 if len(approx) == 3 else 0
                confidence = min(1.0, base_confidence + dark_bonus + vertex_bonus)
                
                if edge_density > 0.02:
                    results.append({
                        'method': f'contour_triangle',
                        'x': x,
                        'y': y,
                        'width': cw,
                        'height': ch,
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
                
                # 检测三角形
                if len(approx) != 3:
                    continue
                
                area = cv2.contourArea(contour)
                x, y, cw, ch = cv2.boundingRect(contour)
                
                if area > 2000 and x > w * 0.4 and 40 < cw < 120 and 40 < ch < 120:
                    results.append({
                        'method': 'dark_triangle',
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
    
    def _detect_by_gradient(self, gray: np.ndarray, h: int, w: int) -> List[Dict]:
        """使用梯度分析检测缺口位置"""
        results = []
        
        # 列方差分析
        col_stds = np.std(gray, axis=0)
        kernel_smooth = np.ones(5) / 5
        col_stds_smooth = np.convolve(col_stds, kernel_smooth, mode='same')
        gradient = np.gradient(col_stds_smooth)
        
        # 在右半部分搜索
        search_start = int(w * 0.35)
        search_end = w - 50
        
        if search_end > search_start:
            # 找梯度最大的位置
            gradient_max_idx = int(np.argmax(gradient[search_start:search_end]) + search_start)
            
            results.append({
                'method': 'gradient',
                'x': gradient_max_idx,
                'width': 70,
                'confidence': 0.65
            })
        
        return results
    
    def detect(self, background: np.ndarray) -> DetectionResult:
        """检测三角形缺口位置"""
        try:
            gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
            h, w = gray.shape
            
            candidates = []
            
            # 方法1: 边缘检测 + 三角形轮廓
            contour_results = self._detect_by_contour(gray, h, w)
            candidates.extend(contour_results)
            
            # 方法2: 暗色区域检测
            dark_results = self._detect_by_dark_region(background, h, w)
            candidates.extend(dark_results)
            
            # 方法3: 梯度分析（兜底）
            if len(candidates) < 2:
                gradient_results = self._detect_by_gradient(gray, h, w)
                candidates.extend(gradient_results)
            
            if not candidates:
                return self.create_failed_result('no_candidates')
            
            # 选择最佳结果
            best = self._select_best_candidate(candidates, w)
            
            return self.create_result(
                offset=best['x'],
                gap_center=best['x'] + best.get('width', 70) // 2,
                gap_width=best.get('width', 70),
                confidence=best['confidence'],
                method=best['method']
            )
            
        except Exception as e:
            self.logger.error(f"三角形检测失败: {e}")
            return self.create_failed_result('error')
    
    def _select_best_candidate(self, candidates: List[Dict], w: int) -> Dict:
        """选择最佳候选结果"""
        for c in candidates:
            score = c['confidence']
            
            x_ratio = c['x'] / w
            if x_ratio > 0.5:
                score += 0.25
            elif x_ratio > 0.4:
                score += 0.1
            
            if c.get('is_dark', False):
                score += 0.2
            
            c['score'] = score
        
        candidates.sort(key=lambda x: x['score'], reverse=True)
        return candidates[0]
