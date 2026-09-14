import { api } from './client'
import type {
  BmiVO,
  TrendPointVO,
  WeightStatsVO,
  WeightUpsertRequest,
  WeightVO,
} from '@/types/domain'

export type WeightGranularity = 'day' | 'month' | 'year'

export const weightApi = {
  list: (start?: string, end?: string) =>
    api.get<WeightVO[]>('/weights', { params: { start, end } }),

  latest: () => api.get<WeightVO | null>('/weights/latest'),

  upsert: (payload: WeightUpsertRequest) => api.post<WeightVO>('/weights', payload),

  update: (id: number, payload: WeightUpsertRequest) => api.put<WeightVO>(`/weights/${id}`, payload),

  remove: (id: number) => api.delete<void>(`/weights/${id}`),

  stats: () => api.get<WeightStatsVO>('/weights/stats'),

  trend: (granularity: WeightGranularity) =>
    api.get<TrendPointVO[]>('/weights/trend', { params: { granularity } }),

  bmi: () => api.get<BmiVO>('/weights/bmi'),
}
