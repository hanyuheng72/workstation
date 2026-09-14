import { api } from './client'
import type {
  AiMessageVO,
  AiStatusVO,
  AiSummaryVO,
  AnalysisVO,
  ChatResponse,
  ConversationVO,
} from '@/types/domain'

export const aiApi = {
  status: () => api.get<AiStatusVO>('/ai/status'),

  /** 只读缓存，不会触发模型调用 */
  todaySummary: () => api.get<AiSummaryVO>('/ai/summary/today'),

  /** 生成或刷新，会真实调用模型 */
  regenerateSummary: () => api.post<AiSummaryVO>('/ai/summary/today'),

  chat: (message: string, conversationId?: number | null) =>
    api.post<ChatResponse>('/ai/chat', { message, conversationId: conversationId ?? null }),

  /** 确认后才会真正写业务数据 */
  confirm: (messageId: number) => api.post<AiMessageVO>(`/ai/actions/${messageId}/confirm`),

  reject: (messageId: number) => api.post<AiMessageVO>(`/ai/actions/${messageId}/reject`),

  analyzeExpense: (month?: string) =>
    api.get<AnalysisVO>('/ai/analysis/expense', { params: { month } }),

  analyzeWorkout: () => api.get<AnalysisVO>('/ai/analysis/workout'),

  conversations: () => api.get<ConversationVO[]>('/ai/conversations'),

  messages: (conversationId: number) =>
    api.get<AiMessageVO[]>(`/ai/conversations/${conversationId}/messages`),
}
