import { useEffect } from 'react'
import { RouterProvider } from 'react-router-dom'
import { router } from '@/router'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { useAuthStore } from '@/store/authStore'

export function App() {
  const status = useAuthStore((state) => state.status)
  const restore = useAuthStore((state) => state.restore)

  useEffect(() => {
    void restore()
  }, [restore])

  if (status === 'unknown') {
    return <SplashScreen />
  }
  return <RouterProvider router={router} />
}
