import { cn } from '@/utils/cn'

/** 一组可点选的小标签，用于分类、部位这类选项集合 */
export function ChipGroup<T extends string | number>({
  value,
  options,
  onChange,
  columns,
}: {
  value: T | null
  options: { value: T; label: string; hint?: string }[]
  onChange: (value: T) => void
  columns?: number
}) {
  return (
    <div
      className={cn('grid gap-2.5', columns === 2 ? 'grid-cols-2' : 'grid-cols-3')}
      role="radiogroup"
    >
      {options.map((option) => {
        const active = option.value === value
        return (
          <button
            key={String(option.value)}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => onChange(option.value)}
            className={cn(
              'flex flex-col items-center justify-center gap-0.5 rounded-tile px-2 py-3.5 text-sm transition-all',
              // 选中的是「按下去」的那一个
              active
                ? 'bg-surface font-medium text-primary shadow-inset'
                : 'bg-surface text-fg-muted shadow-raised-sm neu-pressable',
            )}
          >
            <span className="truncate">{option.label}</span>
            {option.hint ? <span className="text-[11px] text-fg-subtle">{option.hint}</span> : null}
          </button>
        )
      })}
    </div>
  )
}
