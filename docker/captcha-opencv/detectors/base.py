"""
验证码检测基类

定义所有形状检测器的通用接口和数据结构
"""

import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional, Tuple
import numpy as np

logger = logging.getLogger(__name__)


@dataclass
class DetectionResult:
    """检测结果数据类"""
    offset: int                    # 缺口左边缘 X 坐标
    gap_center: int                # 缺口中心 X 坐标
    gap_width: int                 # 缺口宽度
    confidence: float              # 置信度 (0-1)
    method: str                    # 使用的检测方法
    shape_type: str                # 形状类型
    success: bool = True           # 是否成功
    
    def to_dict(self) -> dict:
        return {
            'offset': self.offset,
            'gap_center': self.gap_center,
            'gap_width': self.gap_width,
            'confidence': self.confidence,
            'method': self.method,
            'shape_type': self.shape_type,
            'success': self.success,
        }


class BaseDetector(ABC):
    """
    验证码检测器基类
    
    所有形状特定的检测器都应继承此类并实现 detect 方法
    """
    
    # 默认置信度阈值
    DEFAULT_CONFIDENCE_THRESHOLD = 0.6
    
    # 形状类型标识
    SHAPE_TYPE = 'unknown'
    
    def __init__(self, confidence_threshold: float = None):
        self.confidence_threshold = confidence_threshold or self.DEFAULT_CONFIDENCE_THRESHOLD
        self.logger = logging.getLogger(self.__class__.__name__)
    
    @abstractmethod
    def detect(self, background: np.ndarray) -> DetectionResult:
        """
        检测缺口位置
        
        Args:
            background: BGR 格式的背景图像
            
        Returns:
            DetectionResult: 检测结果
        """
        pass
    
    def can_detect(self, background: np.ndarray) -> bool:
        """
        判断当前检测器是否适用于该图像
        
        子类可以重写此方法以实现更精确的判断
        """
        return True
    
    def preprocess(self, image: np.ndarray) -> np.ndarray:
        """
        图像预处理
        
        子类可以重写此方法以实现特定的预处理逻辑
        """
        return image
    
    def create_failed_result(self, method: str = 'unknown') -> DetectionResult:
        """创建失败结果"""
        return DetectionResult(
            offset=0,
            gap_center=0,
            gap_width=0,
            confidence=0.0,
            method=method,
            shape_type=self.SHAPE_TYPE,
            success=False
        )
    
    def create_result(self, offset: int, gap_center: int, gap_width: int,
                      confidence: float, method: str) -> DetectionResult:
        """创建成功结果"""
        return DetectionResult(
            offset=offset,
            gap_center=gap_center,
            gap_width=gap_width,
            confidence=confidence,
            method=method,
            shape_type=self.SHAPE_TYPE,
            success=True
        )
