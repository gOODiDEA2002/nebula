"""
缩放计算工具
"""

from dataclasses import dataclass
from typing import Optional


@dataclass
class ScalingResult:
    """缩放计算结果"""
    original_offset: int           # 原始图片上的偏移量
    scaled_offset: int             # 缩放后的偏移量
    original_gap_center: int       # 原始图片上的缺口中心
    scaled_gap_center: int         # 缩放后的缺口中心
    original_width: int            # 原始图片宽度
    target_width: int              # 目标宽度
    scale_ratio: float             # 缩放比例


class ScalingUtils:
    """缩放计算工具类"""
    
    @staticmethod
    def calculate(original_offset: int, gap_center: int, gap_width: int,
                  original_width: int, target_width: Optional[int] = None) -> ScalingResult:
        """
        计算缩放后的坐标
        
        Args:
            original_offset: 原始图片上的缺口左边缘 X 坐标
            gap_center: 原始图片上的缺口中心 X 坐标
            gap_width: 原始图片上的缺口宽度
            original_width: 原始图片宽度
            target_width: 目标显示宽度
            
        Returns:
            ScalingResult: 缩放计算结果
        """
        if target_width and target_width > 0 and target_width != original_width:
            scale_ratio = target_width / original_width
            scaled_offset = int(original_offset * scale_ratio)
            scaled_gap_center = int(gap_center * scale_ratio)
        else:
            scale_ratio = 1.0
            scaled_offset = original_offset
            scaled_gap_center = gap_center
            target_width = original_width
        
        return ScalingResult(
            original_offset=original_offset,
            scaled_offset=scaled_offset,
            original_gap_center=gap_center,
            scaled_gap_center=scaled_gap_center,
            original_width=original_width,
            target_width=target_width,
            scale_ratio=scale_ratio
        )
    
    @staticmethod
    def scale_value(value: int, original_width: int,
                    target_width: Optional[int] = None) -> int:
        """缩放单个值"""
        if target_width and target_width > 0 and target_width != original_width:
            return int(value * target_width / original_width)
        return value
