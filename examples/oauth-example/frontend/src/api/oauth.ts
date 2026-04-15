import axios from 'axios'
import type { AxiosResponse } from 'axios'

// 创建 axios 实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

/**
 * API 响应结构（Nebula 框架）
 */
export interface ApiResponse<T> {
  success: boolean
  code?: string
  message?: string
  data?: T
}

/**
 * 授权响应
 */
export interface AuthorizeResponse {
  authUrl: string
}

/**
 * 授权请求
 */
export interface AuthorizeRequest {
  scope: string
}

/**
 * 当前用户信息
 */
export interface CurrentUser {
  id: number
  username: string
  nickname: string
  avatar: string
  mobile: string
  email: string
  companyName: string
}

/**
 * 获取 Vocoor 授权 URL
 * @param scope 权限范围
 * @returns 授权 URL
 */
export async function getAuthUrl(scope?: string): Promise<string> {
  const params = scope ? { scope } : {}
  const response = await api.get<ApiResponse<AuthorizeResponse>>('/api/oauth/authorize', { params })
  if (response.data.success && response.data.data) {
    return response.data.data.authUrl
  }
  
  throw new Error(response.data.message || '获取授权地址失败')
}

/**
 * 获取当前登录用户信息
 * @returns 用户信息
 */
export async function getCurrentUser(): Promise<CurrentUser> {
  const response: AxiosResponse<ApiResponse<CurrentUser>> = await api.get('/api/oauth/user/current')
  
  if (response.data.success && response.data.data) {
    return response.data.data
  }
  
  throw new Error(response.data.message || '获取用户信息失败')
}

/**
 * 退出登录
 */
export async function logout(): Promise<void> {
  await api.post('/oauth/logout')
  localStorage.removeItem('access_token')
}

/**
 * 健康检查响应
 */
export interface HealthResult {
  status: string
}

/**
 * 健康检查
 * @returns 健康状态
 */
export async function healthCheck(): Promise<HealthResult> {
  const response: AxiosResponse<ApiResponse<HealthResult>> = await api.get('/health')
  const healthResult: HealthResult = response.data.data  as HealthResult
  if (healthResult.status === 'UP') {
    return healthResult
  }
  
  throw new Error(response.data.message || '健康检查失败')
}

