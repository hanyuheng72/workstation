import { Card } from '@/components/ui/Card'
import { cn } from '@/utils/cn'
import {
  failReasonLabel,
  focusStatusLabel,
  focusStatusTone,
  formatClock,
  formatMinutes,
  minutesOf,
  shortDateLabel,
} from '@/utils/focus'
import type { FocusSessionVO } from '@/types/domain'

const TONES = {
  success: 'text-success',
  danger: 'text-danger',
  primary: 'text-primary',
  muted: 'text-fg-subtle',
}

/**
 * 成功的场次显示**真正专注了多久**，不是当初设定的时长；
 * 提前结束的标出来，否则「计划 45 分钟只学了 12 分钟」在记录里看不出来。
 */
function successLabel(session: FocusSessionVO): string {
  const actual = formatMinutes(minutesOf(session.actualSeconds))
  return session.endedEarly ? `${actual} · 提前结束` : actual
}

/**
 * 专注记录。成功的显示时长，失败的显示原因——只写「失败」两个字，
 * 过几天就分不清是自己放弃的还是暂停机会用完了。
 */
export function FocusHistoryList({
  sessions,
  loading,
  error,
}: {
  sessions: FocusSessionVO[]
  loading: boolean
  /** 请求失败时要说失败，别退化成「还没有记录」——那是另一回事 */
  error?: string | null
}) {
  return (
    <Card className="p-5">
      <div className="mb-3 flex items-center justify-between">
        <span className="text-[15px] font-medium">专注记录</span>
        <span className="text-xs text-fg-subtle">最近 30 天</span>
      </div>

      {error ? (
        <p className="py-2 text-sm text-danger">记录没取到，稍后重试。</p>
      ) : loading && sessions.length === 0 ? (
        <p className="py-2 text-sm text-fg-subtle">载入中…</p>
      ) : sessions.length === 0 ? (
        <p className="py-2 text-sm text-fg-subtle">还没有记录。开始第一场吧。</p>
      ) : (
        <ul className="flex flex-col">
          {sessions.map((session) => (
            <li
              key={session.id}
              className="flex items-center gap-3 border-b border-line py-2.5 last:border-b-0"
            >
              <div className="w-12 shrink-0">
                <p className="tabular text-xs text-fg-muted">
                  {shortDateLabel(session.sessionDate)}
                </p>
                <p className="tabular text-[11px] text-fg-subtle">
                  {formatClock(session.startedAt)}
                </p>
              </div>

              <div className="min-w-0 flex-1">
                <p className="truncate text-[15px]">{session.subject}</p>
                <p className="text-[11px] text-fg-subtle">
                  {session.status === 'SUCCESS'
                    ? successLabel(session)
                    : session.status === 'FAILED'
                      ? failReasonLabel(session.failReason)
                      : `计划 ${session.plannedMinutes} 分钟`}
                </p>
              </div>

              <span
                className={cn('shrink-0 text-[11px]', TONES[focusStatusTone(session.status)])}
              >
                {focusStatusLabel(session.status)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
