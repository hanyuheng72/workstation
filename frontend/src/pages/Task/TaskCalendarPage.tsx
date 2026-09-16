import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { TaskForm } from '@/components/domain/TaskForm'
import { taskApi } from '@/api/task'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'
import { cn } from '@/utils/cn'
import { todayISO } from '@/utils/date'
import { formatMonthDay, weekdayLabel } from '@/utils/format'
import { occurrenceToTask } from '@/utils/task'
import type { TaskOccurrenceVO, TaskVO } from '@/types/domain'

const WEEKDAY_HEADERS = ['一', '二', '三', '四', '五', '六', '日']

export function TaskCalendarPage() {
  const showToast = useUiStore((state) => state.showToast)

  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [selected, setSelected] = useState(todayISO())
  const [busyId, setBusyId] = useState<number | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<TaskVO | null>(null)

  const calendar = useAsync(() => taskApi.calendar(year, month), [year, month], {
    key: `task:calendar:${year}-${month}`,
  })

  const lastDay = new Date(year, month, 0).getDate()

  const dayItems = useAsync(() => taskApi.range(selected, selected), [selected], {
    key: `task:range:${selected}`,
  })

  /** 日历格子按周一对齐：算出本月 1 号前面要空几格 */
  const leadingBlanks = useMemo(() => {
    const weekday = new Date(year, month - 1, 1).getDay()
    return (weekday + 6) % 7
  }, [year, month])

  const byDate = useMemo(() => {
    const map = new Map<string, { total: number; done: number }>()
    for (const day of calendar.data ?? []) {
      map.set(day.date, { total: day.total, done: day.done })
    }
    return map
  }, [calendar.data])

  const shiftMonth = (delta: number) => {
    const next = new Date(year, month - 1 + delta, 1)
    setYear(next.getFullYear())
    setMonth(next.getMonth() + 1)
  }

  const toggle = async (item: TaskOccurrenceVO) => {
    setBusyId(item.taskId)
    try {
      if (item.status === 'DONE') {
        await taskApi.uncomplete(item.taskId, item.occurDate)
      } else {
        await taskApi.complete(item.taskId, item.occurDate)
      }
      calendar.reload()
      dayItems.reload()
    } catch {
      showToast('操作失败', 'danger')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <PageHeader
        back
        title="任务日历"
        subtitle="每天的任务数与完成情况"
        action={
          <Link
            to="/tasks"
            className="flex h-9 items-center gap-1.5 rounded-tile px-3.5 text-sm text-fg-muted shadow-raised-sm neu-pressable"
          >
            <CalendarDays size={15} />
            列表
          </Link>
        }
      />

      <Card className="p-4">
        <div className="mb-3 flex items-center justify-between">
          <button
            type="button"
            aria-label="上个月"
            onClick={() => shiftMonth(-1)}
            className="flex size-8 items-center justify-center rounded-full text-fg-muted active:bg-surface-2"
          >
            <ChevronLeft size={18} />
          </button>
          <span className="text-sm font-medium">
            {year} 年 {month} 月
          </span>
          <button
            type="button"
            aria-label="下个月"
            onClick={() => shiftMonth(1)}
            className="flex size-8 items-center justify-center rounded-full text-fg-muted active:bg-surface-2"
          >
            <ChevronRight size={18} />
          </button>
        </div>

        {calendar.loading && !calendar.data ? (
          <SplashScreen />
        ) : (
          <>
            <div className="grid grid-cols-7 gap-1 text-center text-[11px] text-fg-subtle">
              {WEEKDAY_HEADERS.map((label) => (
                <span key={label} className="py-1">
                  {label}
                </span>
              ))}
            </div>

            <div className="mt-1 grid grid-cols-7 gap-1">
              {Array.from({ length: leadingBlanks }).map((_, index) => (
                <span key={`blank-${index}`} />
              ))}

              {Array.from({ length: lastDay }).map((_, index) => {
                const dayNumber = index + 1
                const iso = `${year}-${String(month).padStart(2, '0')}-${String(dayNumber).padStart(2, '0')}`
                const info = byDate.get(iso)
                const complete = info ? info.total > 0 && info.done === info.total : false
                const isSelected = iso === selected
                const isToday = iso === todayISO()

                return (
                  <button
                    key={iso}
                    type="button"
                    onClick={() => setSelected(iso)}
                    className={cn(
                      'flex aspect-square flex-col items-center justify-center gap-0.5 rounded-lg text-sm transition-colors',
                      isSelected
                        ? 'bg-primary text-primary-fg'
                        : info
                          ? 'bg-primary-soft text-primary'
                          : 'text-fg-muted active:bg-surface-2',
                    )}
                  >
                    <span className={cn(isToday && !isSelected && 'font-semibold text-fg')}>
                      {dayNumber}
                    </span>
                    {/* 有任务的日子给一个点：全部完成是实心，未完成是空心 */}
                    {info ? (
                      <span
                        className={cn(
                          'size-1.5 rounded-full',
                          isSelected
                            ? 'bg-primary-fg'
                            : complete
                              ? 'bg-primary'
                              : 'border border-primary',
                        )}
                      />
                    ) : (
                      <span className="size-1.5" />
                    )}
                  </button>
                )
              })}
            </div>
          </>
        )}
      </Card>

      <h2 className="mt-5 mb-2 flex items-center justify-between px-1">
        <span className="text-sm font-medium text-fg-muted">
          {selected === todayISO() ? '今天' : formatMonthDay(selected)}
          <span className="ml-1.5 text-xs font-normal text-fg-subtle">
            {weekdayLabel(selected)}
          </span>
        </span>
        {/* 选中哪天就在哪天加任务，不用回列表页再改日期 */}
        <button
          type="button"
          onClick={() => {
            setEditing(null)
            setFormOpen(true)
          }}
          className="flex items-center gap-1 rounded-tile px-2.5 py-1.5 text-xs text-primary shadow-raised-sm neu-pressable"
        >
          <Plus size={13} />
          加任务
        </button>
      </h2>

      {dayItems.loading && !dayItems.data ? (
        <SplashScreen />
      ) : !dayItems.data || dayItems.data.length === 0 ? (
        <EmptyState title="这一天没有任务" description="点上面的「加任务」直接加到这一天。" />
      ) : (
        <Card className="overflow-hidden">
          {dayItems.data.map((item, index) => {
            const done = item.status === 'DONE'
            return (
              <div
                key={item.occurrenceId}
                className={cn('flex items-center gap-3 px-4 py-3', index > 0 && 'border-t border-line')}
              >
                <button
                  type="button"
                  aria-label={done ? '取消完成' : '标记完成'}
                  disabled={busyId === item.taskId}
                  onClick={() => toggle(item)}
                  className={cn(
                    'flex size-6 shrink-0 items-center justify-center rounded-pill text-xs transition-all',
                    done ? 'bg-primary text-primary-fg shadow-raised-sm' : 'bg-surface shadow-inset',
                  )}
                >
                  {done ? <span className="text-xs">✓</span> : null}
                </button>
                <button
                  type="button"
                  className="min-w-0 flex-1 text-left"
                  onClick={() => {
                    setEditing(occurrenceToTask(item))
                    setFormOpen(true)
                  }}
                >
                  <p className={cn('truncate text-sm', done && 'text-fg-subtle line-through')}>
                    {item.title}
                  </p>
                  <p className="mt-0.5 text-xs text-fg-subtle">
                    {item.taskType === 'LONG_TERM' ? '长期' : '今日'}
                    {item.recurring ? ' · 重复' : ''}
                  </p>
                </button>
              </div>
            )
          })}
        </Card>
      )}

      <TaskForm
        open={formOpen}
        onClose={() => {
          setFormOpen(false)
          setEditing(null)
        }}
        onSaved={() => {
          calendar.reload()
          dayItems.reload()
        }}
        editing={editing}
        defaultDate={selected}
      />
    </>
  )
}
