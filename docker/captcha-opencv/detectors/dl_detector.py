"""
深度学习滑块验证码检测器

基于 ONNX 模型进行实例分割，检测滑块缺口位置
参考: https://github.com/chenwei-zhao/captcha-recognizer
"""

import logging
import os
from typing import List, Tuple, Union

import cv2
import numpy as np

try:
    import onnxruntime as ort
    ONNX_AVAILABLE = True
except ImportError:
    ONNX_AVAILABLE = False
    ort = None

from .base import BaseDetector, DetectionResult

logger = logging.getLogger(__name__)


class DLDetector(BaseDetector):
    """
    深度学习滑块验证码检测器
    
    使用 ONNX 模型进行实例分割，支持检测各种形状的滑块缺口。
    特点：
    - 高准确率（92%+ 置信度）
    - 泛化能力强
    - 支持矩形、六边形、三角形等多种形状
    """
    
    SHAPE_TYPE = 'dl_detected'
    DEFAULT_CONFIDENCE_THRESHOLD = 0.5
    
    # 模型配置
    CONF_THRESHOLD = 0.5
    IOU_THRESHOLD = 0.8
    
    def __init__(self, model_path: str = None, confidence_threshold: float = None):
        super().__init__(confidence_threshold)
        
        if not ONNX_AVAILABLE:
            self.logger.warning("onnxruntime 未安装，深度学习检测器不可用")
            self.session = None
            return
        
        # 默认模型路径
        if model_path is None:
            root_dir = os.path.dirname(os.path.dirname(__file__))
            model_path = os.path.join(root_dir, 'models', 'slider.onnx')
        
        if not os.path.exists(model_path):
            self.logger.warning(f"模型文件不存在: {model_path}")
            self.session = None
            return
        
        try:
            # 初始化 ONNX 运行时
            providers = ["CUDAExecutionProvider", "CPUExecutionProvider"] if ort.get_device() == 'GPU' else ["CPUExecutionProvider"]
            self.session = ort.InferenceSession(model_path, providers=providers)
            self.logger.info(f"深度学习模型加载成功: {model_path}")
        except Exception as e:
            self.logger.error(f"模型加载失败: {e}")
            self.session = None
    
    def is_available(self) -> bool:
        """检查检测器是否可用"""
        return self.session is not None
    
    def detect(self, background: np.ndarray) -> DetectionResult:
        """检测缺口位置"""
        if not self.is_available():
            return self.create_failed_result('model_unavailable')
        
        try:
            # 预处理
            prep_img = self._preprocess(background, (640, 640))
            
            # 推理
            outs = self.session.run(None, {self.session.get_inputs()[0].name: prep_img})
            
            # 后处理
            results = self._postprocess(background, prep_img, outs)
            
            if not results or len(results[0][0]) == 0:
                return self.create_failed_result('no_detection')
            
            boxes, masks = results[0]
            
            # 如果检测到多个目标，选择 X 值最小的（最左侧的缺口）
            if len(boxes) == 1:
                box = boxes[0].tolist()
            else:
                box = min(boxes, key=lambda x: x[0]).tolist()
            
            x1, y1, x2, y2, score, class_id = box[:6]
            
            gap_width = int(x2 - x1)
            gap_center = int((x1 + x2) / 2)
            
            self.logger.info(f"深度学习检测: x1={x1:.1f}, center={gap_center}, width={gap_width}, conf={score:.3f}")
            
            return self.create_result(
                offset=int(x1),
                gap_center=gap_center,
                gap_width=gap_width,
                confidence=float(score),
                method='dl_onnx'
            )
            
        except Exception as e:
            self.logger.error(f"深度学习检测失败: {e}")
            return self.create_failed_result('error')
    
    def _preprocess(self, img: np.ndarray, new_shape: Tuple[int, int]) -> np.ndarray:
        """图像预处理"""
        img = self._letterbox(img, new_shape)
        img = img[..., ::-1].transpose([2, 0, 1])[None]
        img = np.ascontiguousarray(img)
        img = img.astype(np.float32) / 255
        return img
    
    @staticmethod
    def _letterbox(img: np.ndarray, new_shape: Tuple[int, int] = (640, 640)) -> np.ndarray:
        """调整图像大小并保持宽高比"""
        shape = img.shape[:2]
        r = min(new_shape[0] / shape[0], new_shape[1] / shape[1])
        new_unpad = int(round(shape[1] * r)), int(round(shape[0] * r))
        new_unpad = (max(1, min(new_unpad[0], new_shape[1])),
                     max(1, min(new_unpad[1], new_shape[0])))
        
        if shape[::-1] != new_unpad:
            img = cv2.resize(img, new_unpad, interpolation=cv2.INTER_LINEAR)
        
        dw, dh = new_shape[1] - new_unpad[0], new_shape[0] - new_unpad[1]
        top, bottom = int(round(dh / 2)), int(round(dh / 2))
        left, right = int(round(dw / 2)), int(round(dw / 2))
        
        img = cv2.copyMakeBorder(img, top, bottom, left, right, cv2.BORDER_CONSTANT, value=(114, 114, 114))
        
        if img.shape[0] != new_shape[0] or img.shape[1] != new_shape[1]:
            img = cv2.resize(img, new_shape, interpolation=cv2.INTER_LINEAR)
        
        return img
    
    def _postprocess(self, img: np.ndarray, prep_img: np.ndarray, outs: List) -> List:
        """后处理"""
        preds, protos = outs
        preds = self._non_max_suppression(preds, self.CONF_THRESHOLD, self.IOU_THRESHOLD)
        
        results = []
        for i, pred in enumerate(preds):
            pred[:, :4] = self._scale_boxes(prep_img.shape[2:], pred[:, :4], img.shape)
            masks = self._process_mask(protos[i], pred[:, 6:], pred[:, :4], img.shape[:2])
            results.append([pred[:, :6], masks])
        
        return results
    
    def _scale_boxes(self, img1_shape: Tuple[int, int], boxes: np.ndarray, 
                     img0_shape: Tuple[int, int]) -> np.ndarray:
        """缩放边界框"""
        gain = min(img1_shape[0] / img0_shape[0], img1_shape[1] / img0_shape[1])
        pad = (
            round((img1_shape[1] - img0_shape[1] * gain) / 2),
            round((img1_shape[0] - img0_shape[0] * gain) / 2),
        )
        
        boxes[..., 0] -= pad[0]
        boxes[..., 1] -= pad[1]
        boxes[..., 2] -= pad[0]
        boxes[..., 3] -= pad[1]
        boxes[..., :4] /= gain
        
        # Clip
        boxes[..., [0, 2]] = np.clip(boxes[..., [0, 2]], 0, img0_shape[1])
        boxes[..., [1, 3]] = np.clip(boxes[..., [1, 3]], 0, img0_shape[0])
        
        return boxes
    
    def _process_mask(self, protos: np.ndarray, masks_in: np.ndarray, 
                      bboxes: np.ndarray, shape: Tuple[int, int]) -> np.ndarray:
        """处理掩码"""
        c, mh, mw = protos.shape
        masks = (masks_in @ protos.reshape(c, -1)).reshape(-1, mh, mw)
        masks = self._scale_masks(masks, shape)
        masks = self._crop_mask(masks, bboxes)
        return masks > 0.0
    
    @staticmethod
    def _scale_masks(masks: np.ndarray, shape: Tuple[int, int]) -> np.ndarray:
        """缩放掩码"""
        mh, mw = masks.shape[1:]
        gain = min(mh / shape[0], mw / shape[1])
        pad = [mw - shape[1] * gain, mh - shape[0] * gain]
        pad[0] /= 2
        pad[1] /= 2
        
        top, left = int(round(pad[1])), int(round(pad[0]))
        bottom, right = mh - int(round(pad[1])), mw - int(round(pad[0]))
        
        masks_cropped = masks[:, top:bottom, left:right]
        
        resized_masks = np.zeros((masks_cropped.shape[0], shape[0], shape[1]), dtype=masks_cropped.dtype)
        for i, mask in enumerate(masks_cropped):
            resized_masks[i] = cv2.resize(mask, (shape[1], shape[0]), interpolation=cv2.INTER_LINEAR)
        
        return resized_masks
    
    @staticmethod
    def _crop_mask(masks: np.ndarray, boxes: np.ndarray) -> np.ndarray:
        """裁剪掩码到边界框"""
        _, h, w = masks.shape
        boxes = boxes[:, :, None] if boxes.ndim == 2 else boxes
        x1, y1, x2, y2 = np.split(boxes, 4, axis=1)
        r = np.arange(w, dtype=x1.dtype)[None, None, :]
        c = np.arange(h, dtype=x1.dtype)[None, :, None]
        return masks * ((r >= x1) * (r < x2) * (c >= y1) * (c < y2))
    
    @staticmethod
    def _xywh2xyxy(x: np.ndarray) -> np.ndarray:
        """xywh 转 xyxy 格式"""
        y = np.empty_like(x, dtype=np.float32)
        xy = x[..., :2]
        wh = x[..., 2:] / 2
        y[..., :2] = xy - wh
        y[..., 2:] = xy + wh
        return y
    
    def _non_max_suppression(self, prediction: np.ndarray, conf_thres: float = 0.25,
                             iou_thres: float = 0.45, max_det: int = 300) -> List:
        """非极大值抑制"""
        nc = 1  # 类别数
        
        if isinstance(prediction, (list, tuple)):
            prediction = prediction[0]
        
        bs = prediction.shape[0]
        mi = 4 + nc
        xc = np.amax(prediction[:, 4:mi], axis=1) > conf_thres
        
        prediction = np.transpose(prediction, (0, 2, 1))
        prediction[..., :4] = self._xywh2xyxy(prediction[..., :4])
        
        output = [np.zeros((0, 6 + prediction.shape[-1] - 4 - nc), dtype=np.float32)] * bs
        
        for xi, x in enumerate(prediction):
            x = x[xc[xi]]
            
            if not x.shape[0]:
                continue
            
            box, cls, mask = np.split(x, [4, 4 + nc], axis=1)
            
            conf = np.amax(cls, axis=1, keepdims=True)
            j = np.argmax(cls, axis=1, keepdims=True)
            filt = conf.squeeze(-1) > conf_thres
            x = np.concatenate((box, conf, j.astype(np.float32), mask), axis=1)[filt]
            
            n = x.shape[0]
            if not n:
                continue
            
            # NMS
            scores = x[:, 4]
            boxes = x[:, :4]
            
            i = []
            if boxes.shape[0] > 0:
                y1, x1, y2, x2 = boxes[:, 1], boxes[:, 0], boxes[:, 3], boxes[:, 2]
                area = (x2 - x1) * (y2 - y1)
                order = scores.argsort()[::-1]
                
                while order.size > 0:
                    idx = order[0]
                    i.append(idx)
                    xx1 = np.maximum(x1[idx], x1[order[1:]])
                    yy1 = np.maximum(y1[idx], y1[order[1:]])
                    xx2 = np.minimum(x2[idx], x2[order[1:]])
                    yy2 = np.minimum(y2[idx], y2[order[1:]])
                    w = np.maximum(0.0, xx2 - xx1)
                    h = np.maximum(0.0, yy2 - yy1)
                    inter = w * h
                    iou = inter / (area[idx] + area[order[1:]] - inter)
                    order = order[np.where(iou <= iou_thres)[0] + 1]
            
            i = np.array(i)[:max_det]
            output[xi] = x[i]
        
        return output
