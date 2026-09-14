import type { HTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '@/utils/cn'

type CardProps = PropsWithChildren<
  HTMLAttributes<HTMLDivElement> & {
    /** inset 用于「凹进去」的块，例如 AI 的解读、图表底槽 */
    variant?: 'raised' | 'inset'
  }
>

/**
 * 拟物纸片。颜色和底色完全相同，立体感只来自一明一暗两道阴影——
 * 所以这里**没有边框**，加任何描边都会立刻破坏效果。
 */
export function Card({ className, children, variant = 'raised', ...rest }: CardProps) {
  return (
    <div
      className={cn(
        'rounded-panel bg-surface',
        variant === 'raised' ? 'shadow-raised' : 'shadow-inset',
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  )
}
