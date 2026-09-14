import { api } from './client'
import type { PageResult } from '@/types/api'
import type { CategoryVO, TransactionRequest, TransactionType, TransactionVO } from '@/types/domain'

export const financeApi = {
  categories: (type?: TransactionType) =>
    api.get<CategoryVO[]>('/finance/categories', { params: { type } }),

  createCategory: (name: string, type: TransactionType) =>
    api.post<CategoryVO>('/finance/categories', { name, type }),

  removeCategory: (id: number) => api.delete<void>(`/finance/categories/${id}`),

  transactions: (params: {
    start?: string
    end?: string
    type?: TransactionType
    categoryId?: number
    page?: number
    size?: number
  }) => api.get<PageResult<TransactionVO>>('/finance/transactions', { params }),

  create: (payload: TransactionRequest) => api.post<TransactionVO>('/finance/transactions', payload),

  update: (id: number, payload: TransactionRequest) =>
    api.put<TransactionVO>(`/finance/transactions/${id}`, payload),

  remove: (id: number) => api.delete<void>(`/finance/transactions/${id}`),
}
