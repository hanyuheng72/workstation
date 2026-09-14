import { api } from './client'
import type { DashboardOverviewVO } from '@/types/domain'

export const dashboardApi = {
  overview: () => api.get<DashboardOverviewVO>('/dashboard/overview'),
}
