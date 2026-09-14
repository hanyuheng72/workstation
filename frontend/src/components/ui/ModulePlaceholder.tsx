import type { ReactNode } from 'react'
import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState } from '@/components/ui/EmptyState'

interface ModulePlaceholderProps {
  title: string
  description: string
  phase: string
  icon: ReactNode
}

/**
 * 尚未实现的模块占位。刻意写明「还没做」而不是放假数据，
 * 避免用着用着分不清哪些数字是真的。
 */
export function ModulePlaceholder({ title, description, phase, icon }: ModulePlaceholderProps) {
  return (
    <>
      <PageHeader title={title} subtitle={description} />
      <EmptyState
        icon={icon}
        title="这个模块还没开始做"
        description={`计划在 ${phase} 阶段实现。当前阶段先把骨架、数据库和登录打通。`}
      />
    </>
  )
}
