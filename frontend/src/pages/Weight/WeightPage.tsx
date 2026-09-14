import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Plus, Scale, Trash2 } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { Segmented } from '@/components/ui/Segmented'
import { ChartFrame } from '@/components/charts/ChartFrame'
import { TrendLine } from '@/components/charts/TrendLine'
import { BmiScale } from '@/components/charts/BmiScale'
import { WeightForm } from '@/components/domain/WeightForm'
import { weightApi, type WeightGranularity } from '@/api/weight'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'
import { cn } from '@/utils/cn'
import { formatWeight, formatMonthDay, weekdayLabel, isToday, trimNumber } from '@/utils/format'
import type { WeightVO } from '@/types/domain'

const GRANULARITIES = [
  { value: 'day' as WeightGranularity, label: '日' },
  { value: 'month' as WeightGranularity, label: '月' },
  { value: 'year' as WeightGranularity, label: '年' },
]

const GRANULARITY_HINT: Record<WeightGranularity, string> = {
  day: '最近 30 天',
  month: '最近 12 个月，取当月平均',
  year: '最近 5 年，取当年平均',
}

export function WeightPage() {
  const [params, setParams] = useSearchParams()
  const showToast = useUiStore((state) => state.showToast)

  const [formOpen, setFormOpen] = useState(params.get('quick') === '1')
  const [editing, setEditing] = useState<WeightVO | null>(null)
  const [granularity, setGranularity] = useState<WeightGranularity>('day')

  const list = useAsync(() => weightApi.list(), [])
  const stats = useAsync(() => weightApi.stats(), [])
  const trend = useAsync(() => weightApi.trend(granularity), [granularity])
  const bmi = useAsync(() => weightApi.bmi(), [])

  const reloadAll = () => {
    list.reload()
    stats.reload()
    trend.reload()
    bmi.reload()
  }

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    if (params.has('quick')) {
      params.delete('quick')
      setParams(params, { replace: true })
    }
  }

  const remove = async (record: WeightVO) => {
    if (!window.confirm(`删除 ${record.recordDate} 的体重记录？`)) return
    try {
      await weightApi.remove(record.id)
      showToast('已删除', 'success')
      reloadAll()
    } catch {
      showToast('删除失败', 'danger')
    }
  }

  const records = list.data ?? []
  const current = records.length > 0 ? records[0] : null
  const stat = stats.data
  const delta = stat?.latestDelta ?? null

  return (
    <>
      <PageHeader
        title="体重"
        subtitle="记录每天的数字，看长期趋势"
        action={
          <Button size="sm" onClick={() => setFormOpen(true)}>
            <Plus size={16} />
            记录
          </Button>
        }
      />

      {list.loading && !list.data ? (
        <SplashScreen />
      ) : (
        <div className="flex flex-col gap-4">
          {/* 当前体重是这张页面的主角，直接排在台面上，不塞进卡片 */}
          {current ? (
            <section className="px-1">
              <p className="text-[11px] font-medium tracking-[0.14em] text-fg-subtle">当前体重</p>
              <p className="mt-2 flex items-baseline gap-2">
                <span className="display text-[2.75rem] font-semibold">
                  {formatWeight(current.weightKg)}
                </span>
                <span className="text-sm text-fg-muted">kg</span>
                {delta !== null && delta !== 0 ? (
                  <span className="text-sm text-fg-muted">
                    {delta > 0 ? '↑' : '↓'} {formatWeight(Math.abs(delta))}
                  </span>
                ) : null}
              </p>
              <p className="mt-1 text-xs text-fg-muted">
                {isToday(current.recordDate) ? '今天' : formatMonthDay(current.recordDate)}
                {current.note ? ` · ${current.note}` : ''}
              </p>

              {stat && stat.recordCount > 1 ? (
                <div className="mt-4 flex gap-6 border-t border-line pt-3">
                  <Mini label="最高" value={formatWeight(stat.max)} />
                  <Mini label="最低" value={formatWeight(stat.min)} />
                  <Mini label="平均" value={formatWeight(stat.average)} />
                  <Mini label="记录" value={`${stat.recordCount} 次`} />
                </div>
              ) : null}
            </section>
          ) : (
            <EmptyState
              icon={<Scale size={28} />}
              title="还没有体重记录"
              description="每天记一次就够，趋势图和 BMI 刻度会自动出现。"
              action={
                <Button onClick={() => setFormOpen(true)}>
                  <Plus size={16} />
                  记录第一条
                </Button>
              }
            />
          )}

          <ChartFrame
            title="体重趋势"
            subtitle={GRANULARITY_HINT[granularity]}
            isEmpty={!trend.data || trend.data.length === 0}
            emptyHint="记满两天就能看到趋势"
            action={
              <Segmented
                value={granularity}
                onChange={setGranularity}
                options={GRANULARITIES}
                className="w-32"
              />
            }
          >
            <TrendLine points={trend.data ?? []} unit="kg" format={(value) => trimNumber(value, 1)} />
          </ChartFrame>

          <Card className="p-4">
            <p className="mb-3 text-sm font-medium">BMI</p>
            {bmi.data ? <BmiScale bmi={bmi.data} /> : <SplashScreen />}
          </Card>

          {records.length > 0 ? (
            <section>
              <h2 className="mb-2 px-1 text-[11px] font-medium tracking-[0.14em] text-fg-subtle">
                历史记录
              </h2>
              <Card className="overflow-hidden">
                {records.map((record, index) => (
                  <div
                    key={record.id}
                    className={cn(
                      'flex items-center gap-3 px-4 py-3',
                      index > 0 && 'border-t border-line',
                    )}
                  >
                    <button
                      type="button"
                      className="flex min-w-0 flex-1 items-baseline gap-2 text-left"
                      onClick={() => {
                        setEditing(record)
                        setFormOpen(true)
                      }}
                    >
                      <span className="text-sm">{formatMonthDay(record.recordDate)}</span>
                      <span className="text-[11px] text-fg-subtle">
                        {weekdayLabel(record.recordDate)}
                      </span>
                      {record.note ? (
                        <span className="truncate text-[11px] text-fg-subtle">
                          {record.note}
                        </span>
                      ) : null}
                    </button>
                    <span className="tabular shrink-0 text-[15px] font-medium">
                      {formatWeight(record.weightKg)}
                      <span className="ml-0.5 text-[11px] font-normal text-fg-subtle">kg</span>
                    </span>
                    <button
                      type="button"
                      aria-label="删除"
                      onClick={() => remove(record)}
                      className="-mr-1 flex size-8 shrink-0 items-center justify-center rounded-tile text-fg-subtle active:bg-surface-2"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                ))}
              </Card>
            </section>
          ) : null}
        </div>
      )}

      <WeightForm open={formOpen} onClose={closeForm} onSaved={reloadAll} editing={editing} />
    </>
  )
}

function Mini({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-[11px] text-fg-subtle">{label}</p>
      <p className="tabular mt-0.5 text-[13px] font-medium">{value}</p>
    </div>
  )
}
