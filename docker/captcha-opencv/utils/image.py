"""
图像处理工具
"""

import cv2
import numpy as np
import base64
from typing import Optional, Tuple


class ImageUtils:
    """图像处理工具类"""
    
    @staticmethod
    def decode_base64(base64_str: str) -> np.ndarray:
        """解码 Base64 图像"""
        if base64_str.startswith('data:image'):
            base64_str = base64_str.split(',')[1]
        
        img_data = base64.b64decode(base64_str)
        nparr = np.frombuffer(img_data, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if img is None:
            raise ValueError("无法解码图像")
        
        return img
    
    @staticmethod
    def encode_base64(img: np.ndarray, format: str = '.png') -> str:
        """编码图像为 Base64"""
        _, buffer = cv2.imencode(format, img)
        return base64.b64encode(buffer).decode('utf-8')
    
    @staticmethod
    def to_grayscale(img: np.ndarray) -> np.ndarray:
        """转换为灰度图"""
        if len(img.shape) == 2:
            return img
        return cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    @staticmethod
    def to_hsv(img: np.ndarray) -> np.ndarray:
        """转换为 HSV 色彩空间"""
        return cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    
    @staticmethod
    def canny_edge(gray: np.ndarray, low: int = 50, high: int = 150) -> np.ndarray:
        """Canny 边缘检测"""
        return cv2.Canny(gray, low, high)
    
    @staticmethod
    def morphology_close(img: np.ndarray, kernel_size: int = 3) -> np.ndarray:
        """形态学闭操作"""
        kernel = np.ones((kernel_size, kernel_size), np.uint8)
        return cv2.morphologyEx(img, cv2.MORPH_CLOSE, kernel)
    
    @staticmethod
    def morphology_open(img: np.ndarray, kernel_size: int = 3) -> np.ndarray:
        """形态学开操作"""
        kernel = np.ones((kernel_size, kernel_size), np.uint8)
        return cv2.morphologyEx(img, cv2.MORPH_OPEN, kernel)
    
    @staticmethod
    def save_debug_image(img: np.ndarray, path: str):
        """保存调试图像"""
        cv2.imwrite(path, img)
    
    @staticmethod
    def draw_detection_result(img: np.ndarray, x: int, gap_width: int,
                              gap_center: int = None, y: int = None,
                              height: int = None) -> np.ndarray:
        """在图像上绘制检测结果"""
        result = img.copy()
        h, w = result.shape[:2]
        
        # 画缺口左边缘（红色）
        cv2.line(result, (x, 0), (x, h), (0, 0, 255), 2)
        
        # 画缺口中心（绿色）
        if gap_center:
            cv2.line(result, (gap_center, 0), (gap_center, h), (0, 255, 0), 2)
        
        # 画缺口区域矩形（蓝色）
        if y is not None and height is not None:
            cv2.rectangle(result, (x, y), (x + gap_width, y + height), (255, 0, 0), 2)
        
        return result
