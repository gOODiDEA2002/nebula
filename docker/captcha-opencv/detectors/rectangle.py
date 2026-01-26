"""
矩形缺口检测器

用于检测标准的矩形滑块验证码缺口
"""

import cv2
import numpy as np
from typing import List, Dict
from .base import BaseDetector, DetectionResult


class RectangleDetector(BaseDetector):
    """
    矩形缺口检测器
    
    检测策略:
    1. 多边形轮廓检测（4边形）
    2. 垂直边缘检测（霍夫变换）
    3. 梯度分析
    """
    
    SHAPE_TYPE = 'rectangle'
    DEFAULT_CONFIDENCE_THRESHOLD = 0.6
    
    # Canny 阈值组合
    CANNY_THRESHOLDS = [(50, 100), (80, 160), (100, 200)]
    
    def detect(self, background: np.ndarray) -> DetectionResult:
        """检测矩形缺口位置"""
        try:
            gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
            h, w = gray.shape
            
            candidates = []
            
            # 方法1: 多边形轮廓检测
            contour_results = self._detect_by_contour(gray, h, w)
            candidates.extend(contour_results)
            
            # 方法2: 垂直边缘检测
            edge_results = self._detect_by_vertical_edges(gray, h, w)
            candidates.extend(edge_results)
            
            # 方法3: 梯度分析
            gradient_result = self._detect_by_gradient(gray, h, w)
            if gradient_result:
                candidates.append(gradient_result)
            
            if not candidates:
                return self.create_failed_result('no_candidates')
            
            # 综合评分选择最佳结果
            best = self._select_best_candidate(candidates, w)
            
            return self.create_result(
                offset=best['x'],
                gap_center=best['x'] + best.get('width', 80) // 2,
                gap_width=best.get('width', 80),
                confidence=best['confidence'],
                method=best['method']
            )
            
        except Exception as e:
            self.logger.error(f"矩形检测失败: {e}")
            return self.create_failed_result('error')
    
    def _detect_by_contour(self, gray: np.ndarray, h: int, w: int) -> List[Dict]:
        """使用轮廓检测矩形"""
        results = []
        
        for canny_thresh in self.CANNY_THRESHOLDS:
            edges = cv2.Canny(gray, canny_thresh[0], canny_thresh[1])
            
            # 闭运算连接边缘
            kernel = np.ones((3, 3), np.uint8)
            closed = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)
            
            contours, _ = cv2.findContours(closed, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
            
            for contour in contours:
                epsilon = 0.02 * cv2.arcLength(contour, True)
                approx = cv2.approxPolyDP(contour, epsilon, True)
                
                # 只处理 4 边形
                if len(approx) != 4:
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
                
                # 检测内部是否暗色
                roi_gray = gray[y:y+ch, x:x+cw]
                mean_brightness = np.mean(roi_gray)
                img_mean = np.mean(gray)
                is_dark = mean_brightness < img_mean * 0.6
                
                # 计算置信度
                base_confidence = min(0.8, edge_density * 4)
                dark_bonus = 0.3 if is_dark else 0
                confidence = min(1.0, base_confidence + 0.1 + dark_bonus)
                
                if edge_density > 0.04:
                    results.append({
                        'method': f'contour_{canny_thresh}_rect',
                        'x': x,
                        'y': y,
                        'width': cw,
                        'height': ch,
                        'area': area,
                        'is_dark': is_dark,
                        'confidence': confidence
                    })
        
        return results
    
    def _detect_by_vertical_edges(self, gray: np.ndarray, h: int, w: int) -> List[Dict]:
        """使用垂直边缘检测"""
        results = []
        
        edges = cv2.Canny(gray, 100, 200)
        lines = cv2.HoughLinesP(edges, 1, np.pi / 180, 50, minLineLength=50, maxLineGap=5)
        
        if lines is None:
            return results
        
        vertical_lines_x = []
        for line in lines:
            x1, y1, x2, y2 = line[0]
            if abs(x1 - x2) < 5:  # 垂直线
                avg_x = (x1 + x2) // 2
                length = abs(y2 - y1)
                if length > 60 and avg_x > w * 0.4:
                    vertical_lines_x.append(int(avg_x))
        
        if not vertical_lines_x:
            return results
        
        vertical_lines_x.sort()
        
        for i in range(len(vertical_lines_x) - 1):
            gap = vertical_lines_x[i + 1] - vertical_lines_x[i]
            if 60 < gap < 110:
                results.append({
                    'method': 'vertical_edges',
                    'x': vertical_lines_x[i],
                    'width': gap,
                    'confidence': 0.9
                })
        
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
            
            # 位置加分
            x_ratio = c['x'] / w
            if x_ratio > 0.65:
                score += 0.3
            elif x_ratio > 0.55:
                score += 0.15
            
            # 暗色内部加分
            if c.get('is_dark', False):
                score += 0.25
            
            # 面积加分
            if c.get('area', 0) > 5000:
                score += 0.1
            
            c['score'] = score
        
        candidates.sort(key=lambda x: x['score'], reverse=True)
        return candidates[0]
