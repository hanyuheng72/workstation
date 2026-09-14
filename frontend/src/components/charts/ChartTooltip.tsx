import type { ReactNode } from 'react'

interface Row {
  label: string
  value: ReactNode
  color?: string
}

interface ChartTooltipProps {
  title?: string
  rows: Row[]
}

/**
 * 自定义 tooltip。tooltip 只是增强，不是读数的唯一途径——
 * 同样的数字在下方列表和统计卡里都能看到。
 */
export function ChartTooltip({ title, rows }: ChartTooltipProps) {
  return (
    <div className="rounded-tile bg-surface px-3.5 py-2.5 shadow-raised-sm">
      {title ? <p className="mb-1 text-xs text-fg-subtle">{title}</p> : null}
      <div className="flex flex-col gap-0.5">
        {rows.map((row) => (
          <div key={row.label} className="flex items-center gap-2 text-xs">
            {row.color ? (
              <span
                className="size-2 shrink-0 rounded-full"
                style={{ backgroundColor: row.color }}
                aria-hidden="true"
              />
            ) : null}
            <span className="text-fg-muted">{row.label}</span>
            <span className="ml-auto pl-3 font-medium text-fg">{row.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
