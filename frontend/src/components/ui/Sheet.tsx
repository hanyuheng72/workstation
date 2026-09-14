import { useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { cn } from '@/utils/cn'

interface SheetProps {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
}

/**
 * 底部弹层。整个面板是「浮起来」的，所以这里用凸起阴影而不是描边。
 * 关闭动画结束后才卸载，避免内容在滑动过程中消失。
 */
export function Sheet({ open, onClose, title, children }: SheetProps) {
  const [mounted, setMounted] = useState(open)
  const panelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (open) {
      setMounted(true)
      return
    }
    const timer = window.setTimeout(() => setMounted(false), 240)
    return () => window.clearTimeout(timer)
  }, [open])

  useEffect(() => {
    if (!open) return

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [open, onClose])

  if (!mounted) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
      <div
        className={cn(
          'absolute inset-0 bg-fg/25 transition-opacity duration-200',
          open ? 'opacity-100' : 'opacity-0',
        )}
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={cn(
          'relative flex max-h-[88vh] w-full flex-col bg-surface shadow-raised-lg',
          'rounded-t-[2rem] sm:max-w-md sm:rounded-panel',
          'transition-transform duration-200 ease-out',
          open ? 'translate-y-0' : 'translate-y-full sm:translate-y-4 sm:opacity-0',
        )}
      >
        <div className="flex items-center justify-between px-6 pt-6 pb-3">
          <h2 className="text-base font-semibold">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="关闭"
            className="-mr-1.5 flex size-9 items-center justify-center rounded-pill text-fg-subtle shadow-raised-sm neu-pressable"
          >
            <X size={18} />
          </button>
        </div>
        <div className="safe-bottom overflow-y-auto px-6 pb-6">{children}</div>
      </div>
    </div>,
    document.body,
  )
}
