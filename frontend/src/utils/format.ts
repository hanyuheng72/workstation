/** 去掉无意义的小数位：65.30 → 65.3，65.00 → 65。整数不受影响（500 不会变成 5） */
export function trimNumber(value: number | string | null | undefined, digits = 2): string {
  if (value === null || value === undefined) return '—'
  const num = typeof value === 'string' ? Number(value) : value
  if (Number.isNaN(num)) return '—'
  const fixed = num.toFixed(digits)
  return fixed.includes('.') ? fixed.replace(/\.?0+$/, '') : fixed
}

export function formatWeight(value: number | string | null | undefined): string {
  return trimNumber(value, 1)
}

/** 金额固定两位，且加千分位，避免一列数字长短不一 */
export function formatMoney(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return '0.00'
  const num = typeof value === 'string' ? Number(value) : value
  if (Number.isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const WEEKDAYS_SHORT = ['日', '一', '二', '三', '四', '五', '六']

/** '2026-09-13' → '9月13日' */
export function formatMonthDay(iso: string): string {
  const [, month, day] = iso.split('-')
  return `${Number(month)}月${Number(day)}日`
}

/** '2026-09-13' → '周六' */
export function weekdayLabel(iso: string): string {
  const date = new Date(`${iso}T00:00:00`)
  return `周${WEEKDAYS_SHORT[date.getDay()]}`
}

/** 判断是不是今天，用于列表里做「今天」标记 */
export function isToday(iso: string): boolean {
  const now = new Date()
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  return iso === today
}

/** 把表单里的字符串转成数字，空串与非法值都返回 null */
export function parseNumber(input: string): number | null {
  const trimmed = input.trim()
  if (trimmed === '') return null
  const value = Number(trimmed)
  return Number.isFinite(value) ? value : null
}

/**
 * 坐标轴刻度的紧凑写法。轴标签空间有限，四位数以上会被裁掉首位，
 * 所以用 1.2万 / 3.5k 这类写法把宽度压下来。
 */
export function formatCompact(value: number): string {
  const abs = Math.abs(value)
  if (abs >= 10000) {
    const scaled = value / 10000
    return `${Number.isInteger(scaled) ? scaled : scaled.toFixed(1)}万`
  }
  if (abs >= 1000) {
    const scaled = value / 1000
    return `${Number.isInteger(scaled) ? scaled : scaled.toFixed(1)}k`
  }
  return String(Math.round(value))
}
