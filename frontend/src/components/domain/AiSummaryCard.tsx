import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, RefreshCw, Sparkles } from 'lucide-react'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { IconWell } from '@/components/ui/IconWell'
import { aiApi } from '@/api/ai'
import { ApiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'

/**
 * 首页的 AI 总结卡。
 * 读的是后端缓存，打开首页不会每次都打一次模型；只有点「重新生成」才重新调用。
 */
export function AiSummaryCard() {
  const showToast = useUiStore((state) => state.showToast)

  const status = useAsync(() => aiApi.status(), [])
  const summary = useAsync(() => aiApi.todaySummary(), [])
  const [generating, setGenerating] = useState(false)

  const configured = status.data?.configured ?? false

  const generate = async () => {
    setGenerating(true)
    try {
      await aiApi.regenerateSummary()
      summary.reload()
      showToast('已生成', 'success')
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : '生成失败', 'danger')
    } finally {
      setGenerating(false)
    }
  }

  const content = summary.data?.content

  return (
    // 凹陷而不是凸起：这是机器给的一段解读，不是你自己录进去的数据，
    // 在质感上把它和其余凸起的块区分开。
    <Card variant="inset" className="p-5">
      <div className="mb-4 flex items-center justify-between gap-2">
        <Link to="/ai" className="flex items-center gap-2.5">
          <IconWell icon={Sparkles} tone="primary" size="sm" />
          <span className="text-[13px] font-medium text-primary">AI 今日总结</span>
          <ArrowRight size={12} className="text-primary/60" />
        </Link>
        {configured && content ? (
          <button
            type="button"
            onClick={generate}
            disabled={generating}
            aria-label="重新生成"
            className="flex size-8 items-center justify-center rounded-pill text-fg-subtle shadow-raised-sm neu-pressable disabled:opacity-40"
          >
            <RefreshCw size={13} className={generating ? 'animate-spin' : undefined} />
          </button>
        ) : null}
      </div>

      {!configured ? (
        <p className="text-[13.5px] leading-[1.8] text-fg-muted">
          还没有配置 DeepSeek API Key。把 Key 填进{' '}
          <code className="rounded-tile bg-surface px-1.5 py-0.5 text-xs shadow-inset">backend/.env</code>{' '}
          并重启后端，这里就会根据你的真实数据生成总结。
        </p>
      ) : content ? (
        <>
          {/* 是一整段话，不是列表也不是表格，所以排版的唯一任务就是「好读」：
              字号比正文略小半号、行高放到 1.8、字色用次级灰而不是浅灰。 */}
          <p className="text-[13.5px] leading-[1.8] whitespace-pre-wrap text-fg-muted">
            {content}
          </p>
          {summary.data?.generatedAt ? (
            <p className="mt-3.5 text-[11px] text-fg-subtle">
              生成于 {summary.data.generatedAt.slice(11, 16)}
              {summary.data.cached ? ' · 缓存' : ''}
            </p>
          ) : null}
        </>
      ) : (
        <div className="flex flex-col items-start gap-3">
          <p className="text-[13.5px] leading-[1.8] text-fg-muted">
            根据今天的任务、体重、训练和记账生成一段总结与建议。
          </p>
          <Button size="sm" loading={generating} onClick={generate}>
            生成今日总结
          </Button>
        </div>
      )}
    </Card>
  )
}
