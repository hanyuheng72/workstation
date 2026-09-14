import type { ReactNode } from 'react'
import { cn } from '@/utils/cn'

interface StatTileProps {
  label: string
  value: ReactNode
  unit?: string
  hint?: ReactNode
  tone?: 'default' | 'success' | 'danger' | 'warning'
  className?: string
}

const TONES = {
  default: 'text-fg',
  success: 'text-success',
  danger: 'text-danger',
  warning: 'text-warning',
}

export function StatTile({ label, value, unit, hint, tone = 'default', className }: StatTileProps) {
  return (
    <div className={cn('flex flex-col gap-1', className)}>
      <span className="text-xs text-fg-subtle">{label}</span>
      {/* 大数字用比例字形：等宽数字在大字号下会显得松散 */}
      <span className={cn('flex items-baseline gap-1 text-xl font-semibold', TONES[tone])}>
        {value}
        {unit ? <span className="text-xs font-normal text-fg-muted">{unit}</span> : null}
      </span>
      {hint ? <span className="text-xs text-fg-muted">{hint}</span> : null}
    </div>
  )
}
