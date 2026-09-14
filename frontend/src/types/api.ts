export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface LoginResponse {
  token: string
  expiresAt: number
}
