import { useEffect, useMemo, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { Sheet } from '@/components/ui/Sheet'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { workoutApi } from '@/api/workout'
import { ApiError } from '@/api/client'
import { useUiStore } from '@/store/uiStore'
import { parseNumber, trimNumber } from '@/utils/format'
import { todayISO } from '@/utils/date'
import type { ExerciseVO, WorkoutVO } from '@/types/domain'

interface WorkoutFormProps {
  open: boolean
  onClose: () => void
  onSaved: () => void
  exercise: ExerciseVO | null
  cardio: boolean
}

interface StrengthRow {
  weight: string
  reps: string
}

interface CardioRow {
  duration: string
  distance: string
}

export function WorkoutForm({ open, onClose, onSaved, exercise, cardio }: WorkoutFormProps) {
  const showToast = useUiStore((state) => state.showToast)

  const [strength, setStrength] = useState<StrengthRow[]>([{ weight: '', reps: '' }])
  const [cardioRows, setCardioRows] = useState<CardioRow[]>([{ duration: '', distance: '' }])
  const [note, setNote] = useState('')
  const [last, setLast] = useState<WorkoutVO | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open || !exercise) return
    setStrength([{ weight: '', reps: '' }])
    setCardioRows([{ duration: '', distance: '' }])
    setNote('')
    setError(null)
    setLast(null)

    // 拉上次同动作的记录，边填边对照
    workoutApi
      .lastRecord(exercise.id)
      .then(setLast)
      .catch(() => setLast(null))
  }, [open, exercise])

  const lastSummary = useMemo(() => {
    if (!last) return null
    if (last.cardio) {
      const minutes = last.sets.reduce((sum, set) => sum + (set.durationMin ?? 0), 0)
      const distance = last.sets.reduce((sum, set) => sum + (set.distanceKm ?? 0), 0)
      return `${last.recordDate}  ${trimNumber(minutes, 1)} 分钟${distance > 0 ? ` · ${trimNumber(distance, 2)} km` : ''}`
    }
    const top = last.sets.reduce((best, set) => {
      if (set.weightKg === null) return best
      if (!best || set.weightKg > best.weightKg!) return set
      return best
    }, null as typeof last.sets[number] | null)
    return top ? `${last.recordDate}  ${trimNumber(top.weightKg)}kg × ${top.reps}` : null
  }, [last])

  const submit = async () => {
    if (!exercise) return

    setError(null)
    const payload = cardio
      ? {
          recordDate: todayISO(),
          exerciseId: exercise.id,
          sets: cardioRows
            .map((row) => ({
              durationMin: parseNumber(row.duration),
              distanceKm: parseNumber(row.distance),
            }))
            .filter((row) => row.durationMin !== null || row.distanceKm !== null),
          note: note.trim() || null,
        }
      : {
          recordDate: todayISO(),
          exerciseId: exercise.id,
          sets: strength
            .map((row) => ({ weightKg: parseNumber(row.weight), reps: parseNumber(row.reps) }))
            .filter((row) => row.weightKg !== null || row.reps !== null),
          note: note.trim() || null,
        }

    if (payload.sets.length === 0) {
      setError(cardio ? '至少填一组时长或距离' : '至少填一组重量和次数')
      return
    }

    setSaving(true)
    try {
      const saved = await workoutApi.create(payload)
      // 保存后立刻和上次比，把结论直接告诉用户
      const comparison = await workoutApi.comparison(saved.id).catch(() => null)
      showToast(comparison?.message ?? '已记录', 'success')
      onSaved()
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={open} onClose={onClose} title={exercise ? `记录 · ${exercise.name}` : '记录训练'}>
      <div className="flex flex-col gap-4">
        {lastSummary ? (
          <div className="rounded-tile bg-primary-soft px-3.5 py-2.5 text-xs text-primary">
            上次：{lastSummary}
          </div>
        ) : null}

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-fg-muted">
              {cardio ? '分段（时长 / 距离）' : '组数（重量 / 次数）'}
            </span>
            <button
              type="button"
              onClick={() =>
                cardio
                  ? setCardioRows((rows) => [...rows, { duration: '', distance: '' }])
                  : setStrength((rows) => [...rows, { weight: '', reps: '' }])
              }
              className="flex items-center gap-1 text-xs font-medium text-primary"
            >
              <Plus size={14} />
              加一组
            </button>
          </div>

          {cardio
            ? cardioRows.map((row, index) => (
                <div key={index} className="flex items-center gap-2">
                  <span className="w-6 shrink-0 text-center text-xs text-fg-subtle">{index + 1}</span>
                  <Input
                    type="number"
                    inputMode="decimal"
                    placeholder="分钟"
                    value={row.duration}
                    onChange={(event) =>
                      setCardioRows((rows) =>
                        rows.map((item, i) => (i === index ? { ...item, duration: event.target.value } : item)),
                      )
                    }
                  />
                  <Input
                    type="number"
                    inputMode="decimal"
                    placeholder="公里"
                    value={row.distance}
                    onChange={(event) =>
                      setCardioRows((rows) =>
                        rows.map((item, i) => (i === index ? { ...item, distance: event.target.value } : item)),
                      )
                    }
                  />
                  <RemoveButton
                    disabled={cardioRows.length === 1}
                    onClick={() => setCardioRows((rows) => rows.filter((_, i) => i !== index))}
                  />
                </div>
              ))
            : strength.map((row, index) => (
                <div key={index} className="flex items-center gap-2">
                  <span className="w-6 shrink-0 text-center text-xs text-fg-subtle">{index + 1}</span>
                  <Input
                    type="number"
                    inputMode="decimal"
                    step="0.5"
                    placeholder="kg"
                    value={row.weight}
                    onChange={(event) =>
                      setStrength((rows) =>
                        rows.map((item, i) => (i === index ? { ...item, weight: event.target.value } : item)),
                      )
                    }
                  />
                  <Input
                    type="number"
                    inputMode="numeric"
                    placeholder="次"
                    value={row.reps}
                    onChange={(event) =>
                      setStrength((rows) =>
                        rows.map((item, i) => (i === index ? { ...item, reps: event.target.value } : item)),
                      )
                    }
                  />
                  <RemoveButton
                    disabled={strength.length === 1}
                    onClick={() => setStrength((rows) => rows.filter((_, i) => i !== index))}
                  />
                </div>
              ))}
        </div>

        <Field label="备注" hint="可选">
          <Input
            placeholder="状态、感受…"
            value={note}
            onChange={(event) => setNote(event.target.value)}
          />
        </Field>

        {error ? <p className="text-sm text-danger">{error}</p> : null}

        <Button size="lg" block loading={saving} onClick={submit} disabled={!exercise}>
          保存
        </Button>
      </div>
    </Sheet>
  )
}

function RemoveButton({ disabled, onClick }: { disabled: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-label="删除这一组"
      disabled={disabled}
      onClick={onClick}
      className="flex size-9 shrink-0 items-center justify-center rounded-full text-fg-subtle active:bg-surface-2 disabled:opacity-30"
    >
      <Trash2 size={16} />
    </button>
  )
}
