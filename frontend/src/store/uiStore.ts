import { create } from 'zustand'

export type ToastTone = 'default' | 'success' | 'danger'

interface Toast {
  id: number
  message: string
  tone: ToastTone
}

interface UiState {
  toast: Toast | null
  showToast: (message: string, tone?: ToastTone) => void
  dismissToast: () => void
}

let nextId = 1

export const useUiStore = create<UiState>((set) => ({
  toast: null,
  showToast: (message, tone = 'default') => set({ toast: { id: nextId++, message, tone } }),
  dismissToast: () => set({ toast: null }),
}))
