import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'

interface PageHeaderProps {
  title: string
  subtitle?: ReactNode
  back?: boolean
  action?: ReactNode
}

export function PageHeader({ title, subtitle, back, action }: PageHeaderProps) {
  const navigate = useNavigate()

  return (
    <header className="mb-5 flex items-start gap-3">
      {back ? (
        <button
          type="button"
          onClick={() => navigate(-1)}
          aria-label="返回"
          className="-ml-2 mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-full text-fg-muted active:bg-surface-2"
        >
          <ChevronLeft size={22} />
        </button>
      ) : null}

      <div className="min-w-0 flex-1">
        <h1 className="text-[22px] leading-tight font-semibold tracking-tight">{title}</h1>
        {subtitle ? <div className="mt-1 text-sm text-fg-muted">{subtitle}</div> : null}
      </div>

      {action ? <div className="shrink-0">{action}</div> : null}
    </header>
  )
}
