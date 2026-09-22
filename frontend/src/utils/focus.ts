import type { FailReason, FocusStatus } from '@/types/domain'

/**
 * 提前结束的门槛，与服务端 FocusService.MIN_EARLY_SECONDS 对齐。
 * 前端只是提前把按钮置灰并说明原因，真正拦得住的是服务端。
 */
export const MIN_EARLY_SECONDS = 60

/** 服务端记的是秒，展示按分钟四舍五入。换算只走这里，免得各处取整方式不一致 */
export function minutesOf(seconds: number): number {
  return Math.round(seconds / 60)
}

/** 倒计时。超过一小时才带上小时位，否则「00:45:00」里前面那两个 0 是噪音 */
export function formatCountdown(seconds: number): string {
  const total = Math.max(Math.floor(seconds), 0)
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const rest = total % 60
  const pad = (value: number) => String(value).padStart(2, '0')
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`
}

/** 时长文案：90 → 1 小时 30 分。用于历史与统计，不用于倒计时 */
export function formatMinutes(minutes: number): string {
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}

/** '2026-09-22' → '9/22'。趋势图的横轴放不下「9月22日」 */
export function shortDateLabel(iso: string): string {
  const [, month, day] = iso.split('-')
  return `${Number(month)}/${Number(day)}`
}

/** '2026-09-22T14:30:00' → '14:30' */
export function formatClock(iso: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

/** 失败原因的可读文案。成功时为 null，调用方自己决定显示什么 */
export function failReasonLabel(reason: FailReason | null): string {
  if (reason === 'PAUSE_EXHAUSTED') return '暂停机会用完了'
  if (reason === 'ABANDONED') return '主动放弃'
  return '未完成'
}

export function focusStatusLabel(status: FocusStatus): string {
  switch (status) {
    case 'SUCCESS':
      return '完成'
    case 'FAILED':
      return '失败'
    case 'PAUSED':
      return '已暂停'
    default:
      return '进行中'
  }
}

export function focusStatusTone(status: FocusStatus): 'success' | 'danger' | 'primary' | 'muted' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'RUNNING':
    case 'PAUSED':
      return 'primary'
    default:
      return 'muted'
  }
}
