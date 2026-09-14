import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ChartTooltip } from './ChartTooltip'

export interface CountPoint {
  key: string
  label: string
  value: number
}

interface BarSingleProps {
  points: CountPoint[]
  unit?: string
}

const GRID = 'var(--app-viz-grid)'
const AXIS_INK = 'var(--app-fg-subtle)'
const BAR = 'var(--app-primary)'

/** 单序列柱状图。只有一条序列，一个颜色，不需要图例。 */
export function BarSingle({ points, unit = '' }: BarSingleProps) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={points} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
        <CartesianGrid stroke={GRID} strokeWidth={1} vertical={false} />
        <XAxis
          dataKey="label"
          tick={{ fill: AXIS_INK, fontSize: 11 }}
          tickLine={false}
          axisLine={{ stroke: GRID }}
          interval="preserveStartEnd"
          minTickGap={12}
        />
        <YAxis
          tick={{ fill: AXIS_INK, fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          width={36}
          allowDecimals={false}
        />
        <Tooltip
          cursor={{ fill: 'var(--app-surface-2)' }}
          content={({ active, payload, label }) => {
            if (!active || !payload?.length) return null
            return (
              <ChartTooltip
                title={String(label)}
                rows={[
                  { label: '次数', value: `${payload[0].value}${unit}`, color: BAR },
                ]}
              />
            )
          }}
        />
        <Bar dataKey="value" fill={BAR} radius={[4, 4, 0, 0]} maxBarSize={22} isAnimationActive={false} />
      </BarChart>
    </ResponsiveContainer>
  )
}
