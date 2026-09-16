import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ArrowUp, Bot, Brain, Check, Sparkles, TriangleAlert, X } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { AiAnalysisCard } from '@/components/domain/AiAnalysisCard'
import { AiMemorySheet } from '@/components/domain/AiMemorySheet'
import { aiApi } from '@/api/ai'
import { ApiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'
import type { AiMessageVO } from '@/types/domain'

const EXAMPLES = [
  '我今天体重 65.3 公斤',
  '今天做了卧推，60 公斤，4 组，每组 10 次',
  '今天吃饭花了 35 元',
  '我这个月花了多少钱？',
]

export function AiChatPage() {
  const showToast = useUiStore((state) => state.showToast)
  const [params, setParams] = useSearchParams()

  const status = useAsync(() => aiApi.status(), [], { key: 'ai:status' })

  const [messages, setMessages] = useState<AiMessageVO[]>([])
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [memoryOpen, setMemoryOpen] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  // 只加载今天的对话：记忆是长期的，但每天的聊天不该拖成一条无限长的流水
  useEffect(() => {
    aiApi
      .todayMessages()
      .then(setMessages)
      .catch(() => setMessages([]))
  }, [])

  useEffect(() => {
    const preset = params.get('ask')
    if (preset) {
      setInput(preset)
      params.delete('ask')
      setParams(params, { replace: true })
    }
  }, [params, setParams])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages.length, sending])

  const send = async (text: string) => {
    const message = text.trim()
    if (!message || sending) return

    setSending(true)
    setInput('')
    try {
      const result = await aiApi.chat(message, conversationId)
      setConversationId(result.conversationId)
      setMessages((prev) => [...prev, result.userMessage, result.assistantMessage])
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : '发送失败', 'danger')
      setInput(message)
    } finally {
      setSending(false)
    }
  }

  const resolveDraft = async (id: number, confirm: boolean) => {
    setBusyId(id)
    try {
      const updated = confirm ? await aiApi.confirm(id) : await aiApi.reject(id)
      setMessages((prev) => prev.map((item) => (item.id === id ? updated : item)))
      if (confirm) {
        showToast(
          updated.actionStatus === 'EXECUTED' ? '已完成' : '没能完成，请看提示',
          updated.actionStatus === 'EXECUTED' ? 'success' : 'danger',
        )
      }
    } catch {
      showToast('操作失败', 'danger')
    } finally {
      setBusyId(null)
    }
  }

  if (status.loading && !status.data) return <SplashScreen />

  const configured = status.data?.configured ?? false

  return (
    <div className="flex min-h-[calc(100dvh-9rem)] flex-col">
      <PageHeader
        title="AI 助手"
        subtitle={configured ? `用一句话记录，也能问我数据，模型：${status.data?.model}` : '还没有配置 DeepSeek'}
        action={
          configured ? (
            <button
              type="button"
              onClick={() => setMemoryOpen(true)}
              className="flex items-center gap-1.5 rounded-tile px-3 py-2 text-xs text-fg-muted shadow-raised-sm neu-pressable"
            >
              <Brain size={14} />
              记忆
            </button>
          ) : null
        }
      />

      {!configured ? (
        <Card className="p-4">
          <p className="text-sm font-medium">需要先配置 DeepSeek API Key</p>
          <p className="mt-2 text-sm text-fg-muted">
            打开 <code className="rounded-tile bg-surface px-1.5 py-0.5 text-xs shadow-inset">backend/.env</code>
            ，把 <code className="rounded-tile bg-surface px-1.5 py-0.5 text-xs shadow-inset">DEEPSEEK_API_KEY</code>{' '}
            填上，然后重启后端。
          </p>
          <p className="mt-2 text-xs text-fg-subtle">
            Key 只在后端读取，任何接口都不会把它发给前端。
          </p>
        </Card>
      ) : (
        <>
          <div className="mb-3">
            <AiAnalysisCard />
          </div>

          <div className="flex-1">
            {messages.length === 0 ? (
              <Card className="p-4">
                <div className="mb-3 flex items-center gap-2">
                  <Sparkles size={16} className="text-primary" />
                  <span className="text-sm font-medium">直接说人话就行</span>
                </div>
                <p className="mb-3 text-sm text-fg-muted">
                  我会先解析成草稿，<span className="font-medium text-fg">你确认之后才会真正记录</span>
                  ，不会擅自改你的数据。也可以直接问我问题，我能看到你的数据。
                </p>
                <div className="flex flex-col gap-2">
                  {EXAMPLES.map((example) => (
                    <button
                      key={example}
                      type="button"
                      onClick={() => void send(example)}
                      className="rounded-tile bg-surface px-4 py-3 text-left text-sm text-fg-muted shadow-raised-sm neu-pressable"
                    >
                      {example}
                    </button>
                  ))}
                </div>
              </Card>
            ) : (
              <div className="flex flex-col gap-3">
                {messages.map((message) => (
                  <MessageBubble
                    key={message.id}
                    message={message}
                    busy={busyId === message.id}
                    onResolve={resolveDraft}
                  />
                ))}
                {sending ? (
                  <div className="flex items-center gap-2 px-1 text-sm text-fg-subtle">
                    <span className="size-2 animate-pulse rounded-full bg-primary" />
                    正在理解…
                  </div>
                ) : null}
                <div ref={bottomRef} />
              </div>
            )}
          </div>

          {/* 输入区贴底，位于底部导航之上 */}
          <div className="sticky bottom-0 -mx-4 mt-5 bg-bg px-4 py-3 md:-mx-8 md:px-8">
            <div className="flex items-end gap-2">
              <textarea
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && !event.shiftKey) {
                    event.preventDefault()
                    void send(input)
                  }
                }}
                rows={1}
                placeholder="说点什么，回车发送"
                aria-label="给 AI 发消息"
                className="max-h-32 min-h-11 flex-1 resize-none rounded-tile bg-surface px-4 py-3 text-[15px] shadow-inset outline-none placeholder:text-fg-subtle focus:shadow-inset-deep focus:outline-2 focus:outline-offset-0 focus:outline-primary"
              />
              <Button
                size="md"
                aria-label="发送"
                disabled={!input.trim()}
                loading={sending}
                onClick={() => void send(input)}
                className="size-11 shrink-0 px-0"
              >
                {sending ? null : <ArrowUp size={18} />}
              </Button>
            </div>
          </div>
        </>
      )}

      <AiMemorySheet open={memoryOpen} onClose={() => setMemoryOpen(false)} />
    </div>
  )
}

