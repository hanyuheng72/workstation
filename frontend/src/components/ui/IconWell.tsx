import type { LucideIcon } from 'lucide-react'
import { cn } from '@/utils/cn'

const TONES = {
  primary: 'text-primary',
  success: 'text-success',
  warning: 'text-warning',
  violet: 'text-violet',
  muted: 'text-fg-subtle',
} as const

/**
 * 图标的凹槽底座。
 *
 * 图标直接飘在卡片上是飘的——给它一个微凹的圆槽，图标就像嵌在面板里，
 * 这是拟物里最自然的「安装」方式。四种色调只在图标上小面积使用，
 * 用来把首页几张卡区分开，不做大面积底色。
 */
export function IconWell({
  icon: Icon,
  tone = 'muted',
  size = 'md',
}: {
  icon: LucideIcon
  tone?: keyof typeof TONES
  size?: 'sm' | 'md'
}) {
  const box = size === 'sm' ? 'size-8' : 'size-9'
  const glyph = size === 'sm' ? 15 : 17
  return (
    <span
      className={cn(
        'flex shrink-0 items-center justify-center rounded-pill bg-surface shadow-inset',
        box,
        TONES[tone],
      )}
    >
      <Icon size={glyph} strokeWidth={1.8} />
    </span>
  )
}
