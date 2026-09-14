import axios, { type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import { useAuthStore } from '@/store/authStore'

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * 接口地址。
 *
 * 开发时留空，走相对路径 `/api`，由 Vite 代理转到本机后端。
 * 打包成 App（或任何前后端不同源的情况）时，构建前设 VITE_API_BASE 指向云端：
 *
 *   VITE_API_BASE=https://xxx.onrender.com/api npm run build
 *
 * 打包后页面是从手机本地加载的，相对路径没有意义，必须给绝对地址。
 */
export const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

const http = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 统一出口：成功直接返回 data，失败一律抛 ApiError。
 * 令牌失效时清掉本地登录态，由路由守卫把界面切回登录页（不做硬跳转，避免打断正在编辑的输入）。
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await http.request<ApiResponse<T>>(config)
    const body = response.data
    if (body.code !== 0) {
      throw new ApiError(body.code, body.message)
    }
    return body.data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    if (axios.isAxiosError(error)) {
      const body = error.response?.data as ApiResponse<unknown> | undefined
      if (error.response?.status === 401) {
        useAuthStore.getState().markExpired()
      }
      if (body && typeof body.code === 'number') {
        throw new ApiError(body.code, body.message)
      }
      if (error.code === 'ECONNABORTED') {
        throw new ApiError(-1, '请求超时，请检查网络后重试')
      }
      throw new ApiError(-1, '无法连接到服务器，请确认后端已启动')
    }
    throw error
  }
}

export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) => request<T>({ ...config, url, method: 'GET' }),
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({ ...config, url, method: 'POST', data }),
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({ ...config, url, method: 'PUT', data }),
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    request<T>({ ...config, url, method: 'DELETE' }),
}
