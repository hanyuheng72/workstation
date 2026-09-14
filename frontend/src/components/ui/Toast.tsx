import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useUiStore } from '@/store/uiStore'
import { cn } from '@/utils/cn'

const TONES = {
  default: 'text-fg',
  success: 'text-success',
  danger: 'text-danger',
}

export function ToastHost() {
  const toast = useUiStore((state) => state.toast)
  const dismiss = useUiStore((state) => state.dismissToast)

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(dismiss, 2600)
    return () => window.clearTimeout(timer)
  }, [toast, dismiss])

  if (!toast) return null

  return createPortal(
    <div className="pointer-events-none fixed inset-x-0 top-5 z-[60] flex justify-center px-4">
      <div
        role="status"
        className={cn(
          'pointer-events-auto max-w-sm rounded-pill bg-surface px-5 py-2.5 text-sm shadow-raised',
          TONES[toast.tone],
        )}
      >
        {toast.message}
      </div>
    </div>,
    document.body,
  )
}
