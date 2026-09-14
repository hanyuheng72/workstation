import { NavLink } from 'react-router-dom'
import { Bot, Settings } from 'lucide-react'
import { NAV_ITEMS } from './navItems'
import { cn } from '@/utils/cn'

/** 桌面端把这些内容收进左侧栏，底部导航隐藏 */
const SECONDARY = [
  { to: '/ai', label: 'AI 助手', icon: Bot },
  { to: '/settings', label: '设置', icon: Settings },
]

export function SideNav() {
  return (
    <aside className="sticky top-0 hidden h-dvh w-60 shrink-0 flex-col bg-bg px-4 py-6 md:flex">
      <div className="px-3 pb-5">
        <p className="text-sm font-semibold">个人工作台</p>
        <p className="text-[11px] text-fg-subtle">今天的读数与解读</p>
      </div>

      <nav className="flex flex-col gap-1.5">
        {NAV_ITEMS.map((item) => (
          <SideLink key={item.to} to={item.to} label={item.label} icon={item.icon} end={item.to === '/'} />
        ))}
      </nav>

      <div className="my-4 h-px bg-line" />

      <nav className="flex flex-col gap-1.5">
        {SECONDARY.map((item) => (
          <SideLink key={item.to} to={item.to} label={item.label} icon={item.icon} />
        ))}
      </nav>
    </aside>
  )
}

function SideLink({
  to,
  label,
  icon: Icon,
  end,
}: {
  to: string
  label: string
  icon: typeof NAV_ITEMS[number]['icon']
  end?: boolean
}) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          'flex h-11 items-center gap-3 rounded-tile px-3.5 text-sm transition-all',
          isActive
            ? 'bg-surface font-medium text-primary shadow-inset'
            : 'text-fg-muted shadow-raised-sm neu-pressable',
        )
      }
    >
      <Icon size={17} />
      {label}
    </NavLink>
  )
}
