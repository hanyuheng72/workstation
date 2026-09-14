import type { BmiVO } from '@/types/domain'
import { formatWeight } from '@/utils/format'

interface BmiScaleProps {
  bmi: BmiVO
}

/** 严重度状态色，与 index.css 里那组保留色一致 */
const BAND_COLOR: Record<string, string> = {
  THIN: 'var(--app-status-warning)',
  NORMAL: 'var(--app-status-good)',
  OVERWEIGHT: 'var(--app-status-serious)',
  OBESE: 'var(--app-status-critical)',
}

/**
 * BMI 直线刻度图。
 *
 * 自绘而不是用图表库：这是一个「把单个值落在固定刻度上」的展示，
 * 需要精确控制分段宽度与标记位置，用图表库反而绕。
 *
 * 每一段都直接写区间名，颜色只是辅助——色觉障碍或黑白打印时同样可读。
 */
export function BmiScale({ bmi }: BmiScaleProps) {
  const { bmi: value, category, scaleMin, scaleMax, bands } = bmi

  if (value === null || scaleMin === null || scaleMax === null || bands.length === 0) {
    return (
      <p className="text-sm text-fg-subtle">记录一次体重后，这里会显示你的 BMI 落在哪个区间。</p>
    )
  }

  const span = scaleMax - scaleMin
  const position = Math.min(100, Math.max(0, ((value - scaleMin) / span) * 100))

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-baseline gap-2">
        <span className="text-3xl font-semibold tracking-tight">{value}</span>
        <span className="text-sm text-fg-muted">BMI</span>
        <span
          className="ml-1 rounded-full px-2 py-0.5 text-xs font-medium text-white"
          style={{ backgroundColor: BAND_COLOR[bandCode(category)] ?? 'var(--app-fg-subtle)' }}
        >
          {category}
        </span>
      </div>

      {/* 刻度尺：分段宽度按 BMI 区间比例分配 */}
      <div className="relative pt-7 pb-6">
        <div className="flex h-2.5 overflow-hidden rounded-full">
          {bands.map((band) => (
            <div
              key={band.code}
              style={{
                width: `${((band.max - band.min) / span) * 100}%`,
                backgroundColor: BAND_COLOR[band.code],
              }}
              title={`${band.label} ${band.min}–${band.max}`}
            />
          ))}
        </div>

        {/* 当前位置标记 */}
        <div
          className="absolute top-0 flex -translate-x-1/2 flex-col items-center"
          style={{ left: `${position}%` }}
        >
          <span className="mb-1 rounded-full bg-fg px-2 py-0.5 text-[11px] font-medium text-bg">
            {value}
          </span>
          <span className="h-3 w-0.5 bg-fg" />
        </div>

        {/* 分段标签 */}
        <div className="absolute inset-x-0 bottom-0 flex">
          {bands.map((band) => (
            <div
              key={band.code}
              className="flex flex-col items-center text-[11px] text-fg-subtle"
              style={{ width: `${((band.max - band.min) / span) * 100}%` }}
            >
              <span className="whitespace-nowrap">{band.label}</span>
            </div>
          ))}
        </div>
      </div>

      {bmi.healthyMinKg !== null && bmi.healthyMaxKg !== null ? (
        <p className="text-xs text-fg-muted">
          按身高 {formatWeight(bmi.heightCm ?? 0)}cm 计算，正常体重大约在{' '}
          <span className="font-medium text-fg">
            {formatWeight(bmi.healthyMinKg)}–{formatWeight(bmi.healthyMaxKg)}kg
          </span>{' '}
          之间
        </p>
      ) : null}
    </div>
  )
}

function bandCode(category: string | null): string {
  switch (category) {
    case '偏瘦':
      return 'THIN'
    case '正常':
      return 'NORMAL'
    case '超重':
      return 'OVERWEIGHT'
    case '肥胖':
      return 'OBESE'
    default:
      return ''
  }
}
