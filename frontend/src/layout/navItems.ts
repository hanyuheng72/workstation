import { CalendarCheck, Dumbbell, Home, Scale, Wallet, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
}

/**
 * 底部导航五项。顺序是刻意排的：首页之后紧跟任务，因为那是最常回来勾一下的东西。
 * 中间原先是一个「＋」快捷入口，实测用不上反而挡住了任务，已改成「任务」。
 */
export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: '首页', icon: Home },
  { to: '/tasks', label: '任务', icon: CalendarCheck },
  { to: '/weight', label: '体重', icon: Scale },
  { to: '/workout', label: '健身', icon: Dumbbell },
  { to: '/finance', label: '记账', icon: Wallet },
]
