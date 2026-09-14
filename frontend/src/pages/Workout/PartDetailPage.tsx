import { useMemo, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { WorkoutForm } from '@/components/domain/WorkoutForm'
import { workoutApi } from '@/api/workout'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'
import { trimNumber, formatMonthDay } from '@/utils/format'
import type { ExerciseVO, WorkoutVO } from '@/types/domain'

export function PartDetailPage() {
  const { partId } = useParams()
  const id = Number(partId)
  const [params, setParams] = useSearchParams()
  const showToast = useUiStore((state) => state.showToast)

  const parts = useAsync(() => workoutApi.parts(), [], { key: 'workout:parts' })
  const exercises = useAsync(() => workoutApi.exercises(id), [id], {
    key: `workout:exercises:${id}`,
  })
  const records = useAsync(() => workoutApi.records({ partId: id }), [id], {
    key: `workout:records:part:${id}`,
  })

  const [formOpen, setFormOpen] = useState(params.get('quick') === '1')
  const [active, setActive] = useState<ExerciseVO | null>(null)
  const [adding, setAdding] = useState(false)
  const [newName, setNewName] = useState('')

  const part = useMemo(() => parts.data?.find((item) => item.id === id) ?? null, [parts.data, id])

  const closeForm = () => {
    setFormOpen(false)
    setActive(null)
    if (params.has('quick')) {
      params.delete('quick')
      setParams(params, { replace: true })
    }
  }

  const openFor = (exercise: ExerciseVO) => {
    setActive(exercise)
    setFormOpen(true)
  }

  const addExercise = async () => {
    const name = newName.trim()
    if (!name) return
    try {
      const created = await workoutApi.createExercise(id, name)
      showToast('已添加动作', 'success')
      setNewName('')
      setAdding(false)
      exercises.reload()
      openFor(created)
    } catch {
      showToast('添加失败，可能已存在同名动作', 'danger')
    }
  }

  const removeRecord = async (record: WorkoutVO) => {
    if (!window.confirm(`删除 ${record.recordDate} 的「${record.exerciseName}」记录？`)) return
    try {
      await workoutApi.remove(record.id)
      showToast('已删除', 'success')
      records.reload()
      parts.reload()
    } catch {
      showToast('删除失败', 'danger')
    }
  }

  /**
   * 删除动作。后端是逻辑删除，所以已经记过的训练历史不会丢，
   * 历史里仍然看得到这个动作的名字。
   */
  const removeExercise = async (exercise: ExerciseVO) => {
    if (
      !window.confirm(
        `删除动作「${exercise.name}」？\n\n已经记过的训练历史会保留，只是这个动作不再出现在列表里。`,
      )
    ) {
      return
    }
    try {
      await workoutApi.removeExercise(exercise.id)
      showToast('已删除', 'success')
      exercises.reload()
      parts.reload()
    } catch {
      showToast('删除失败', 'danger')
    }
  }

  if (parts.loading && !parts.data) return <SplashScreen />

  return (
    <>
      <PageHeader
        back
        title={part ? part.name : '部位'}
        subtitle={part?.cardio ? '记录时长与距离' : '记录重量与次数'}
        action={
          <Button size="sm" variant="secondary" onClick={() => setAdding((value) => !value)}>
            <Plus size={16} />
            动作
          </Button>
        }
      />

      {adding ? (
        <Card className="mb-4 flex items-center gap-2 p-3">
          <input
            autoFocus
            value={newName}
            onChange={(event) => setNewName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') void addExercise()
            }}
            placeholder="动作名称，回车确认"
            className="min-w-0 flex-1 bg-transparent text-[15px] outline-none placeholder:text-fg-subtle"
          />
          <Button size="sm" onClick={addExercise} disabled={!newName.trim()}>
            添加
          </Button>
        </Card>
      ) : null}

      <h2 className="mb-2 px-1 text-sm font-medium text-fg-muted">动作</h2>

      {exercises.loading && !exercises.data ? (
        <SplashScreen />
      ) : !exercises.data || exercises.data.length === 0 ? (
        <EmptyState title="还没有动作" description="点右上角「动作」添加一个。" />
      ) : (
        <div className="grid grid-cols-2 gap-2.5 md:grid-cols-3">
          {exercises.data.map((exercise) => (
            <div
              key={exercise.id}
              className="flex items-stretch rounded-tile bg-surface shadow-raised-sm"
            >
              <button
                type="button"
                onClick={() => openFor(exercise)}
                className="min-w-0 flex-1 rounded-l-tile px-3 py-2.5 text-left active:bg-surface-2"
              >
                <span className="block truncate text-sm font-medium">{exercise.name}</span>
                <span className="mt-0.5 block text-[11px] text-fg-subtle">
                  {exercise.isDefault ? '预置' : '自定义'}
                </span>
              </button>
              <button
                type="button"
                aria-label={`删除动作 ${exercise.name}`}
                onClick={() => removeExercise(exercise)}
                className="flex w-8 shrink-0 items-center justify-center rounded-r-tile text-fg-subtle active:bg-surface-2"
              >
                <Trash2 size={14} />
              </button>
            </div>
          ))}
        </div>
      )}

      <h2 className="mt-6 mb-2 px-1 text-sm font-medium text-fg-muted">历史记录</h2>

      {!records.data || records.data.length === 0 ? (
        <EmptyState title="这个部位还没有训练记录" description="选一个动作开始记录。" />
      ) : (
        <Card className="divide-y divide-line">
          {records.data.map((record) => (
            <div key={record.id} className="flex items-start gap-3 px-4 py-3">
              <div className="min-w-0 flex-1">
                <p className="flex items-center gap-2 text-sm font-medium">
                  <span className="truncate">{record.exerciseName}</span>
                  <span className="shrink-0 text-xs font-normal text-fg-subtle">
                    {formatMonthDay(record.recordDate)}
                  </span>
                </p>
                <p className="tabular mt-0.5 text-xs text-fg-muted">
                  {record.sets
                    .map((set) =>
                      record.cardio
                        ? `${trimNumber(set.durationMin, 1)}分`
                        : `${trimNumber(set.weightKg)}kg×${set.reps}`,
                    )
                    .join('  ')}
                </p>
              </div>
              <button
                type="button"
                aria-label="删除"
                onClick={() => removeRecord(record)}
                className="flex size-8 shrink-0 items-center justify-center rounded-full text-fg-subtle active:bg-surface-2"
              >
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </Card>
      )}

      <WorkoutForm
        open={formOpen}
        onClose={closeForm}
        onSaved={() => {
          records.reload()
          parts.reload()
        }}
        exercise={active}
        cardio={part?.cardio ?? false}
      />
    </>
  )
}
