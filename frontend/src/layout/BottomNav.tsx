import { NavLink } from 'react-router-dom'
import { NAV_ITEMS } from './navItems'
import { cn } from '@/utils/cn'

/** 移动端底部导航：首页｜任务｜体重｜健身｜记账 */
export function BottomNav() {
  return (
    <nav className="safe-bottom fixed inset-x-0 bottom-0 z-30 bg-bg px-3 pt-2 pb-2 md:hidden">
      <div className="mx-auto grid max-w-lg grid-cols-5 rounded-pill bg-surface p-1.5 shadow-raised">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                cn(
                  'flex flex-col items-center gap-1 rounded-pill py-2 text-[10px] transition-all',
                  // 选中项按下去，形成一个小凹槽
                  isActive ? 'text-primary shadow-inset' : 'text-fg-subtle',
                )
              }
            >
              {({ isActive }) => (
                <>
                  <Icon size={19} strokeWidth={isActive ? 2.3 : 1.7} />
                  <span>{item.label}</span>
                </>
              )}
            </NavLink>
          )
        })}
      </div>
    </nav>
  )
}
