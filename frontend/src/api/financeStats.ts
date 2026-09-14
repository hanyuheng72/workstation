import { api } from './client'
import type {
  CategoryStatVO,
  FinanceOverviewVO,
  PeriodAmountVO,
  TransactionType,
} from '@/types/domain'

export const financeStatsApi = {
  overview: () => api.get<FinanceOverviewVO>('/finance/stats/overview'),

  /** month 省略时后端取当前月 */
  category: (type: TransactionType, month?: string) =>
    api.get<CategoryStatVO[]>('/finance/stats/category', { params: { type, month } }),

  dailyTrend: (month?: string) =>
    api.get<PeriodAmountVO[]>('/finance/stats/daily-trend', { params: { month } }),

  monthlyTrend: (year?: number) =>
    api.get<PeriodAmountVO[]>('/finance/stats/monthly-trend', { params: { year } }),
}
