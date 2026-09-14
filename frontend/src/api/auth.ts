import { api } from './client'
import type { LoginResponse } from '@/types/api'

export interface AuthApi {
  /** 这个部署要不要登录。关掉登录的部署直接进首页 */
  status: () => Promise<{ authRequired: boolean }>
  login: (password: string) => Promise<LoginResponse>
  me: () => Promise<{ authenticated: boolean }>
}

export const authApi: AuthApi = {
  status: () => api.get<{ authRequired: boolean }>('/auth/status'),
  login: (password) => api.post<LoginResponse>('/auth/login', { password }),
  me: () => api.get<{ authenticated: boolean }>('/auth/me'),
}
