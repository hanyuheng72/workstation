import { useEffect, useState } from 'react'
import { Check, Timer, X } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { IconWell } from '@/components/ui/IconWell'
import { StatTile } from '@/components/ui/StatTile'
import { ChartFrame } from '@/components/charts/ChartFrame'
import { BarSingle, type CountPoint } from '@/components/charts/BarSingle'
import { FocusSetupSheet } from '@/components/domain/FocusSetupSheet'
import { FocusTimerCard } from '@/components/domain/FocusTimerCard'
import { FocusHistoryList } from '@/components/domain/FocusHistoryList'
import { focusApi } from '@/api/focus'
import { ApiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useFocusSession } from '@/hooks/useFocusSession'
import { useUiStore } from '@/store/uiStore'
import { cn } from '@/utils/cn'
import { failReasonLabel, formatMinutes, minutesOf, shortDateLabel } from '@/utils/focus'
import type { FocusSessionVO, FocusStartRequest } from '@/types/domain'

/**
 * 自习室。
 *
 * 三种形态共用一块位置：没有进行中的场次时是开始入口，有就是倒计时，
 * 刚结束时是结果。倒计时本身由 useFocusSession 维护，这一层只负责摆放与反馈。
 *
 * 计时期间离开这个页面不影响结果——剩下的秒数是服务端按真实时钟给的，
 * 回到这里（或切回前台）会重新对齐。
 */
