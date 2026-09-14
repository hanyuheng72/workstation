import type { ReactNode } from 'react'
import { Card } from '@/components/ui/Card'
import { cn } from '@/utils/cn'

interface ChartFrameProps {
  title: string
  subtitle?: ReactNode
  /** 右上角的切换控件，比如粒度选择 */
  action?: ReactNode
  /** 数据为空时显示的文案；为空时不会渲染图表区域 */
  emptyHint?: string
  isEmpty?: boolean
  /** 图表区域高度。传 'auto' 让内容自己撑开（饼图带图例时用） */
  height?: number | 'auto'
  children: ReactNode
}

/**
 * 图表外框。高度把 X 轴刻度算在内，避免出现「图能放下、轴标签放不下」
 * 导致卡片里冒出一个小滚动条。
 */
export function ChartFrame({
  title,
  subtitle,
  action,
  emptyHint = '还没有数据',
  isEmpty,
  height = 240,
  children,
}: ChartFrameProps) {
  const boxStyle = height === 'auto' ? undefined : { height }
  const emptyStyle = height === 'auto' ? { minHeight: 160 } : { height }

  return (
    <Card className="p-4">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium">{title}</p>
          {subtitle ? <p className="mt-0.5 text-xs text-fg-subtle">{subtitle}</p> : null}
        </div>
        {action ? <div className="shrink-0">{action}</div> : null}
      </div>

      {isEmpty ? (
        // 空状态不用大块灰底——那是一整块没有信息的色块，比留白更显廉价
        <div
          className="flex items-center justify-center text-sm text-fg-subtle"
          style={emptyStyle}
        >
          {emptyHint}
        </div>
      ) : (
        <div style={boxStyle} className={cn('-mx-1')}>
          {children}
        </div>
      )}
    </Card>
  )
}
