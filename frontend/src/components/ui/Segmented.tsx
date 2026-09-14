import { cn } from '@/utils/cn'

export interface SegmentedOption<T extends string> {
  value: T
  label: string
}

interface SegmentedProps<T extends string> {
  value: T
  options: SegmentedOption<T>[]
  onChange: (value: T) => void
  className?: string
}

/**
 * 分段选择。轨道凹陷、选中项凸起——凹槽里嵌着一块按下去又弹起来的东西，
 * 这是拟物里最直观的「选中」表达，不需要额外加颜色。
 */
export function Segmented<T extends string>({ value, options, onChange, className }: SegmentedProps<T>) {
  return (
    <div className={cn('flex gap-1 rounded-pill bg-surface p-1 shadow-inset', className)} role="tablist">
      {options.map((option) => {
        const active = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(option.value)}
            className={cn(
              'flex-1 rounded-pill py-2 text-[13px] transition-all',
              active
                ? 'bg-surface font-medium text-primary shadow-raised-sm'
                : 'text-fg-muted active:text-fg',
            )}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
