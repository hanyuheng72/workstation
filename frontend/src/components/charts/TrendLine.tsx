import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  LabelList,
} from 'recharts'
import { ChartTooltip } from './ChartTooltip'

export interface TrendPoint {
  key: string
  label: string
  value: number
}

interface TrendLineProps {
  points: TrendPoint[]
  unit?: string
  /** 数值格式化，用于坐标轴与 tooltip */
  format?: (value: number) => string
}

const LINE_COLOR = 'var(--app-primary)'
const GRID = 'var(--app-viz-grid)'
const AXIS_INK = 'var(--app-fg-subtle)'

/**
 * 单序列折线。只有一条序列，所以不需要图例——标题已经说明了它是什么。
 * 直接标注最新一个点，其余交给坐标轴与 tooltip。
 */
export function TrendLine({ points, unit = '', format }: TrendLineProps) {
  const fmt = format ?? ((value: number) => String(value))
  const lastIndex = points.length - 1
  // 点少的时候给每个点画圆点，点多了就只留线，避免变噪
  const showDots = points.length <= 14

  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={points} margin={{ top: 12, right: 18, bottom: 4, left: 0 }}>
        <CartesianGrid stroke={GRID} strokeWidth={1} vertical={false} />
        <XAxis
          dataKey="label"
          tick={{ fill: AXIS_INK, fontSize: 11 }}
          tickLine={false}
          axisLine={{ stroke: GRID }}
          interval="preserveStartEnd"
          minTickGap={18}
        />
        <YAxis
          tick={{ fill: AXIS_INK, fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          width={44}
          domain={['dataMin - 1', 'dataMax + 1']}
          tickFormatter={(value: number) => fmt(value)}
        />
        <Tooltip
          cursor={{ stroke: AXIS_INK, strokeWidth: 1 }}
          content={({ active, payload, label }) => {
            if (!active || !payload?.length) return null
            return (
              <ChartTooltip
                title={String(label)}
                rows={[{ label: '数值', value: `${fmt(payload[0].value as number)}${unit}`, color: LINE_COLOR }]}
              />
            )
          }}
        />
        <Line
          type="monotone"
          dataKey="value"
          stroke={LINE_COLOR}
          strokeWidth={2}
          dot={showDots ? { r: 4, fill: LINE_COLOR, stroke: 'var(--app-surface)', strokeWidth: 2 } : false}
          activeDot={{ r: 5, stroke: 'var(--app-surface)', strokeWidth: 2 }}
          isAnimationActive={false}
        >
          {/* 只标最后一个点，不是每个点都写数字 */}
          <LabelList
            dataKey="value"
            content={({ index, x, y, value }) =>
              index === lastIndex ? (
                <text
                  x={Number(x)}
                  y={Number(y) - 10}
                  textAnchor="middle"
                  fontSize={11}
                  fontWeight={600}
                  fill="var(--app-fg)"
                >
                  {`${fmt(Number(value))}${unit}`}
                </text>
              ) : null
            }
          />
        </Line>
      </LineChart>
    </ResponsiveContainer>
  )
}
