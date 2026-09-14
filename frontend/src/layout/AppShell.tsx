import { Outlet } from 'react-router-dom'
import { BottomNav } from './BottomNav'
import { SideNav } from './SideNav'
import { ToastHost } from '@/components/ui/Toast'

/**
 * 应用外壳。同一份内容在移动端走底部导航，桌面端走左侧栏——
 * 是响应式布局，不是两套界面。
 */
export function AppShell() {
  return (
    <div className="flex min-h-dvh bg-bg">
      <SideNav />

      <main className="mx-auto w-full max-w-3xl flex-1 px-4 pt-5 pb-[calc(4.5rem+env(safe-area-inset-bottom))] md:px-8 md:pt-8 md:pb-10">
        <Outlet />
      </main>

      <BottomNav />
      <ToastHost />
    </div>
  )
}
