import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-6 py-12 text-center">
      {icon ? <div className="mb-1 text-fg-subtle">{icon}</div> : null}
      <p className="text-[15px] font-medium text-fg">{title}</p>
      {description ? <p className="max-w-xs text-sm text-fg-muted">{description}</p> : null}
      {action ? <div className="mt-3">{action}</div> : null}
    </div>
  )
}
