import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { ChartTooltip } from './ChartTooltip'

export interface PeriodPoint {
  key: string
  label: string
  income: number
  expense: number
}

interface BarTrendProps {
  points: PeriodPoint[]
  format?: (value: number) => string
}

const INCOME = 'var(--app-success)'
const EXPENSE = 'var(--app-danger)'
const GRID = 'var(--app-viz-grid)'
const AXIS_INK = 'var(--app-fg-subtle)'

const SERIES = [
  { key: 'income', name: '收入', color: INCOME },
  { key: 'expense', name: '支出', color: EXPENSE },
]

/**
 * 收支双序列柱状图。
 * 绿/红是财务上的通行语义，但这两色在色觉障碍下的分离度落在警告带，
 * 所以必须配第二重编码：常驻图例 + 收入恒在左、支出恒在右的固定位置 + 柱间 2px 间隙。
 */
export function BarTrend({ points, format }: BarTrendProps) {
  const fmt = format ?? ((value: number) => String(value))

  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={points} margin={{ top: 8, right: 8, bottom: 0, left: 0 }} barGap={2}>
        <CartesianGrid stroke={GRID} strokeWidth={1} vertical={false} />
        <XAxis
          dataKey="label"
          tick={{ fill: AXIS_INK, fontSize: 11 }}
          tickLine={false}
          axisLine={{ stroke: GRID }}
          interval="preserveStartEnd"
          minTickGap={14}
        />
        <YAxis
          tick={{ fill: AXIS_INK, fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          width={44}
          tickFormatter={(value: number) => fmt(value)}
        />
        <Tooltip
          cursor={{ fill: 'var(--app-surface-2)' }}
          content={({ active, payload, label }) => {
            if (!active || !payload?.length) return null
            return (
              <ChartTooltip
                title={String(label)}
                rows={SERIES.map((series) => ({
                  label: series.name,
                  color: series.color,
                  value: fmt(Number(payload.find((item) => item.dataKey === series.key)?.value ?? 0)),
                }))}
              />
            )
          }}
        />
        <Legend
          verticalAlign="top"
          align="right"
          height={24}
          content={() => (
            <div className="flex justify-end gap-3 pb-1">
              {SERIES.map((series) => (
                <span key={series.key} className="flex items-center gap-1.5 text-xs text-fg-muted">
                  <span
                    className="size-2 rounded-full"
                    style={{ backgroundColor: series.color }}
                    aria-hidden="true"
                  />
                  {series.name}
                </span>
              ))}
            </div>
          )}
        />
        {SERIES.map((series) => (
          <Bar
            key={series.key}
            dataKey={series.key}
            fill={series.color}
            radius={[4, 4, 0, 0]}
            maxBarSize={18}
            isAnimationActive={false}
          />
        ))}
      </BarChart>
    </ResponsiveContainer>
  )
}
