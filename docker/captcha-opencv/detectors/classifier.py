"""
验证码形状分类器

通过分析滑块区域或背景图特征，判断验证码类型
"""

import logging
from typing import Optional, Dict, Tuple
import cv2
import numpy as np

logger = logging.getLogger(__name__)


class ShapeClassifier:
    """
    验证码形状分类器
    
    支持的形状类型:
    - rectangle: 矩形缺口（标准滑块验证码）
    - triangle: 三角形缺口
    - hexagon: 六边形/八边形缺口
    - polygon_puzzle: 多边形拼图（两个形状，需要将右侧拖到左侧）
    - unknown: 无法识别
    """
    
    SHAPE_RECTANGLE = 'rectangle'
    SHAPE_TRIANGLE = 'triangle'
    SHAPE_HEXAGON = 'hexagon'
    SHAPE_POLYGON_PUZZLE = 'polygon_puzzle'
    SHAPE_UNKNOWN = 'unknown'
    
    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)
    
    def classify(self, background: np.ndarray,
                 slider_position: Optional[Dict] = None) -> str:
        """
        分类验证码形状类型
        
        Args:
            background: BGR 格式的背景图像
            slider_position: 滑块位置信息 {'x': int, 'y': int, 'w': int, 'h': int}
            
        Returns:
            str: 形状类型
        """
        h, w = background.shape[:2]
        
        # 方法1: 如果提供了滑块位置，分析滑块形状
        if slider_position:
            shape = self._classify_by_slider(background, slider_position)
            if shape != self.SHAPE_UNKNOWN:
                self.logger.info(f"通过滑块分类: {shape}")
                return shape
        
        # 方法2: 分析背景图特征
        shape = self._classify_by_features(background)
        self.logger.info(f"通过特征分类: {shape}")
        return shape
    
    def _classify_by_slider(self, background: np.ndarray,
                            slider_pos: Dict) -> str:
        """
        通过分析滑块区域的形状进行分类
        """
        try:
            x = int(slider_pos.get('x', 0))
            y = int(slider_pos.get('y', 0))
            sw = int(slider_pos.get('w', 60))
            sh = int(slider_pos.get('h', 60))
            
            h, w = background.shape[:2]
            
            # 边界检查
            if x < 0 or y < 0 or x + sw > w or y + sh > h:
                return self.SHAPE_UNKNOWN
            
            # 提取滑块区域
            slider_region = background[y:y+sh, x:x+sw]
            
            # 分析滑块形状
            vertices = self._detect_polygon_vertices(slider_region)
            
            self.logger.info(f"滑块区域顶点数: {vertices}")
            
            if vertices == 3:
                return self.SHAPE_TRIANGLE
            elif vertices == 4:
                return self.SHAPE_RECTANGLE
            elif 5 <= vertices <= 8:
                return self.SHAPE_HEXAGON
            else:
                return self.SHAPE_UNKNOWN
                
        except Exception as e:
            self.logger.warning(f"滑块分类失败: {e}")
            return self.SHAPE_UNKNOWN
    
    def _detect_polygon_vertices(self, region: np.ndarray) -> int:
        """
        检测区域内多边形的顶点数
        """
        try:
            gray = cv2.cvtColor(region, cv2.COLOR_BGR2GRAY)
            hsv = cv2.cvtColor(region, cv2.COLOR_BGR2HSV)
            
            # 检测白色边框
            v_channel = hsv[:, :, 2]
            s_channel = hsv[:, :, 1]
            white_mask = ((v_channel > 180) & (s_channel < 60)).astype(np.uint8) * 255
            
            # 形态学操作连接边框
            kernel = np.ones((5, 5), np.uint8)
            closed = cv2.morphologyEx(white_mask, cv2.MORPH_CLOSE, kernel)
            
            # 查找轮廓
            contours, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            # 找最大轮廓
            if not contours:
                return 0
            
            largest = max(contours, key=cv2.contourArea)
            area = cv2.contourArea(largest)
            
            if area < 100:
                return 0
            
            # 多边形近似
            epsilon = 0.02 * cv2.arcLength(largest, True)
            approx = cv2.approxPolyDP(largest, epsilon, True)
            
            return len(approx)
            
        except Exception as e:
            self.logger.warning(f"顶点检测失败: {e}")
            return 0
    
    def _classify_by_features(self, background: np.ndarray) -> str:
        """
        通过分析背景图特征进行分类
        """
        try:
            h, w = background.shape[:2]
            gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
            
            # 检测是否是拼图类型（两个相似形状）
            is_puzzle = self._check_puzzle_type(background)
            if is_puzzle:
                return self.SHAPE_POLYGON_PUZZLE
            
            # 使用边缘检测分析主要形状
            edges = cv2.Canny(gray, 50, 150)
            contours, _ = cv2.findContours(edges, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
            
            # 分析图像下半部分的主要形状
            shape_votes = {3: 0, 4: 0, 6: 0}  # 三角形、矩形、六边形
            
            for contour in contours:
                area = cv2.contourArea(contour)
                if area < 1000:
                    continue
                
                x, y, cw, ch = cv2.boundingRect(contour)
                
                # 只分析可能是缺口的区域
                if y > h * 0.3 and cw > 30 and ch > 30:
                    epsilon = 0.02 * cv2.arcLength(contour, True)
                    approx = cv2.approxPolyDP(contour, epsilon, True)
                    
                    n = len(approx)
                    if n == 3:
                        shape_votes[3] += area
                    elif n == 4:
                        shape_votes[4] += area
                    elif 5 <= n <= 8:
                        shape_votes[6] += area
            
            # 选择得票最高的形状
            if not any(shape_votes.values()):
                return self.SHAPE_UNKNOWN
            
            best_shape = max(shape_votes, key=shape_votes.get)
            
            if shape_votes[best_shape] == 0:
                return self.SHAPE_UNKNOWN
            elif best_shape == 3:
                return self.SHAPE_TRIANGLE
            elif best_shape == 4:
                return self.SHAPE_RECTANGLE
            else:
                return self.SHAPE_HEXAGON
                
        except Exception as e:
            self.logger.warning(f"特征分类失败: {e}")
            return self.SHAPE_UNKNOWN
    
    def _check_puzzle_type(self, background: np.ndarray) -> bool:
        """
        检测是否是拼图类型（背景图上有两个相似的多边形形状）
        """
        try:
            h, w = background.shape[:2]
            gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
            hsv = cv2.cvtColor(background, cv2.COLOR_BGR2HSV)
            
            # 使用边缘 + 白色边框组合检测
            edges = cv2.Canny(gray, 100, 200)
            v_channel = hsv[:, :, 2]
            s_channel = hsv[:, :, 1]
            
            white_mask = ((v_channel > 160) & (s_channel < 80)).astype(np.uint8) * 255
            white_dilated = cv2.dilate(white_mask, np.ones((15, 15), np.uint8))
            
            combined = cv2.bitwise_and(edges, white_dilated)
            combined[:int(h * 0.45), :] = 0  # 只分析下半部分
            
            kernel = np.ones((11, 11), np.uint8)
            closed = cv2.morphologyEx(combined, cv2.MORPH_CLOSE, kernel)
            
            contours, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            # 筛选有效形状
            shapes = []
            for contour in contours:
                area = cv2.contourArea(contour)
                x, y, cw, ch = cv2.boundingRect(contour)
                
                if area > 1000 and cw > 40 and ch > 40:
                    aspect = float(cw) / ch
                    if 0.5 < aspect < 2.0:
                        shapes.append({'x': x, 'area': area})
            
            shapes.sort(key=lambda s: s['x'])
            
            # 如果检测到两个相距足够远的形状，判定为拼图类型
            if len(shapes) >= 2:
                left = shapes[0]
                right = shapes[-1]
                if right['x'] - left['x'] > 100:
                    self.logger.info(f"检测到拼图类型: 左侧 x={left['x']}, 右侧 x={right['x']}")
                    return True
            
            return False
            
        except Exception as e:
            self.logger.warning(f"拼图类型检测失败: {e}")
            return False
