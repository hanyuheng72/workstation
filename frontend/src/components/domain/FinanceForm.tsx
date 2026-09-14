import { useEffect, useState } from 'react'
import { Sheet } from '@/components/ui/Sheet'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { Segmented } from '@/components/ui/Segmented'
import { ChipGroup } from '@/components/ui/ChipGroup'
import { financeApi } from '@/api/finance'
import { ApiError } from '@/api/client'
import { useUiStore } from '@/store/uiStore'
import { useAsync } from '@/hooks/useAsync'
import { parseNumber } from '@/utils/format'
import { todayISO } from '@/utils/date'
import type { TransactionType } from '@/types/domain'

interface FinanceFormProps {
  open: boolean
  onClose: () => void
  onSaved: () => void
}

export function FinanceForm({ open, onClose, onSaved }: FinanceFormProps) {
  const showToast = useUiStore((state) => state.showToast)

  const [type, setType] = useState<TransactionType>('EXPENSE')
  const [amount, setAmount] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 分类按方向分开拉，切到收入时不会残留支出的选中项
  const categories = useAsync(() => financeApi.categories(type), [type, open])

  useEffect(() => {
    if (!open) return
    setAmount('')
    setNote('')
    setError(null)
  }, [open])

  useEffect(() => {
    setCategoryId(null)
  }, [type])

  // 默认选中第一个分类，少一次点击
  useEffect(() => {
    if (categoryId === null && categories.data && categories.data.length > 0) {
      setCategoryId(categories.data[0].id)
    }
  }, [categories.data, categoryId])

  const submit = async () => {
    const value = parseNumber(amount)
    if (value === null || value <= 0) {
      setError('请填写金额')
      return
    }
    if (categoryId === null) {
      setError('请选择分类')
      return
    }

    setSaving(true)
    setError(null)
    try {
      await financeApi.create({
        type,
        amount: value,
        categoryId,
        occurDate: todayISO(),
        note: note.trim() || null,
      })
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
    <Sheet open={open} onClose={onClose} title="记一笔">
      <div className="flex flex-col gap-4">
        <Segmented
          value={type}
          onChange={(value) => setType(value)}
          options={[
            { value: 'EXPENSE', label: '支出' },
            { value: 'INCOME', label: '收入' },
          ]}
        />

        <Field label="金额">
          <div className="relative">
            <span className="absolute top-1/2 left-3.5 -translate-y-1/2 text-lg text-fg-subtle">¥</span>
            <Input
              type="number"
              inputMode="decimal"
              step="0.01"
              placeholder="0.00"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              autoFocus
              className="pl-8 text-2xl font-semibold"
            />
          </div>
        </Field>

        <Field label="分类">
          {categories.data && categories.data.length > 0 ? (
            <ChipGroup
              value={categoryId}
              options={categories.data.map((category) => ({
                value: category.id,
                label: category.name,
              }))}
              onChange={setCategoryId}
            />
          ) : (
            <p className="text-sm text-fg-subtle">加载分类中…</p>
          )}
        </Field>

        <Field label="备注" hint="可选">
          <Input
            placeholder="午饭 / 地铁…"
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
