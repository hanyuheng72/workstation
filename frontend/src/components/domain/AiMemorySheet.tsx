import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { Sheet } from '@/components/ui/Sheet'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { aiApi } from '@/api/ai'
import { ApiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'

/** 记忆分类，与后端 AiMemoryService 里的取值对应 */
const CATEGORIES: { value: string; label: string }[] = [
  { value: 'PROFILE', label: '基本资料' },
  { value: 'GOAL', label: '目标' },
  { value: 'PREFERENCE', label: '偏好' },
  { value: 'HABIT', label: '习惯' },
  { value: 'OTHER', label: '其他' },
]

const LABEL_OF: Record<string, string> = Object.fromEntries(
  CATEGORIES.map((item) => [item.value, item.label]),
)

interface AiMemorySheetProps {
  open: boolean
  onClose: () => void
}

/**
 * 画像记忆的管理面板。
 *
 * 记忆是永久保留的，所以必须让用户能看到、能删——否则模型在聊天里
 * 攒了一堆关于他的判断，他却不知道里面写了什么。
 */
export function AiMemorySheet({ open, onClose }: AiMemorySheetProps) {
  const showToast = useUiStore((state) => state.showToast)
  const memories = useAsync(() => aiApi.memories(), [open], { key: 'ai:memories' })

  const [draft, setDraft] = useState('')
  const [category, setCategory] = useState('PROFILE')
  const [saving, setSaving] = useState(false)

  const add = async () => {
    const content = draft.trim()
    if (!content) return
    setSaving(true)
    try {
      await aiApi.addMemory(content, category)
      setDraft('')
      memories.reload()
      showToast('已记住', 'success')
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : '保存失败', 'danger')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (id: number, content: string) => {
    if (!window.confirm(`忘掉这条记忆？\n\n${content}`)) return
    try {
      await aiApi.removeMemory(id)
      memories.reload()
      showToast('已忘掉', 'success')
    } catch {
      showToast('删除失败', 'danger')
    }
  }

  const items = memories.data ?? []

  return (
    <Sheet open={open} onClose={onClose} title="AI 对我的记忆">
      <div className="flex flex-col gap-4">
        <p className="text-xs leading-relaxed text-fg-muted">
          这些是 AI 长期记住的关于你的事。它每次和你聊天、以及生成每日总结时都会参考，
          所以总结和建议会更有针对性。记忆永久保留，你可以随时删掉。
        </p>

        {/* 手动补一条：有些事不用等在聊天里说 */}
        <div className="flex items-end gap-2">
          <div className="min-w-0 flex-1">
            <input
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') void add()
              }}
              placeholder="比如：目标体重 70kg"
              aria-label="新增记忆"
              className="w-full rounded-tile bg-surface px-3.5 py-3 text-sm shadow-inset outline-none placeholder:text-fg-subtle focus:shadow-inset-deep focus:outline-2 focus:outline-offset-0 focus:outline-primary"
            />
          </div>
          <Button size="md" loading={saving} disabled={!draft.trim()} onClick={add}>
            <Plus size={16} />
          </Button>
        </div>

        <div className="flex flex-wrap gap-1.5">
          {CATEGORIES.map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => setCategory(item.value)}
              className={
                item.value === category
                  ? 'rounded-pill px-3 py-1.5 text-xs text-primary shadow-inset'
                  : 'rounded-pill px-3 py-1.5 text-xs text-fg-muted shadow-raised-xs neu-pressable'
              }
            >
              {item.label}
            </button>
          ))}
        </div>

        {memories.loading && !memories.data ? (
          <SplashScreen />
        ) : items.length === 0 ? (
          <EmptyState
            title="还没有任何记忆"
            description="在聊天中告诉 AI 关于你的事，它会问你「要记住吗」，确认后就会出现在这里。"
          />
        ) : (
          <ul className="flex flex-col gap-2">
            {items.map((memory) => (
              <li
                key={memory.id}
                className="flex items-start gap-3 rounded-tile bg-surface px-3.5 py-3 shadow-raised-xs"
              >
                <div className="min-w-0 flex-1">
                  <p className="text-sm leading-relaxed">{memory.content}</p>
                  <p className="mt-1 text-[11px] text-fg-subtle">
                    {LABEL_OF[memory.category] ?? '其他'}
                    {memory.source === 'CHAT' ? ' · 聊天中记下' : ' · 手动添加'}
                  </p>
                </div>
                <button
                  type="button"
                  aria-label="删除这条记忆"
                  onClick={() => remove(memory.id, memory.content)}
                  className="-mr-1 flex size-8 shrink-0 items-center justify-center rounded-tile text-fg-subtle active:bg-surface-2"
                >
                  <Trash2 size={15} />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Sheet>
  )
}
