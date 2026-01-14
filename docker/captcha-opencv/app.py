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
版本: 1.0.0
"""

import base64
import io
import logging
import os
from typing import Optional, Tuple

import cv2
import numpy as np
from flask import Flask, jsonify, request, render_template_string
from PIL import Image

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)


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
    """
    try:
        # 转换为灰度图
        bg_gray = cv2.cvtColor(background, cv2.COLOR_BGR2GRAY)
        slider_gray = cv2.cvtColor(slider, cv2.COLOR_BGR2GRAY)
        
        # 边缘检测
        bg_edges = cv2.Canny(bg_gray, 100, 200)
        slider_edges = cv2.Canny(slider_gray, 100, 200)
        
        # 模板匹配
        result = cv2.matchTemplate(bg_edges, slider_edges, cv2.TM_CCOEFF_NORMED)
        
        # 获取最佳匹配位置
        min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(result)
        
        # 偏移量为 x 坐标
        offset = max_loc[0]
        confidence = float(max_val)
        
        logger.info(f"滑块缺口检测: offset={offset}, confidence={confidence:.4f}")
        
        return offset, confidence
        
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
    """
    try:
        background_b64 = request.form.get('background')
        slider_b64 = request.form.get('slider')
        
        if not background_b64:
            return jsonify({
                'success': False,
                'error': '缺少 background 参数'
            }), 400
        
        # 解码背景图
        background = decode_base64_image(background_b64)
        if background is None:
            return jsonify({
                'success': False,
                'error': '背景图解码失败'
            }), 400
        
        # 如果有滑块图，使用模板匹配
        if slider_b64:
            slider = decode_base64_image(slider_b64)
            if slider is None:
                return jsonify({
                    'success': False,
                    'error': '滑块图解码失败'
                }), 400
            
            offset, confidence = detect_slider_gap(background, slider)
        else:
            # 只有背景图，使用轮廓检测
            offset, confidence = detect_slider_gap_contour(background)
        
        return jsonify({
            'success': True,
            'offset': offset,
            'confidence': confidence
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
