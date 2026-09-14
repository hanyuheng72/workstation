import { api } from './client'
import type {
  ExercisePartVO,
  ExerciseVO,
  PersonalBestVO,
  WorkoutComparisonVO,
  WorkoutFrequencyVO,
  WorkoutRequest,
  WorkoutVO,
  WorkoutVolumeVO,
} from '@/types/domain'

export type FrequencyGranularity = 'week' | 'month'

export const workoutStatsApi = {
  frequency: (granularity: FrequencyGranularity) =>
    api.get<WorkoutFrequencyVO>('/workout/stats/frequency', { params: { granularity } }),

  volume: (start?: string, end?: string) =>
    api.get<WorkoutVolumeVO>('/workout/stats/volume', { params: { start, end } }),

  personalBests: () => api.get<PersonalBestVO[]>('/workout/stats/pr'),
}

export const workoutApi = {
  parts: () => api.get<ExercisePartVO[]>('/workout/parts'),

  exercises: (partId: number) => api.get<ExerciseVO[]>(`/workout/parts/${partId}/exercises`),

  createExercise: (partId: number, name: string) =>
    api.post<ExerciseVO>('/workout/exercises', { partId, name }),

  removeExercise: (id: number) => api.delete<void>(`/workout/exercises/${id}`),

  records: (params: { start?: string; end?: string; partId?: number; exerciseId?: number }) =>
    api.get<WorkoutVO[]>('/workout/records', { params }),

  record: (id: number) => api.get<WorkoutVO>(`/workout/records/${id}`),

  lastRecord: (exerciseId: number) =>
    api.get<WorkoutVO | null>('/workout/records/last', { params: { exerciseId } }),

  comparison: (id: number) => api.get<WorkoutComparisonVO>(`/workout/records/${id}/comparison`),

  create: (payload: WorkoutRequest) => api.post<WorkoutVO>('/workout/records', payload),

  update: (id: number, payload: WorkoutRequest) => api.put<WorkoutVO>(`/workout/records/${id}`, payload),

  remove: (id: number) => api.delete<void>(`/workout/records/${id}`),
}
