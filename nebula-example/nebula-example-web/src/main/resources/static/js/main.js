/**
 * Nebula Web Example - JavaScript
 */

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    console.log('Nebula Web Example loaded');
    
    // 添加页面加载动画
    document.body.classList.add('loaded');
});

// API 调用示例
async function fetchApiInfo() {
    try {
        const response = await fetch('/api/info');
        const data = await response.json();
        console.log('System Info:', data);
        return data;
    } catch (error) {
        console.error('Failed to fetch API info:', error);
    }
}

// 健康检查
async function checkHealth() {
    try {
        const response = await fetch('/actuator/health');
        const data = await response.json();
        console.log('Health Status:', data.status);
        return data;
    } catch (error) {
        console.error('Health check failed:', error);
    }
}
