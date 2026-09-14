import { useEffect, useState } from 'react'
import { Sheet } from '@/components/ui/Sheet'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { Segmented } from '@/components/ui/Segmented'
import { taskApi } from '@/api/task'
import { ApiError } from '@/api/client'
import { useUiStore } from '@/store/uiStore'
import type { TaskRequest, TaskType, TaskVO } from '@/types/domain'

interface TaskFormProps {
  open: boolean
  onClose: () => void
  onSaved: () => void
  editing?: TaskVO | null
  defaultType?: TaskType
}

/**
 * 任务表单。只留标题、类型、备注三样 —— 日期固定为今天，重复规则也交给
 * AI 助手去设（「提醒我每天背单词」），日常手动加任务用不到这两个。
 *
 * 编辑已有任务时，日期与重复规则会**原样回传**：
 * 后端把「字段为空」理解为「改成默认值」，不回传就会把任务悄悄挪到今天、
 * 或者把重复任务改成不重复。
 */
export function TaskForm({ open, onClose, onSaved, editing, defaultType = 'TODAY' }: TaskFormProps) {
  const showToast = useUiStore((state) => state.showToast)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [type, setType] = useState<TaskType>(defaultType)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setTitle(editing?.title ?? '')
    setDescription(editing?.description ?? '')
    setType(editing?.taskType ?? defaultType)
    setError(null)
  }, [open, editing, defaultType])

  const submit = async () => {
    if (!title.trim()) {
      setError('请填写任务内容')
      return
    }

    const payload: TaskRequest = {
      title: title.trim(),
      description: description.trim() || null,
      taskType: type,
      priority: editing?.priority ?? 0,
      // 新建时不传日期，后端补今天；编辑时把原日期带回去，避免挪到今天
      planDate: editing?.planDate ?? null,
      dueDate: editing?.dueDate ?? null,
      // 重复规则表单里不提供，但编辑时必须原样保留
      recurrenceType: editing?.recurrenceType ?? 'NONE',
      recurrenceInterval: editing?.recurrenceInterval ?? 1,
      recurrenceEndDate: editing?.recurrenceEndDate ?? null,
    }

    setSaving(true)
    setError(null)
    try {
      if (editing) {
        await taskApi.update(editing.id, payload)
      } else {
        await taskApi.create(payload)
      }
      showToast(editing ? '已保存' : '已添加', 'success')
      onSaved()
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={open} onClose={onClose} title={editing ? '编辑任务' : '添加任务'}>
      <div className="flex flex-col gap-4">
        <input
          value={title}
          onChange={(event) => {
            setTitle(event.target.value)
            if (error) setError(null)
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              void submit()
            }
          }}
          placeholder="要做什么？"
          aria-label="任务内容"
          autoFocus
          className="w-full rounded-tile bg-surface px-4 py-3.5 text-base shadow-inset outline-none placeholder:text-fg-subtle focus:shadow-inset-deep focus:outline-2 focus:outline-offset-0 focus:outline-primary"
        />

        <Field label="类型">
          <Segmented
            value={type}
            onChange={setType}
            options={[
              { value: 'TODAY' as TaskType, label: '今日' },
              { value: 'LONG_TERM' as TaskType, label: '长期' },
            ]}
          />
        </Field>

        <Field label="备注" hint="可选">
          <Input
            placeholder="补充说明"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </Field>

        {error ? <p className="text-sm text-danger">{error}</p> : null}

        <Button size="lg" block loading={saving} onClick={submit}>
          {editing ? '保存' : '添加'}
        </Button>
      </div>
    </Sheet>
  )
}
