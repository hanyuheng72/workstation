import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { KeyRound } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { useAuthStore } from '@/store/authStore'
import { ApiError } from '@/api/client'

export function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((state) => state.login)

  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!password || submitting) return

    setSubmitting(true)
    setError(null)
    try {
      await login(password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '登录失败，请重试')
      setPassword('')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="safe-top safe-bottom flex min-h-dvh flex-col items-center justify-center bg-bg px-6">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <div className="mb-4 flex size-14 items-center justify-center rounded-panel bg-primary-soft text-primary">
            <KeyRound size={26} />
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">个人工作台</h1>
          <p className="mt-1.5 text-sm text-fg-muted">输入口令进入你的今日控制中心</p>
        </div>

        <form onSubmit={onSubmit} className="flex flex-col gap-3">
          <input
            type="password"
            inputMode="numeric"
            autoComplete="current-password"
            autoFocus
            value={password}
            onChange={(event) => {
              setPassword(event.target.value)
              if (error) setError(null)
            }}
            placeholder="口令"
            aria-label="口令"
            aria-invalid={error !== null}
            className="tabular h-13 w-full rounded-tile bg-surface px-4 text-center text-lg tracking-[0.4em] shadow-inset outline-none placeholder:tracking-normal placeholder:text-fg-subtle focus:shadow-inset-deep focus:outline-2 focus:outline-offset-0 focus:outline-primary"
          />

          {error ? (
            <p role="alert" className="text-center text-sm text-danger">
              {error}
            </p>
          ) : null}

          <Button type="submit" size="lg" block loading={submitting} disabled={!password}>
            进入
          </Button>
        </form>

        <p className="mt-6 text-center text-xs text-fg-subtle">
          第一版仅供个人使用，不提供注册与找回
        </p>
      </div>
    </div>
  )
}
