import { useState } from 'react'
import { LineChart, Sparkles } from 'lucide-react'
import { Card } from '@/components/ui/Card'
import { Sheet } from '@/components/ui/Sheet'
import { aiApi } from '@/api/ai'
import { ApiError } from '@/api/client'
import { useUiStore } from '@/store/uiStore'

type Kind = 'expense' | 'workout'

const OPTIONS: { kind: Kind; label: string; hint: string }[] = [
  { kind: 'expense', label: '分析本月消费', hint: '花在哪了、有没有异常' },
  { kind: 'workout', label: '分析近期训练', hint: '频率与部位均衡' },
]

/** 专项分析。结果只读不落库，每次点都重新调用模型。 */
export function AiAnalysisCard() {
  const showToast = useUiStore((state) => state.showToast)
  const [running, setRunning] = useState<Kind | null>(null)
  const [open, setOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')

  const run = async (kind: Kind) => {
    setRunning(kind)
    try {
      const result =
        kind === 'expense' ? await aiApi.analyzeExpense() : await aiApi.analyzeWorkout()
      setTitle(kind === 'expense' ? '本月消费分析' : '近期训练分析')
      setContent(result.content)
      setOpen(true)
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : '分析失败', 'danger')
    } finally {
      setRunning(null)
    }
  }

  return (
    <>
      <Card className="p-4">
        <div className="mb-3 flex items-center gap-2">
          <LineChart size={16} className="text-primary" />
          <span className="text-sm font-medium">让我分析一下</span>
        </div>
        <div className="grid grid-cols-2 gap-2">
          {OPTIONS.map((option) => (
            <button
              key={option.kind}
              type="button"
              disabled={running !== null}
              onClick={() => void run(option.kind)}
              className="flex flex-col items-start gap-1 rounded-tile bg-surface shadow-inset p-3 text-left transition-transform active:scale-[0.98] disabled:opacity-50"
            >
              <span className="text-sm font-medium">
                {running === option.kind ? '分析中…' : option.label}
              </span>
              <span className="text-xs text-fg-subtle">{option.hint}</span>
            </button>
          ))}
        </div>
      </Card>

      <Sheet open={open} onClose={() => setOpen(false)} title={title}>
        <div className="flex items-start gap-2">
          <Sparkles size={16} className="mt-0.5 shrink-0 text-primary" />
          <p className="text-sm leading-relaxed whitespace-pre-wrap">{content}</p>
        </div>
      </Sheet>
    </>
  )
}
