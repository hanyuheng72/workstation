import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { cn } from '@/utils/cn'
import { MIN_EARLY_SECONDS, formatCountdown } from '@/utils/focus'
import type { FocusSessionVO } from '@/types/domain'

/**
 * 正在进行的那一场。整页的主角是那个倒计时数字，其余元素都往后退。
 *
 * 暂停按钮在机会用掉之后依然可点——这是刻意的：规则是「机会用完了还想暂停
 * 就判失败」，按钮既然失效就没人能触发失败，那条规则也就成了空话。
 * 二次确认放在页面层，这里只负责把状态说清楚。
 *
 * 提前结束则反过来：没学满 1 分钟就置灰。不是为了拦人，而是「开一下点一下
 * 就算成功」会让成功这件事本身失去意义；与服务端同一道门槛。
 */
export function FocusTimerCard({
  session,
  remaining,
  busy,
  onPause,
  onResume,
  onFinishEarly,
  onAbandon,
}: {
  session: FocusSessionVO
  remaining: number
  busy: boolean
  onPause: () => void
  onResume: () => void
  onFinishEarly: () => void
  onAbandon: () => void
}) {
  const paused = session.status === 'PAUSED'
  const total = session.plannedMinutes * 60
  // 暂停期间 remaining 是冻住的，所以这个差值天然就是「已专注」而不含暂停
  const elapsed = Math.min(Math.max(total - remaining, 0), total)
  const percent = total === 0 ? 0 : Math.round((elapsed / total) * 100)
  const earlyLocked = elapsed < MIN_EARLY_SECONDS

  return (
    <Card className="p-6">
      <div className="flex items-center justify-between">
        <span className={cn('text-[11px]', paused ? 'text-warning' : 'text-primary')}>
          {paused ? '已暂停' : '专注中'}
        </span>
        <span className="tabular text-[11px] text-fg-subtle">
          共 {session.plannedMinutes} 分钟
        </span>
      </div>

      <p className="mt-2 text-[15px] font-medium break-words">{session.subject}</p>

      <p
        className={cn(
          'display tabular mt-6 text-center text-[3.25rem] leading-none font-semibold',
          paused ? 'text-fg-subtle' : 'text-fg',
        )}
      >
        {formatCountdown(remaining)}
      </p>

      {/* 进度槽：填充部分嵌在里面，和首页今日任务那条用同一套做法 */}
      <div className="mt-7 h-2 overflow-hidden rounded-pill bg-surface shadow-inset">
        <div
          className={cn(
            'h-full rounded-pill transition-[width] duration-300',
            paused ? 'bg-warning' : 'bg-primary',
          )}
          style={{ width: `${percent}%` }}
        />
      </div>
      <div className="mt-2 flex justify-between text-[11px] text-fg-subtle">
        <span className="tabular">已过 {formatCountdown(elapsed)}</span>
        <span className="tabular">{percent}%</span>
      </div>

      {/* 这条规则会被忘掉，所以每次都写出来 */}
      <p
        className={cn(
          'mt-5 text-xs leading-relaxed',
          session.pauseUsed && !paused ? 'text-danger' : 'text-fg-subtle',
        )}
      >
        {paused
          ? '这次暂停已经用掉。再按一次暂停，这一场就判为失败。'
          : session.pauseUsed
            ? '暂停机会已经用掉了 —— 再按一次暂停，这一场就判为失败。'
            : '这一场还有一次暂停机会。'}
      </p>

      <div className="mt-4 flex flex-col gap-3">
        {paused ? (
          <Button block loading={busy} onClick={onResume}>
            继续
          </Button>
        ) : (
          <Button block variant="secondary" disabled={busy} onClick={onPause}>
            暂停
          </Button>
        )}

        {/* 两个「结束这一场」的动作并排：上面那个算成功，右边那个算失败 */}
        <div className="flex gap-3">
          <Button
            variant="secondary"
            className="flex-1"
            disabled={busy || earlyLocked}
            onClick={onFinishEarly}
          >
            提前学完了
          </Button>
          <Button variant="ghost" className="shrink-0" disabled={busy} onClick={onAbandon}>
            放弃
          </Button>
        </div>

        {earlyLocked ? (
          <p className="text-center text-[11px] text-fg-subtle">
            提前结束要学满 1 分钟才能用
          </p>
        ) : null}
      </div>
    </Card>
  )
}
