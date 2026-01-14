#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ddddocr 验证码识别服务

提供以下功能：
1. 通用验证码识别 - /ocr/b64/text
2. 服务健康检查 - /ping
3. 测试样本获取 - /test/sample (用于验证服务可用性)
4. 测试页面 - /test/page

作者: Nebula Team
版本: 1.0.0
"""

import base64
import logging
import os
import io
import random
from typing import Optional

import ddddocr
from flask import Flask, jsonify, request, send_file, render_template_string
from PIL import Image, ImageDraw, ImageFont

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 初始化 ddddocr (懒加载或启动时加载，这里启动时加载以快速响应)
# show_ad=False 关闭广告打印
ocr = ddddocr.DdddOcr(show_ad=False)
logger.info("ddddocr 模型加载完成")


def decode_base64_image(base64_str: str) -> Optional[bytes]:
    """
    解码 Base64 图片
    """
    try:
        # 移除可能的 data:image 前缀
        if ',' in base64_str:
            base64_str = base64_str.split(',')[1]
        
        return base64.b64decode(base64_str)
    except Exception as e:
        logger.error(f"解码 Base64 图片失败: {e}")
        return None

def generate_sample_captcha():
    """
    生成一个简单的测试验证码图片 (纯PIL生成，不依赖外部样本)
    """
    width, height = 120, 40
    image = Image.new('RGB', (width, height), (255, 255, 255))
    draw = ImageDraw.Draw(image)
    
    # 生成随机字符
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    code = "".join(random.sample(chars, 4))
    
    # 简单的绘制，不依赖字体文件，使用默认字体
    try:
        font = ImageFont.truetype("arial.ttf", 30)
    except:
        font = ImageFont.load_default()
        
    draw.text((10, 5), code, font=font, fill=(0, 0, 0))
    
    # 添加噪点
    for _ in range(50):
        x = random.randint(0, width)
        y = random.randint(0, height)
        draw.point((x, y), fill=(random.randint(0, 200), random.randint(0, 200), random.randint(0, 200)))
        
    buf = io.BytesIO()
    image.save(buf, format='JPEG')
    return buf.getvalue(), code


@app.route('/ping', methods=['GET'])
def ping():
    """健康检查"""
    return jsonify({
        'status': 'ok',
        'service': 'captcha-ddddocr',
        'version': '1.0.0'
    })


@app.route('/ocr/b64/text', methods=['POST'])
def ocr_b64_text():
    """
    Base64 图片识别接口
    """
    try:
        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({
                'success': False,
                'error': '缺少 image 参数'
            }), 400
            
        image_bytes = decode_base64_image(data['image'])
        if not image_bytes:
            return jsonify({
                'success': False,
                'error': '图片解码失败'
            }), 400
            
        # 识别
        res = ocr.classification(image_bytes)
        logger.info(f"识别成功: {res}")
        
        return jsonify({
            'success': True,
            'result': res
        })
        
    except Exception as e:
        logger.exception("识别异常")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/test/sample', methods=['GET'])
def test_sample():
    """
    获取测试样本 (返回 Base64 和 预期结果)
    """
    try:
        img_bytes, code = generate_sample_captcha()
        b64_str = base64.b64encode(img_bytes).decode('utf-8')
        
        return jsonify({
            'success': True,
            'image': b64_str,
            'code': code
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
        <title>ddddocr 测试页面</title>
        <style>
            body { font-family: sans-serif; padding: 20px; }
            .container { max-width: 600px; margin: 0 auto; border: 1px solid #ccc; padding: 20px; border-radius: 8px; }
            img { border: 1px solid #ddd; margin-bottom: 10px; }
            button { padding: 10px 20px; cursor: pointer; background-color: #007bff; color: white; border: none; border-radius: 4px; }
            button:hover { background-color: #0056b3; }
            .result { margin-top: 20px; font-weight: bold; }
            pre { background: #f8f9fa; padding: 10px; border-radius: 4px; }
        </style>
    </head>
    <body>
        <div class="container">
            <h2>ddddocr 通用验证码识别测试</h2>
            
            <div>
                <h3>1. 获取验证码样本</h3>
                <img id="captcha-img" src="" alt="验证码" style="display:none; max-width: 100%;">
                <br>
                <button onclick="getSample()">刷新验证码</button>
            </div>
            
            <div>
                <h3>2. 识别验证码</h3>
                <button onclick="solveCaptcha()">识别</button>
                <div class="result" id="result-area"></div>
            </div>
            
            <div>
                <h3>API 响应</h3>
                <pre id="api-response"></pre>
            </div>
        </div>

        <script>
            let currentImageBase64 = '';

            async function getSample() {
                try {
                    const res = await fetch('/test/sample');
                    const data = await res.json();
                    if (data.success) {
                        currentImageBase64 = data.image;
                        const img = document.getElementById('captcha-img');
                        img.src = 'data:image/jpeg;base64,' + data.image;
                        img.style.display = 'block';
                        document.getElementById('result-area').innerText = '预期结果: ' + data.code;
                        document.getElementById('api-response').innerText = '样本获取成功';
                    } else {
                        alert('获取失败: ' + data.error);
                    }
                } catch (e) {
                    alert('请求异常: ' + e);
                }
            }

            async function solveCaptcha() {
                if (!currentImageBase64) {
                    alert('请先获取验证码');
                    return;
                }
                
                try {
                    document.getElementById('result-area').innerText += ' | 识别中...';
                    const res = await fetch('/ocr/b64/text', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ image: currentImageBase64 })
                    });
                    const data = await res.json();
                    document.getElementById('api-response').innerText = JSON.stringify(data, null, 2);
                    
                    if (data.success) {
                        document.getElementById('result-area').innerText += ' | 识别结果: ' + data.result;
                    } else {
                        document.getElementById('result-area').innerText += ' | 识别失败';
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
    port = int(os.environ.get('PORT', 8866))
    debug = os.environ.get('DEBUG', 'false').lower() == 'true'
    
    logger.info(f"启动 ddddocr 验证码识别服务: port={port}, debug={debug}")
    
    app.run(host='0.0.0.0', port=port, debug=debug)
