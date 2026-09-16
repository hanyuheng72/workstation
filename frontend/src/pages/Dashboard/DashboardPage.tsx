import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Check, Coins, Dumbbell, Scale, Wallet, type LucideIcon } from 'lucide-react'
import { Card } from '@/components/ui/Card'
import { IconWell } from '@/components/ui/IconWell'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { AiSummaryCard } from '@/components/domain/AiSummaryCard'
import { dashboardApi } from '@/api/dashboard'
import { taskApi } from '@/api/task'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'
import { cn } from '@/utils/cn'
import { formatMoney, formatWeight } from '@/utils/format'

type IconTone = 'primary' | 'success' | 'warning' | 'violet'

/**
 * 首页：顶部日期 → 今日任务 → 2×2 数据卡片 → AI 总结。
 *
 * AI 那一块做成凹陷的，和其余凸起的块区分开——
 * 它是机器给的一段解读，不是你自己录进去的数据，值得在质感上分开。
 */
export function DashboardPage() {
  const showToast = useUiStore((state) => state.showToast)
  const overview = useAsync(() => dashboardApi.overview(), [], { key: 'dashboard:overview' })
  const [busyId, setBusyId] = useState<number | null>(null)

  const data = overview.data
  const now = new Date()
  /** 今天有没有练，决定训练卡显示「已训练」还是「未训练」 */
  const trained = (data?.workout.exerciseCount ?? 0) > 0

  /** 首页直接勾选完成，不用跳进任务页 */
  const complete = async (taskId: number, occurDate: string) => {
    setBusyId(taskId)
    try {
      await taskApi.complete(taskId, occurDate)
      overview.reload()
    } catch {
      showToast('操作失败', 'danger')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      {/*
        报头做成一块凸起的日历牌，而不是一行飘在底面上的字。
        整页只有这一处是纯文字的话，它会显得不属于这套拟物的世界。
        右边配一句随时段变化的问候，让每天打开时页面是「活的」。
      */}
      <header className="mb-6 flex items-center gap-4 px-1">
        {/* 小块用紧凑一档的阴影：大卡片的阴影参数套在 56px 的方块上，
            白色高光会在浅底上糊成一圈光晕，边缘发虚。 */}
        <div className="flex size-14 shrink-0 flex-col items-center justify-center rounded-panel bg-surface shadow-raised-xs">
          <span className="text-[10px] leading-none font-medium text-fg-subtle">
            {now.getMonth() + 1}月
          </span>
          <span className="display mt-1 text-[1.35rem] leading-none font-semibold">
            {now.getDate()}
          </span>
        </div>

        <div className="min-w-0">
          <p className="text-[15px] font-medium">{greetingOf(now)}</p>
          <p className="mt-1 flex items-center gap-2 text-xs text-fg-subtle">
            <span>{weekdayOf(now)}</span>
            <span className="h-3 w-px bg-line-strong" aria-hidden="true" />
            <span className="tabular">{now.getFullYear()}</span>
          </p>
        </div>
      </header>

      {overview.loading && !data ? (
        <SplashScreen />
      ) : !data ? (
        <p className="text-sm text-fg-muted">加载失败，请稍后重试。</p>
      ) : (
        <div className="flex flex-col gap-5">
          {/* 今日任务 */}
          <Card className="p-5">
            <div className="mb-4 flex items-center justify-between">
              <Link to="/tasks" className="flex items-center gap-1.5">
                <span className="text-[15px] font-medium">今日任务</span>
                <ArrowRight size={14} className="text-fg-subtle" />
              </Link>
              <span className="tabular text-sm text-fg-muted">
                {data.tasks.done}/{data.tasks.total}
                <span className="ml-2 font-semibold text-primary">
                  {data.tasks.completionRate}%
                </span>
              </span>
            </div>

            {/* 进度条做成凹槽，填充部分嵌在里面 */}
            <div className="h-2 overflow-hidden rounded-pill bg-surface shadow-inset">
              <div
                className="h-full rounded-pill bg-primary transition-[width] duration-300"
                style={{ width: `${data.tasks.completionRate}%` }}
              />
            </div>

            <div className="mt-4 flex flex-col gap-1">
              {data.tasks.total === 0 ? (
                <p className="py-1 text-sm text-fg-subtle">今天还没有任务，去「任务」页加一条。</p>
              ) : data.tasks.pending.length === 0 ? (
                <p className="py-1 text-sm text-success">今天全部完成了。</p>
              ) : (
                data.tasks.pending.map((item) => (
                  <button
                    key={`${item.taskId}-${item.occurDate}`}
                    type="button"
                    disabled={busyId === item.taskId}
                    onClick={() => complete(item.taskId, item.occurDate)}
                    className="flex w-full items-center gap-3 rounded-tile px-1 py-2.5 text-left active:bg-surface-2"
                  >
                    {/* 未勾选是一个凹下去的小坑，勾选后填成蓝色 */}
                    <span className="flex size-5 shrink-0 items-center justify-center rounded-pill bg-surface shadow-inset">
                      {busyId === item.taskId ? (
                        <Check size={12} className="text-fg-subtle" />
                      ) : null}
                    </span>
                    <span className="min-w-0 flex-1 truncate text-[15px]">{item.title}</span>
                  </button>
                ))
              )}
            </div>
          </Card>

          {/* 2×2 数据卡片。四张卡结构相同，靠图标颜色区分，
              免得变成「四张一模一样的卡」那种模板感。 */}
          <div className="grid grid-cols-2 gap-4">
            <Metric
              to="/weight"
              icon={Scale}
              iconTone="primary"
              label="体重"
              value={data.weight.weightKg === null ? '—' : formatWeight(data.weight.weightKg)}
              unit={data.weight.weightKg === null ? undefined : 'kg'}
              hint={
                data.weight.bmi === null
                  ? '还没有记录'
                  : `BMI ${data.weight.bmi} · ${data.weight.bmiCategory}`
              }
            />
            <Metric
              to="/workout"
              icon={Dumbbell}
              iconTone="success"
              label="今日训练"
              value={trained ? '已训练' : '未训练'}
              valueTone={trained ? 'success' : undefined}
              hint={trained ? `${data.workout.exerciseCount} 个动作` : '去记一次'}
            />
            <Metric
              to="/finance"
              icon={Wallet}
              iconTone="warning"
              label="今日支出"
              prefix="¥"
              value={formatMoney(data.finance.todayExpense)}
              hint={`本周 ¥${formatMoney(data.finance.weekExpense)}`}
            />
            <Metric
              to="/finance"
              icon={Coins}
              iconTone="violet"
              label="本月结余"
              prefix="¥"
              value={formatMoney(data.finance.monthBalance)}
              valueTone={data.finance.monthBalance < 0 ? 'danger' : undefined}
              hint={`收 ¥${formatMoney(data.finance.monthIncome)} · 支 ¥${formatMoney(data.finance.monthExpense)}`}
            />
          </div>

          <AiSummaryCard />
        </div>
      )}
    </>
  )
}

function Metric({
  to,
  icon,
  iconTone,
  label,
  value,
  prefix,
  unit,
  hint,
  valueTone,
}: {
  to: string
  icon: LucideIcon
  iconTone: IconTone
  label: string
  value: string
  /** 数值前的符号（如 ¥），按单位处理而不是拼进数值里 */
  prefix?: string
  unit?: string
  hint: string
  /** 数值本身的颜色，只用于「已训练」「超支」这类状态提示 */
  valueTone?: 'danger' | 'success'
}) {
  const empty = value === '—'
  const valueClass =
    valueTone === 'danger'
      ? 'text-danger'
      : valueTone === 'success'
        ? 'text-success'
        : 'text-fg'
  return (
    <Link to={to} className="block">
      <Card className="flex h-full flex-col p-5 neu-pressable">
        <IconWell icon={icon} tone={iconTone} />

        <p className="mt-4 text-[11px] text-fg-subtle">{label}</p>
        {/*
          四张卡的字号统一：数字 1.6rem，单位（kg / ¥）0.8rem 弱化。
          之前把 ¥ 拼进数值字符串里，导致它在 1.6rem 下渲染成 25.6px，
          而 kg 只有 11px，四张卡看起来参差不齐。
          「已训练」这类状态文字也用 1.6rem——它只有三个字，放得下；
          写成「今日已训练」五个字就会在窄屏上溢出，所以「今日」放在标题里。
        */}
        <p className="mt-1 flex items-baseline gap-1">
          {/* 空值不能用大号加粗渲染：一个大破折号看起来像被涂黑的横杠 */}
          {empty ? (
            <span className="text-lg text-fg-subtle">—</span>
          ) : (
            <>
              {prefix ? (
                <span className="text-[0.8rem] font-medium text-fg-subtle">{prefix}</span>
              ) : null}
              <span
                className={cn('display text-[1.6rem] font-semibold', valueClass)}
              >
                {value}
              </span>
            </>
          )}
          {unit ? <span className="text-[0.8rem] text-fg-subtle">{unit}</span> : null}
        </p>

        {/* 贴底，卡片高度被同行拉齐时不会在下方留一块死空间 */}
        <p className="mt-auto truncate pt-3 text-[11px] leading-relaxed text-fg-subtle">{hint}</p>
      </Card>
    </Link>
  )
}

function weekdayOf(date: Date): string {
  return ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][date.getDay()]
}

/** 随时段变化的问候。深夜单独分一档，那时说「晚上好」不太对味。 */
function greetingOf(date: Date): string {
  const hour = date.getHours()
  if (hour >= 23 || hour < 5) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}
