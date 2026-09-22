import { Suspense, lazy } from 'react'
import { createBrowserRouter, Navigate, Outlet, useLocation } from 'react-router-dom'
import { AppShell } from '@/layout/AppShell'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { LoginPage } from '@/pages/Login/LoginPage'
import { useAuthStore } from '@/store/authStore'

/**
 * 页面按路由懒加载。图表库（recharts）体积可观，
 * 拆出去之后登录页与首页的首次加载只带上真正需要的代码。
 */
const DashboardPage = lazy(() =>
  import('@/pages/Dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })),
)
const WeightPage = lazy(() =>
  import('@/pages/Weight/WeightPage').then((m) => ({ default: m.WeightPage })),
)
const WorkoutPage = lazy(() =>
  import('@/pages/Workout/WorkoutPage').then((m) => ({ default: m.WorkoutPage })),
)
const PartDetailPage = lazy(() =>
  import('@/pages/Workout/PartDetailPage').then((m) => ({ default: m.PartDetailPage })),
)
const FinancePage = lazy(() =>
  import('@/pages/Finance/FinancePage').then((m) => ({ default: m.FinancePage })),
)
const TaskPage = lazy(() => import('@/pages/Task/TaskPage').then((m) => ({ default: m.TaskPage })))
const TaskCalendarPage = lazy(() =>
  import('@/pages/Task/TaskCalendarPage').then((m) => ({ default: m.TaskCalendarPage })),
)
const SettingsPage = lazy(() =>
  import('@/pages/Settings/SettingsPage').then((m) => ({ default: m.SettingsPage })),
)
const AiChatPage = lazy(() =>
  import('@/pages/Ai/AiChatPage').then((m) => ({ default: m.AiChatPage })),
)
const FocusRoomPage = lazy(() =>
  import('@/pages/Focus/FocusRoomPage').then((m) => ({ default: m.FocusRoomPage })),
)

/** 登录态还没确认时先显示载入中，避免闪一下登录页又跳回来 */
function RequireAuth() {
  const status = useAuthStore((state) => state.status)
  const location = useLocation()

  if (status === 'unknown') return <SplashScreen />
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

function LoginRoute() {
  const status = useAuthStore((state) => state.status)

  if (status === 'unknown') return <SplashScreen />
  if (status === 'authenticated') return <Navigate to="/" replace />
  return <LoginPage />
}

/** 懒加载页面切换时显示，尺寸与 SplashScreen 一致，避免布局跳动 */
function LazyFallback() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="size-6 animate-spin rounded-full border-2 border-line border-t-primary" />
    </div>
  )
}

const withSuspense = (node: React.ReactNode) => <Suspense fallback={<LazyFallback />}>{node}</Suspense>

export const router = createBrowserRouter([
  { path: '/login', element: <LoginRoute /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/', element: withSuspense(<DashboardPage />) },
          { path: '/weight', element: withSuspense(<WeightPage />) },
          { path: '/workout', element: withSuspense(<WorkoutPage />) },
          { path: '/workout/part/:partId', element: withSuspense(<PartDetailPage />) },
          { path: '/finance', element: withSuspense(<FinancePage />) },
          { path: '/tasks', element: withSuspense(<TaskPage />) },
          { path: '/tasks/calendar', element: withSuspense(<TaskCalendarPage />) },
          // 自习室不占底部导航位，从首页右上角进
          { path: '/focus', element: withSuspense(<FocusRoomPage />) },
          { path: '/ai', element: withSuspense(<AiChatPage />) },
          { path: '/settings', element: withSuspense(<SettingsPage />) },
          { path: '*', element: <Navigate to="/" replace /> },
        ],
      },
    ],
  },
])
