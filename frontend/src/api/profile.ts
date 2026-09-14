import { api } from './client'
import type { ProfileUpdateRequest, ProfileVO } from '@/types/domain'

export const profileApi = {
  get: () => api.get<ProfileVO>('/profile'),
  update: (payload: ProfileUpdateRequest) => api.put<ProfileVO>('/profile', payload),
}
