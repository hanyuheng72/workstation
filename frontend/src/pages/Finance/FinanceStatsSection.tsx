import { useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { ChartFrame } from '@/components/charts/ChartFrame'
import { CategoryPie } from '@/components/charts/CategoryPie'
import { BarTrend } from '@/components/charts/BarTrend'
import { Segmented } from '@/components/ui/Segmented'
import { financeStatsApi } from '@/api/financeStats'
import { useAsync } from '@/hooks/useAsync'
import { formatCompact, formatMoney } from '@/utils/format'
import type { TransactionType } from '@/types/domain'

/** 当前月份，格式 yyyy-MM */
function currentMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function shiftMonth(month: string, delta: number): string {
  const [year, mon] = month.split('-').map(Number)
  const next = new Date(year, mon - 1 + delta, 1)
  return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`
}

export function FinanceStatsSection() {
  const [pieType, setPieType] = useState<TransactionType>('EXPENSE')
  // 默认看本月，可以往前翻；不允许翻到未来
  const [month, setMonth] = useState(currentMonth)

  const categories = useAsync(() => financeStatsApi.category(pieType, month), [pieType, month])
  const daily = useAsync(() => financeStatsApi.dailyTrend(), [])
  const monthly = useAsync(() => financeStatsApi.monthlyTrend(), [])

  const isCurrentMonth = month === currentMonth()
  const label = `${Number(month.split('-')[0])} 年 ${Number(month.split('-')[1])} 月`

  return (
    <div className="flex flex-col gap-3">
      <ChartFrame
        title={pieType === 'EXPENSE' ? '支出分类占比' : '收入分类占比'}
        subtitle={
          <span className="flex items-center gap-1">
            <button
              type="button"
              aria-label="上个月"
              onClick={() => setMonth((value) => shiftMonth(value, -1))}
              className="-ml-1 flex size-6 items-center justify-center rounded-pill text-fg-muted active:bg-surface-2"
            >
              <ChevronLeft size={14} />
            </button>
            <span className="tabular">{label}</span>
            <button
              type="button"
              aria-label="下个月"
              disabled={isCurrentMonth}
              onClick={() => setMonth((value) => shiftMonth(value, 1))}
              className="flex size-6 items-center justify-center rounded-pill text-fg-muted active:bg-surface-2 disabled:opacity-30"
            >
              <ChevronRight size={14} />
            </button>
            {!isCurrentMonth ? (
              <button
                type="button"
                onClick={() => setMonth(currentMonth())}
                className="ml-1 text-primary"
              >
                回到本月
              </button>
            ) : null}
          </span>
        }
        isEmpty={!categories.data || categories.data.length === 0}
        emptyHint={isCurrentMonth ? '这个月还没有记录' : '这个月没有记录'}
        height="auto"
        action={
          <Segmented
            value={pieType}
            onChange={setPieType}
            options={[
              { value: 'EXPENSE' as TransactionType, label: '支出' },
              { value: 'INCOME' as TransactionType, label: '收入' },
            ]}
            className="w-28"
          />
        }
      >
        <CategoryPie items={categories.data ?? []} />
      </ChartFrame>

      <ChartFrame
        title="每日收支"
        subtitle="本月，没有记录的日期按 0 计"
        isEmpty={!daily.data || daily.data.every((p) => p.income === 0 && p.expense === 0)}
        emptyHint="本月还没有记录"
      >
        <BarTrend points={daily.data ?? []} format={formatCompact} />
      </ChartFrame>

      <ChartFrame
        title="每月收支"
        subtitle="本年"
        isEmpty={!monthly.data || monthly.data.every((p) => p.income === 0 && p.expense === 0)}
        emptyHint="今年还没有记录"
      >
        <BarTrend points={monthly.data ?? []} format={formatCompact} />
      </ChartFrame>

      {monthly.data ? (
        <p className="px-1 text-xs text-fg-subtle">
          全年合计：收入 ¥
          {formatMoney(monthly.data.reduce((sum, p) => sum + Number(p.income), 0))} · 支出 ¥
          {formatMoney(monthly.data.reduce((sum, p) => sum + Number(p.expense), 0))}
        </p>
      ) : null}
    </div>
  )
}
