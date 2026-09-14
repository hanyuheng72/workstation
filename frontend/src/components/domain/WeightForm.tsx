import { useEffect, useState } from 'react'
import { Sheet } from '@/components/ui/Sheet'
import { Button } from '@/components/ui/Button'
import { Field, Input, TextArea } from '@/components/ui/Field'
import { weightApi } from '@/api/weight'
import { ApiError } from '@/api/client'
import { useUiStore } from '@/store/uiStore'
import { parseNumber } from '@/utils/format'
import { todayISO } from '@/utils/date'
import type { WeightVO } from '@/types/domain'

interface WeightFormProps {
  open: boolean
  onClose: () => void
  onSaved: () => void
  editing?: WeightVO | null
}

/**
 * 记录体重。不带日期选择 —— 记的就是今天。
 * 编辑历史记录时保留它原本的日期，不会被挪到今天。
 */
export function WeightForm({ open, onClose, onSaved, editing }: WeightFormProps) {
  const showToast = useUiStore((state) => state.showToast)

  const [weight, setWeight] = useState('')
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setWeight(editing ? String(editing.weightKg) : '')
    setNote(editing?.note ?? '')
    setError(null)
  }, [open, editing])

  const submit = async () => {
    const value = parseNumber(weight)
    if (value === null) {
      setError('请填写体重')
      return
    }

    setSaving(true)
    setError(null)
    try {
      const payload = {
        recordDate: editing?.recordDate ?? todayISO(),
        weightKg: value,
        note: note.trim() || null,
      }
      if (editing) {
        await weightApi.update(editing.id, payload)
      } else {
        await weightApi.upsert(payload)
      }
      showToast('已记录', 'success')
      onSaved()
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={open} onClose={onClose} title={editing ? '修改体重' : '记录体重'}>
      <div className="flex flex-col gap-4">
        <Field
          label="体重"
          hint={editing ? `记在 ${editing.recordDate}` : '记在今天，同一天重复填写会覆盖'}
        >
          <div className="relative">
            <Input
              type="number"
              inputMode="decimal"
              step="0.1"
              placeholder="65.3"
              value={weight}
              onChange={(event) => setWeight(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  void submit()
                }
              }}
              autoFocus
              className="pr-12 text-lg"
            />
            <span className="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-sm text-fg-subtle">
              kg
            </span>
          </div>
        </Field>

        <Field label="备注" hint="可选">
          <TextArea
            placeholder="早上空腹 / 运动后…"
            value={note}
            onChange={(event) => setNote(event.target.value)}
          />
        </Field>

        {error ? <p className="text-sm text-danger">{error}</p> : null}

        <Button size="lg" block loading={saving} onClick={submit}>
          保存
        </Button>
      </div>
    </Sheet>
  )
}
