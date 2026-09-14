const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

/** 2026年9月13日 星期日 */
export function formatFullDate(date: Date): string {
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${WEEKDAYS[date.getDay()]}`
}

/** 本地时区的 yyyy-MM-dd，不能用 toISOString（那是 UTC，跨时区会差一天） */
export function toISODate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function todayISO(): string {
  return toISODate(new Date())
}

/** 当前所在周的周一 */
export function startOfWeek(date: Date): Date {
  const result = new Date(date)
  const weekday = (result.getDay() + 6) % 7
  result.setDate(result.getDate() - weekday)
  result.setHours(0, 0, 0, 0)
  return result
}

export function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}
