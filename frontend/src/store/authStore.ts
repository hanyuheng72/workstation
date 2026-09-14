import { create } from 'zustand'
import { api } from '@/api/client'
import { authApi } from '@/api/auth'
import type { LoginResponse } from '@/types/api'

const STORAGE_KEY = 'workstation.auth'

type Status = 'unknown' | 'authenticated' | 'anonymous'

interface StoredAuth {
  token: string
  expiresAt: number
}

function readStored(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as StoredAuth
    if (typeof parsed?.token !== 'string' || typeof parsed?.expiresAt !== 'number') {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

function writeStored(value: StoredAuth | null) {
  try {
    if (value) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    // 隐私模式下可能写不进去，内存里的登录态仍然有效
  }
}

interface AuthState {
  token: string | null
  status: Status
  /** 这个部署要不要登录。null 表示还没问过服务端 */
  authRequired: boolean | null
  /** 令牌失效时由 HTTP 层调用，把界面切回登录页 */
  markExpired: () => void
  login: (password: string) => Promise<void>
  logout: () => void
  restore: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null,
  status: 'unknown',
  authRequired: null,

  markExpired: () => {
    // 登录已关闭的部署不该被 401 赶去登录页
    if (get().authRequired === false) {
      set({ token: null, status: 'authenticated' })
      return
    }
    writeStored(null)
    set({ token: null, status: 'anonymous' })
  },

  login: async (password) => {
    const result = await api.post<LoginResponse>('/auth/login', { password })
    writeStored({ token: result.token, expiresAt: result.expiresAt })
    set({ token: result.token, status: 'authenticated', authRequired: true })
  },

  logout: () => {
    writeStored(null)
    set({ token: null, status: 'anonymous' })
  },

  restore: async () => {
    // 先问服务端要不要登录。不问的话，关掉登录的部署也会先闪一下登录页。
    try {
      const { authRequired } = await authApi.status()
      set({ authRequired })
      if (!authRequired) {
        writeStored(null)
        set({ token: null, status: 'authenticated' })
        return
      }
    } catch {
      // 服务端连不上就按要登录处理，让登录页去提示连接失败
      set({ authRequired: true })
    }

    const stored = readStored()
    if (!stored || stored.expiresAt <= Date.now()) {
      writeStored(null)
      set({ token: null, status: 'anonymous' })
      return
    }

    // 先信任本地令牌让界面立刻可用，再向服务端确认一次；
    // 若已失效（例如 JWT_SECRET 换过），401 会把状态切回未登录。
    set({ token: stored.token, status: 'authenticated' })
    try {
      await api.get('/auth/me')
    } catch {
      if (get().status === 'authenticated') {
        get().markExpired()
      }
    }
  },
}))