function MessageBubble({
  message,
  busy,
  onResolve,
}: {
  message: AiMessageVO
  busy: boolean
  onResolve: (id: number, confirm: boolean) => void
}) {
  const isUser = message.role === 'USER'
  const pending = message.actionStatus === 'PENDING'
  const failed = message.actionStatus === 'FAILED'
  // 记忆和「记数据」共用同一套确认链路，但文案要分开——用户要知道自己在同意什么
  const isMemory = message.intent === 'SAVE_MEMORY'

  if (isUser) {
    return (
      <div className="flex justify-end">
        <div className="max-w-[85%] rounded-panel rounded-br-md bg-primary px-3.5 py-2.5 text-sm text-primary-fg">
          {message.content}
        </div>
      </div>
    )
  }

  return (
    <div className="flex gap-2.5">
      <span className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-pill bg-primary-soft text-primary">
        <Bot size={15} />
      </span>
      <div className="min-w-0 flex-1">
        <div className="rounded-panel rounded-tl-md bg-surface px-3.5 py-2.5 text-sm whitespace-pre-wrap shadow-raised-sm">
          {message.content}
        </div>

        {/* 待确认的草稿：确认之前不会动任何数据 */}
        {pending && message.draftPreview ? (
          <Card variant="inset" className="mt-2 p-3">
            <p className="flex items-center gap-1.5 text-xs text-fg-muted">
              {isMemory ? <Brain size={12} /> : null}
              {isMemory ? '要记住这条吗：' : '将要记录：'}
            </p>
            <p className="mt-1 text-sm font-medium">{message.draftPreview}</p>
            <div className="mt-3 flex gap-2">
              <Button size="sm" loading={busy} onClick={() => onResolve(message.id, true)}>
                <Check size={15} />
                {isMemory ? '记住' : '确认记录'}
              </Button>
              <Button
                size="sm"
                variant="secondary"
                disabled={busy}
                onClick={() => onResolve(message.id, false)}
              >
                <X size={15} />
                {isMemory ? '不用了' : '不用了'}
              </Button>
            </div>
          </Card>
        ) : null}

        {failed ? (
          <p className="mt-1.5 flex items-center gap-1.5 text-xs text-warning">
            <TriangleAlert size={13} />
            这条没能完成，改个说法再试一次
          </p>
        ) : null}

        {message.actionStatus === 'REJECTED' ? (
          <p className="mt-1.5 text-xs text-fg-subtle">已忽略</p>
        ) : null}
      </div>
    </div>
  )
}
