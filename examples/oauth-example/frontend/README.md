# OAuth Client Frontend

Vocoor OAuth 2.0 客户端示例 - 前端应用

## 项目简介

这是一个基于 Vue 3 + TypeScript 的 OAuth 客户端示例项目，演示如何接入 OAuth 2.0 服务实现第三方登录。

## 技术栈

- Vue 3 (Composition API)
- TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- Sass

## 快速开始

### 1. 环境准备

- Node.js 18+
- npm 或 pnpm

### 2. 安装依赖

```bash
cd /path/to/nebula-projects/example/oauth-client/frontend
npm install
```

### 3. 启动开发服务器

```bash
npm run dev
```

应用将在 http://localhost:5173 启动

### 4. 构建生产版本

```bash
npm run build
```

## 目录结构

```
src/
├── main.ts           # 应用入口
├── App.vue           # 根组件
├── vite-env.d.ts     # 类型声明
├── api/              # API 接口
│   └── oauth.ts      # OAuth 相关 API
├── router/           # 路由配置
│   └── index.ts
├── stores/           # Pinia 状态管理
│   └── user.ts       # 用户状态
├── styles/           # 全局样式
│   └── main.scss
└── views/            # 页面组件
    ├── Home.vue      # 首页（登录入口）
    ├── OAuthResult.vue  # 授权结果页
    └── Dashboard.vue    # 用户中心
```

## 页面说明

### 首页 (/)

- 展示应用信息和登录按钮
- 点击"使用 Vocoor 账号登录"发起授权
- 已登录用户显示用户信息

### 授权结果页 (/oauth/result)

- 处理 Vocoor OAuth 回调
- 展示授权成功/失败状态
- 自动跳转到用户中心

### 用户中心 (/dashboard)

- 展示用户详细信息
- 需要登录才能访问

## 授权流程

```
1. 用户点击"使用 Vocoor 账号登录"
   ↓
2. 前端调用 /api/oauth/authorize 获取授权 URL
   ↓
3. 前端重定向到 Vocoor OAuth 登录页
   ↓
4. 用户在 Vocoor 完成登录授权
   ↓
5. Vocoor 回调到后端 /api/oauth/callback
   ↓
6. 后端处理后重定向到前端 /oauth/result?token=xxx
   ↓
7. 前端保存 Token，获取用户信息
   ↓
8. 跳转到用户中心
```

## 配置说明

### 开发环境代理

`vite.config.ts` 中配置了开发代理：

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8081',  // 后端服务地址
    changeOrigin: true
  }
}
```

### API 基础路径

`src/api/oauth.ts` 中的 axios 实例配置：

```typescript
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})
```

## 注意事项

1. **确保后端服务已启动**：前端依赖后端提供 OAuth 相关接口
2. **端口冲突**：前端默认 5173，后端默认 8081
3. **CORS**：后端需要配置允许前端域名的跨域请求
4. **Token 存储**：Token 存储在 localStorage，注意 XSS 防护

## 相关链接

- [后端项目](../backend/README.md)
- [Vocoor OAuth 接入指南](../../../vocoor-service/docs/vocoor_oauth_integration_guide.md)


