import { useState } from 'react'
import { Trophy } from 'lucide-react'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { Segmented } from '@/components/ui/Segmented'
import { ChartFrame } from '@/components/charts/ChartFrame'
import { BarSingle } from '@/components/charts/BarSingle'
import { workoutStatsApi, type FrequencyGranularity } from '@/api/workout'
import { useAsync } from '@/hooks/useAsync'
import { formatMonthDay, trimNumber } from '@/utils/format'

export function WorkoutStatsSection() {
  const [granularity, setGranularity] = useState<FrequencyGranularity>('week')

  const frequency = useAsync(() => workoutStatsApi.frequency(granularity), [granularity], {
    key: `workout:frequency:${granularity}`,
  })
  const prs = useAsync(() => workoutStatsApi.personalBests(), [], { key: 'workout:pr' })

  const stats = frequency.data
  const hasSessions = stats ? stats.sessions.some((point) => point.value > 0) : false
  const trained = stats?.byPart.filter((part) => part.count > 0) ?? []

  return (
    <div className="flex flex-col gap-3">
      <ChartFrame
        title="训练次数"
        subtitle={granularity === 'week' ? '最近 12 周' : '最近 12 个月'}
        isEmpty={!hasSessions}
        emptyHint="还没有训练记录"
        action={
          <Segmented
            value={granularity}
            onChange={setGranularity}
            options={[
              { value: 'week' as FrequencyGranularity, label: '周' },
              { value: 'month' as FrequencyGranularity, label: '月' },
            ]}
            className="w-28"
          />
        }
      >
        <BarSingle points={stats?.sessions ?? []} unit=" 次" />
      </ChartFrame>

      <Card className="p-4">
        <p className="text-sm font-medium">各部位训练次数</p>
        {trained.length === 0 ? (
          <p className="mt-3 text-sm text-fg-subtle">还没有训练记录</p>
        ) : (
          <ul className="mt-3 flex flex-col gap-2">
            {trained.map((part) => {
              const max = Math.max(...trained.map((item) => item.count))
              return (
                <li key={part.partId} className="flex items-center gap-3">
                  <span className="w-10 shrink-0 text-sm">{part.partName}</span>
                  <span className="h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-surface-2">
                    <span
                      className="block h-full rounded-full bg-primary"
                      style={{ width: `${(part.count / max) * 100}%` }}
                    />
                  </span>
                  <span className="tabular w-8 shrink-0 text-right text-xs text-fg-muted">
                    {part.count}
                  </span>
                </li>
              )
            })}
          </ul>
        )}
      </Card>

      <div>
        <h2 className="mb-2 flex items-center gap-1.5 px-1 text-sm font-medium text-fg-muted">
          <Trophy size={14} />
          个人最佳
        </h2>
        {!prs.data || prs.data.length === 0 ? (
          <EmptyState title="还没有 PR 记录" description="记录训练后，这里会显示每个动作的历史最大重量。" />
        ) : (
          <Card className="divide-y divide-line">
            {prs.data.map((pr) => (
              <div key={pr.exerciseId} className="flex items-center gap-3 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{pr.exerciseName}</p>
                  <p className="mt-0.5 text-xs text-fg-subtle">
                    {pr.partName ?? '—'} · {formatMonthDay(pr.achievedDate)}
                  </p>
                </div>
                <span className="tabular shrink-0 text-[15px] font-semibold">
                  {trimNumber(pr.maxWeight)}
                  <span className="ml-0.5 text-xs font-normal text-fg-subtle">kg</span>
                  <span className="ml-1.5 text-xs font-normal text-fg-subtle">× {pr.reps}</span>
                </span>
              </div>
            ))}
          </Card>
        )}
      </div>
    </div>
  )
}
