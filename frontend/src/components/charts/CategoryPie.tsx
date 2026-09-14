import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { ChartTooltip } from './ChartTooltip'
import { formatMoney } from '@/utils/format'
import type { CategoryStatVO } from '@/types/domain'

interface CategoryPieProps {
  items: CategoryStatVO[]
}

/** 按固定顺序取色，不生成第 7 色 */
const SERIES = [
  'var(--app-viz-1)',
  'var(--app-viz-2)',
  'var(--app-viz-3)',
  'var(--app-viz-4)',
  'var(--app-viz-5)',
  'var(--app-viz-6)',
]

/**
 * 颜色跟着「分类」这个实体走，不跟着它的金额排名走。
 * 否则某个月餐饮超过游戏，两者的颜色就会互换，读图的人会被误导。
 * 这张表把内置分类锁死在固定色位上。
 */
const SLOT_BY_CODE: Record<string, number> = {
  FOOD: 0,
  TRANSPORT: 1,
  SHOPPING: 2,
  GAME: 3,
  STUDY: 4,
  HOUSING: 5,
  ALLOWANCE: 0,
  SALARY: 1,
}

const OTHER_COLOR = 'var(--app-viz-other)'

/** 内置的「其他」与自定义分类落到哪个色位；返回 null 表示用中性灰 */
function colorOf(code: string | null): string {
  if (!code) {
    return OTHER_COLOR
  }
  const fixed = SLOT_BY_CODE[code]
  if (fixed !== undefined) {
    return SERIES[fixed]
  }
  if (code.startsWith('OTHER_')) {
    return OTHER_COLOR
  }
  // 自定义分类：用 code 做稳定哈希，保证同一分类永远同一颜色
  let hash = 0
  for (let index = 0; index < code.length; index += 1) {
    hash = (hash * 31 + code.charCodeAt(index)) >>> 0
  }
  return SERIES[hash % SERIES.length]
}

/** 饼图最多 6 段，多出来的按金额从小到大合并成「其他」 */
const MAX_SLICES = 6

interface Slice {
  name: string
  value: number
  percent: number
  color: string
}

/**
 * 分类占比环形图。
 * 环形图只适合「一眼看个大概」的占比，所以分段数封顶 6 段，
 * 并且图例里给出每个分类的确切金额与百分比——
 * 精确比较靠数字，不靠弧长。
 */
export function CategoryPie({ items }: CategoryPieProps) {
  const total = items.reduce((sum, item) => sum + Number(item.amount), 0)

  const slices: Slice[] = (() => {
    if (items.length <= MAX_SLICES) {
      return items.map((item) => ({
        name: item.categoryName,
        value: Number(item.amount),
        percent: Number(item.percent),
        color: colorOf(item.code),
      }))
    }

    const head = items.slice(0, MAX_SLICES - 1)
    const tail = items.slice(MAX_SLICES - 1)
    const tailAmount = tail.reduce((sum, item) => sum + Number(item.amount), 0)

    return [
      ...head.map((item) => ({
        name: item.categoryName,
        value: Number(item.amount),
        percent: Number(item.percent),
        color: colorOf(item.code),
      })),
      {
        name: `其他 ${tail.length} 项`,
        value: tailAmount,
        percent: total === 0 ? 0 : Number(((tailAmount / total) * 100).toFixed(1)),
        color: OTHER_COLOR,
      },
    ]
  })()

  return (
    <div className="flex flex-col items-center gap-3">
      <div className="relative h-44 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={slices}
              dataKey="value"
              nameKey="name"
              innerRadius="62%"
              outerRadius="92%"
              paddingAngle={2}
              startAngle={90}
              endAngle={-270}
              isAnimationActive={false}
            >
              {slices.map((slice) => (
                <Cell key={slice.name} fill={slice.color} />
              ))}
            </Pie>
            <Tooltip
              content={({ active, payload }) => {
                if (!active || !payload?.length) return null
                const slice = payload[0].payload as Slice
                return (
                  <ChartTooltip
                    title={slice.name}
                    rows={[
                      { label: '金额', value: `¥${formatMoney(slice.value)}`, color: slice.color },
                      { label: '占比', value: `${slice.percent}%` },
                    ]}
                  />
                )
              }}
            />
          </PieChart>
        </ResponsiveContainer>

        {/* 圆心用来放合计，比在环上标数字干净 */}
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xs text-fg-subtle">合计</span>
          <span className="mt-0.5 text-lg font-semibold">¥{formatMoney(total)}</span>
        </div>
      </div>

      {/* 图例带确切数字：识别不靠颜色，读数不靠 tooltip */}
      <ul className="w-full">
        {slices.map((slice) => (
          <li key={slice.name} className="flex items-center gap-2 py-1 text-xs">
            <span
              className="size-2.5 shrink-0 rounded-full"
              style={{ backgroundColor: slice.color }}
              aria-hidden="true"
            />
            <span className="min-w-0 flex-1 truncate text-fg-muted">{slice.name}</span>
            <span className="shrink-0 tabular text-fg-subtle">{slice.percent}%</span>
            <span className="shrink-0 tabular font-medium text-fg">¥{formatMoney(slice.value)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
