import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CalendarDays, Plus, Trash2 } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { Segmented } from '@/components/ui/Segmented'
import { TaskForm } from '@/components/domain/TaskForm'
import { taskApi } from '@/api/task'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'
import { cn } from '@/utils/cn'
import { formatMonthDay } from '@/utils/format'
import { occurrenceToTask } from '@/utils/task'
import type { TaskVO } from '@/types/domain'

type Tab = 'TODAY' | 'LONG_TERM'

const TABS = [
  { value: 'TODAY' as Tab, label: '今日' },
  { value: 'LONG_TERM' as Tab, label: '长期' },
]

export function TaskPage() {
  const [params, setParams] = useSearchParams()
  const showToast = useUiStore((state) => state.showToast)

  const [tab, setTab] = useState<Tab>('TODAY')
  const [formOpen, setFormOpen] = useState(params.get('quick') === '1')
  const [editing, setEditing] = useState<TaskVO | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  const today = useAsync(() => taskApi.today(), [], { key: 'task:today' })
  const longTerm = useAsync(
    () => (tab === 'LONG_TERM' ? taskApi.list('LONG_TERM') : Promise.resolve([] as TaskVO[])),
    [tab],
    { key: `task:list:${tab}` },
  )

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    if (params.has('quick')) {
      params.delete('quick')
      setParams(params, { replace: true })
    }
  }

  const reload = () => {
    today.reload()
    longTerm.reload()
  }

  const toggleToday = async (taskId: number, date: string, done: boolean) => {
    setBusyId(taskId)
    try {
      if (done) {
        await taskApi.uncomplete(taskId, date)
      } else {
        await taskApi.complete(taskId, date)
      }
      today.reload()
    } catch {
      showToast('操作失败', 'danger')
    } finally {
      setBusyId(null)
    }
  }

  /** 长期任务没有「哪一天」，直接改自身状态 */
  const toggleLongTerm = async (task: TaskVO) => {
    setBusyId(task.id)
    try {
      if (task.status === 'DONE') {
        await taskApi.markPending(task.id)
      } else {
        await taskApi.markDone(task.id)
      }
      longTerm.reload()
    } catch {
      showToast('操作失败', 'danger')
    } finally {
      setBusyId(null)
    }
  }

  const remove = async (taskId: number, title: string) => {
    if (!window.confirm(`删除任务「${title}」？`)) return
    try {
      await taskApi.remove(taskId)
      showToast('已删除', 'success')
      reload()
    } catch {
      showToast('删除失败', 'danger')
    }
  }

  const todayData = today.data
  const items = useMemo(() => todayData?.items ?? [], [todayData])
  const rate = todayData?.completionRate ?? 0

  return (
    <>
      <PageHeader
        title="任务"
        subtitle="今天要做的，和更长期的目标"
        action={
          <div className="flex items-center gap-2">
            <Link
              to="/tasks/calendar"
              aria-label="日历视图"
              className="flex size-9 items-center justify-center rounded-tile text-fg-muted shadow-raised-sm neu-pressable"
            >
              <CalendarDays size={16} />
            </Link>
            <Button size="sm" onClick={() => setFormOpen(true)}>
              <Plus size={16} />
              添加
            </Button>
          </div>
        }
      />

      <Segmented value={tab} onChange={setTab} options={TABS} className="mb-4" />

      {tab === 'TODAY' ? (
        today.loading && !todayData ? (
          <SplashScreen />
        ) : (
          <>
            {/* 完成情况做成一条读数，而不是一张占半屏的卡片 */}
            <div className="mb-3 flex items-center gap-3 px-1">
              <span className="text-xs text-fg-subtle">今日完成</span>
              <span className="h-1 min-w-0 flex-1 overflow-hidden rounded-pill bg-surface-3">
                <span
                  className="block h-full rounded-pill bg-primary transition-[width] duration-300"
                  style={{ width: `${rate}%` }}
                />
              </span>
              <span className="tabular text-xs text-fg-muted">
                {todayData?.done ?? 0}/{todayData?.total ?? 0}
              </span>
              <span className="tabular text-sm font-semibold text-primary">{rate}%</span>
            </div>

            {items.length === 0 ? (
              <EmptyState
                title="今天还没有任务"
                description="点右上角「添加」，或从首页的快捷入口进来。"
              />
            ) : (
              <Card className="overflow-hidden">
                {items.map((item, index) => {
                  const done = item.status === 'DONE'
                  return (
                    <div
                      key={item.occurrenceId}
                      className={cn(
                        'flex items-start gap-3 px-4 py-3',
                        index > 0 && 'border-t border-line',
                      )}
                    >
                      <button
                        type="button"
                        aria-label={done ? '取消完成' : '标记完成'}
                        disabled={busyId === item.taskId}
                        onClick={() => toggleToday(item.taskId, item.occurDate, done)}
                        className={cn(
                          'mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-pill transition-all',
                          done
                            ? 'bg-primary text-primary-fg shadow-raised-sm'
                            : 'bg-surface shadow-inset',
                        )}
                      >
                        {done ? (
                          <svg viewBox="0 0 12 12" className="size-3" aria-hidden="true">
                            <path
                              d="M2.5 6.4l2.3 2.3 4.7-5"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                          </svg>
                        ) : null}
                      </button>

                      <button
                        type="button"
                        className="min-w-0 flex-1 text-left"
                        onClick={() => {
                          setEditing(occurrenceToTask(item))
                          setFormOpen(true)
                        }}
                      >
                        <p
                          className={cn(
                            'text-[15px] leading-snug',
                            done && 'text-fg-subtle line-through',
                          )}
                        >
                          {item.title}
                        </p>
                        {/* 备注之前存下来了却没显示，这里补上 */}
                        {item.description ? (
                          <p className="mt-0.5 line-clamp-2 text-xs text-fg-subtle">
                            {item.description}
                          </p>
                        ) : null}
                        {item.recurring ? (
                          <p className="mt-1 text-[11px] text-fg-subtle">每天重复</p>
                        ) : null}
                      </button>

                      {/* 每条都能直接删，不用先点进编辑 */}
                      <button
                        type="button"
                        aria-label="删除任务"
                        onClick={() => remove(item.taskId, item.title)}
                        className="-mr-1 flex size-8 shrink-0 items-center justify-center rounded-tile text-fg-subtle active:bg-surface-2"
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  )
                })}
              </Card>
            )}
          </>
        )
      ) : longTerm.loading && !longTerm.data ? (
        <SplashScreen />
      ) : !longTerm.data || longTerm.data.length === 0 ? (
        <EmptyState
          title="还没有长期任务"
          description="长期任务是一份待办清单，不占日历，也不会混进今日任务。"
        />
      ) : (
        <Card className="overflow-hidden">
          {longTerm.data.map((task, index) => {
            const done = task.status === 'DONE'
            return (
              <div
                key={task.id}
                className={cn(
                  'flex items-start gap-3 px-4 py-3',
                  index > 0 && 'border-t border-line',
                )}
              >
                <button
                  type="button"
                  aria-label={done ? '标记未完成' : '标记完成'}
                  disabled={busyId === task.id}
                  onClick={() => toggleLongTerm(task)}
                  className={cn(
                    'mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-pill transition-all',
                    done
                      ? 'bg-primary text-primary-fg shadow-raised-sm'
                      : 'bg-surface shadow-inset',
                  )}
                >
                  {done ? (
                    <svg viewBox="0 0 12 12" className="size-3" aria-hidden="true">
                      <path
                        d="M2.5 6.4l2.3 2.3 4.7-5"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  ) : null}
                </button>

                <button
                  type="button"
                  className="min-w-0 flex-1 text-left"
                  onClick={() => {
                    setEditing(task)
                    setFormOpen(true)
                  }}
                >
                  <p
                    className={cn(
                      'text-[15px] leading-snug',
                      done && 'text-fg-subtle line-through',
                    )}
                  >
                    {task.title}
                  </p>
                  {task.description ? (
                    <p className="mt-0.5 line-clamp-2 text-xs text-fg-subtle">
                      {task.description}
                    </p>
                  ) : null}
                  {task.dueDate ? (
                    <p className="mt-1 text-[11px] text-fg-subtle">
                      截止 {formatMonthDay(task.dueDate)}
                    </p>
                  ) : null}
                </button>

                <button
                  type="button"
                  aria-label="删除任务"
                  onClick={() => remove(task.id, task.title)}
                  className="-mr-1 flex size-8 shrink-0 items-center justify-center rounded-tile text-fg-subtle active:bg-surface-2"
                >
                  <Trash2 size={15} />
                </button>
              </div>
            )
          })}
        </Card>
      )}

      <TaskForm
        open={formOpen}
        onClose={closeForm}
        onSaved={reload}
        editing={editing}
        defaultType={tab}
      />
    </>
  )
}
