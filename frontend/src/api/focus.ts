import { api } from './client'
import type { FocusSessionVO, FocusStartRequest, FocusStatsVO } from '@/types/domain'

export const focusApi = {
  /** 进行中的那一场；没有则返回 null */
  active: () => api.get<FocusSessionVO | null>('/focus/active'),

  list: (from?: string, to?: string) =>
    api.get<FocusSessionVO[]>('/focus/sessions', { params: { from, to } }),

  stats: () => api.get<FocusStatsVO>('/focus/stats'),

  start: (payload: FocusStartRequest) => api.post<FocusSessionVO>('/focus/sessions', payload),

  /** 暂停机会已用掉时调用它，返回的会是 FAILED 而不是 PAUSED */
  pause: (id: number) => api.post<FocusSessionVO>(`/focus/sessions/${id}/pause`),

  resume: (id: number) => api.post<FocusSessionVO>(`/focus/sessions/${id}/resume`),

  complete: (id: number) => api.post<FocusSessionVO>(`/focus/sessions/${id}/complete`),

  /** 提前学完了：没到时间也能结束，一样记成功 */
  finishEarly: (id: number) => api.post<FocusSessionVO>(`/focus/sessions/${id}/finish-early`),

  abandon: (id: number) => api.post<FocusSessionVO>(`/focus/sessions/${id}/abandon`),
}
