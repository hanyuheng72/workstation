import { api } from './client'
import type {
  CalendarDayVO,
  CompletionStatsVO,
  TaskOccurrenceVO,
  TaskRequest,
  TaskStatus,
  TaskType,
  TaskVO,
  TodayTasksVO,
} from '@/types/domain'

export const taskApi = {
  list: (type?: TaskType, status?: TaskStatus) =>
    api.get<TaskVO[]>('/tasks', { params: { type, status } }),

  today: () => api.get<TodayTasksVO>('/tasks/today'),

  range: (start: string, end: string) =>
    api.get<TaskOccurrenceVO[]>('/tasks/range', { params: { start, end } }),

  calendar: (year: number, month: number) =>
    api.get<CalendarDayVO[]>('/tasks/calendar', { params: { year, month } }),

  completionStats: (start: string, end: string) =>
    api.get<CompletionStatsVO>('/tasks/stats/completion', { params: { start, end } }),

  create: (payload: TaskRequest) => api.post<TaskVO>('/tasks', payload),

  update: (id: number, payload: TaskRequest) => api.put<TaskVO>(`/tasks/${id}`, payload),

  remove: (id: number) => api.delete<void>(`/tasks/${id}`),

  complete: (taskId: number, date: string) =>
    api.post<TaskOccurrenceVO>(`/tasks/${taskId}/occurrences/${date}/complete`),

  uncomplete: (taskId: number, date: string) =>
    api.delete<TaskOccurrenceVO>(`/tasks/${taskId}/occurrences/${date}/complete`),

  /** 长期任务没有「哪一天」，直接改自身状态 */
  markDone: (taskId: number) => api.post<TaskVO>(`/tasks/${taskId}/done`),

  markPending: (taskId: number) => api.delete<TaskVO>(`/tasks/${taskId}/done`),
}
