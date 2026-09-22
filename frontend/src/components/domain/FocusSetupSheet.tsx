import { useEffect, useState } from 'react'
import { Sheet } from '@/components/ui/Sheet'
import { Button } from '@/components/ui/Button'
import { ChipGroup } from '@/components/ui/ChipGroup'
import { Field, Input } from '@/components/ui/Field'
import { parseNumber } from '@/utils/format'
import type { FocusStartRequest } from '@/types/domain'

const PRESETS = [25, 45, 60, 90]
const MAX_MINUTES = 600

/**
 * 开始一场专注。学习内容不记忆上一次——那是历史里的事，
 * 带进新的一场只会让人下意识沿用旧目标。
 */
export function FocusSetupSheet({
  open,
  busy,
  onClose,
  onStart,
}: {
  open: boolean
  busy: boolean
  onClose: () => void
  onStart: (payload: FocusStartRequest) => void
}) {
  const [subject, setSubject] = useState('')
  const [preset, setPreset] = useState<number | null>(45)
  const [custom, setCustom] = useState('')

  useEffect(() => {
    if (!open) return
    setSubject('')
    setPreset(45)
    setCustom('')
  }, [open])

  // 填了自定义分钟数就以它为准，否则用选中的快捷档
  const useCustom = custom.trim() !== ''
  const minutes = useCustom ? parseNumber(custom) : preset
  const valid =
    subject.trim().length > 0 &&
    minutes !== null &&
    minutes >= 1 &&
    minutes <= MAX_MINUTES

  const submit = () => {
    if (!valid || minutes === null) return
    onStart({ subject: subject.trim(), plannedMinutes: minutes })
  }

  return (
    <Sheet open={open} onClose={onClose} title="开始一场专注">
      <div className="flex flex-col gap-5">
        <Field label="这一场学什么">
          <Input
            value={subject}
            maxLength={100}
            placeholder="例如：线性代数第三章"
            onChange={(event) => setSubject(event.target.value)}
          />
        </Field>

        <Field
          label="专注时长"
          hint="中途只有一次暂停机会。用掉之后再按暂停，这一场就判为失败。"
        >
          <div className="flex flex-col gap-3">
            <ChipGroup
              columns={2}
              value={useCustom ? null : preset}
              options={PRESETS.map((value) => ({ value, label: `${value} 分钟` }))}
              onChange={(value) => {
                setPreset(value)
                setCustom('')
              }}
            />
            <div className="flex items-center gap-3">
              <span className="shrink-0 text-xs text-fg-subtle">或自己填</span>
              <Input
                className="w-24"
                inputMode="numeric"
                placeholder="分钟"
                value={custom}
                onChange={(event) => setCustom(event.target.value.replace(/\D/g, ''))}
              />
              <span className="shrink-0 text-xs text-fg-subtle">1 ~ {MAX_MINUTES} 分钟</span>
            </div>
          </div>
        </Field>

        <Button block size="lg" loading={busy} disabled={!valid} onClick={submit}>
          开始专注
        </Button>
      </div>
    </Sheet>
  )
}