export function FocusRoomPage() {
  const showToast = useUiStore((state) => state.showToast)
  const { session, remaining, loading, busy, start, pause, resume, finishEarly, abandon } =
    useFocusSession()
  const [setupOpen, setSetupOpen] = useState(false)

  const stats = useAsync(() => focusApi.stats(), [], { key: 'focus:stats' })
  const history = useAsync(() => focusApi.list(), [], { key: 'focus:history' })

  const sessionId = session?.id ?? null
  const sessionStatus = session?.status ?? null
  const failReason = session?.failReason ?? null

  // 场次一变就把统计和记录刷新一遍。自动结算（时间走满自己结束）走的也是这里。
  useEffect(() => {
    if (sessionId === null) return
    stats.reload()
    history.reload()
  }, [sessionId, sessionStatus, stats.reload, history.reload])

  // 结束的反馈放在这里而不是各个按钮里，这样自动结算也会有一致的提示
  useEffect(() => {
    if (sessionStatus === 'SUCCESS') {
      showToast('专注完成，记进今天了', 'success')
    } else if (sessionStatus === 'FAILED') {
      showToast(`专注失败：${failReasonLabel(failReason)}`, 'danger')
    }
  }, [sessionId, sessionStatus, failReason, showToast])

  const handleStart = async (payload: FocusStartRequest) => {
    try {
      await start(payload)
      setSetupOpen(false)
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : '开始失败，请重试', 'danger')
    }
  }

  const handlePause = async () => {
    // 机会只有一次。第二次按下去这一场就判失败，所以先问一句，
    // 免得手滑把刚学的大半场送掉。
    if (
      session?.pauseUsed &&
      !window.confirm('暂停机会已经用掉了。再暂停这一场就判为失败，确定吗？')
    ) {
      return
    }
    try {
      await pause()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : '暂停失败，请重试', 'danger')
    }
  }

  const handleResume = async () => {
    try {
      await resume()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : '继续失败，请重试', 'danger')
    }
  }

  /** 提前学完也记成功，所以文案里说清后果；确认是为了防误触，不是为了拦人 */
  const handleFinishEarly = async () => {
    if (!window.confirm('提前结束这一场？会记为一次成功，时长按实际学的时间算。')) return
    try {
      await finishEarly()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : '操作失败，请重试', 'danger')
    }
  }

  const handleAbandon = async () => {
    if (!window.confirm('放弃这一场？会记为一次失败。')) return
    try {
      await abandon()
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : '操作失败，请重试', 'danger')
    }
  }

  const summary = stats.data
  const daily: CountPoint[] = (summary?.daily ?? []).map((row) => ({
    key: row.date,
    label: shortDateLabel(row.date),
    value: row.minutes,
  }))

  return (
    <>
      <PageHeader title="自习室" back subtitle="一场只做一件事，中途只有一次暂停机会" />

      <div className="flex flex-col gap-5">
        {/* 今日与累计 */}
        <Card className="p-5">
          {/* 取不到就说取不到。这里曾经会退化成三个 0，看起来像「今天没学」——
              但一次 500 和真的没记录是两回事，不能混在同一个数字里。 */}
          {stats.error ? (
            <p className="text-sm text-danger">统计没取到，稍后重试。</p>
          ) : (
            <div className="grid grid-cols-3 gap-3">
              <StatTile
                label="今日专注"
                value={summary?.todayMinutes ?? 0}
                unit="分钟"
                hint={
                  summary && summary.todayFailCount > 0
                    ? `失败 ${summary.todayFailCount} 场`
                    : undefined
                }
              />
              <StatTile
                label="今日完成"
                value={summary?.todaySuccessCount ?? 0}
                unit="场"
                hint={`累计 ${summary?.totalMinutes ?? 0} 分钟`}
              />
              <StatTile
                label="成功率"
                value={summary?.successRate ?? 0}
                unit="%"
                hint={
                  summary
                    ? `${summary.totalSuccessCount} 成 ${summary.totalFailCount} 败`
                    : undefined
                }
              />
            </div>
          )}
        </Card>

        {/* 进行中 / 刚结束 / 开始入口，三者共用这一块 */}
        {loading ? (
          <Card className="flex min-h-[120px] items-center justify-center p-6">
            <span className="size-6 animate-spin rounded-full border-2 border-line border-t-primary" />
          </Card>
        ) : session && (session.status === 'RUNNING' || session.status === 'PAUSED') ? (
          <FocusTimerCard
            session={session}
            remaining={remaining}
            busy={busy}
            onPause={handlePause}
            onResume={handleResume}
            onFinishEarly={handleFinishEarly}
            onAbandon={handleAbandon}
          />
        ) : session ? (
          <ResultCard session={session} onAgain={() => setSetupOpen(true)} />
        ) : (
          <Card className="flex flex-col items-center gap-3 p-6 text-center">
            <IconWell icon={Timer} tone="primary" />
            <p className="text-[15px] font-medium">现在开始一场专注</p>
            <p className="max-w-xs text-sm leading-relaxed text-fg-muted">
              设定一段时长，写下这一场要学的东西。倒计时走满才算完成。
            </p>
            <Button block size="lg" className="mt-1" onClick={() => setSetupOpen(true)}>
              开始专注
            </Button>
          </Card>
        )}

        <ChartFrame
          title="每日专注时长"
          subtitle="最近 30 天，只算完成的场次"
          isEmpty={daily.length === 0}
          emptyHint={stats.error ? '统计没取到，稍后重试' : '还没有专注记录，开始第一场吧'}
        >
          <BarSingle points={daily} unit="分" seriesName="专注" />
        </ChartFrame>

        <FocusHistoryList
          sessions={history.data ?? []}
          loading={history.loading}
          error={history.error}
        />
      </div>

      <FocusSetupSheet
        open={setupOpen}
        busy={busy}
        onClose={() => setSetupOpen(false)}
        onStart={handleStart}
      />
    </>
  )
}

/** 刚结束的那一场。留在这里直到开始下一场，免得结算完页面立刻变空 */
function ResultCard({ session, onAgain }: { session: FocusSessionVO; onAgain: () => void }) {
  const success = session.status === 'SUCCESS'
  const focused = formatMinutes(minutesOf(session.actualSeconds))

  return (
    <Card className="flex flex-col items-center gap-2 p-6 text-center">
      <IconWell icon={success ? Check : X} tone={success ? 'success' : 'muted'} />
      <p className={cn('text-[15px] font-medium', success ? 'text-success' : 'text-danger')}>
        {success ? (session.endedEarly ? '提前完成' : '专注完成') : '专注失败'}
      </p>
      <p className="text-sm break-words text-fg-muted">{session.subject}</p>
      <p className="text-xs text-fg-subtle">
        {success
          ? session.endedEarly
            ? `提前结束，实际专注 ${focused}，已记进今天。`
            : `${focused}走满了，已记进今天的专注时长。`
          : failReasonLabel(session.failReason)}
      </p>
      <Button block size="lg" className="mt-3" onClick={onAgain}>
        再来一场
      </Button>
    </Card>
  )
}
