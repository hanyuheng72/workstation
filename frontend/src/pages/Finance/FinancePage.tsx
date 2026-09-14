import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Plus, Trash2, Wallet } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { Segmented } from '@/components/ui/Segmented'
import { FinanceForm } from '@/components/domain/FinanceForm'
import { FinanceStatsSection } from './FinanceStatsSection'
import { financeApi } from '@/api/finance'
import { financeStatsApi } from '@/api/financeStats'
import { useAsync } from '@/hooks/useAsync'
import { useUiStore } from '@/store/uiStore'
import { cn } from '@/utils/cn'
import { formatMoney, formatMonthDay, weekdayLabel, isToday } from '@/utils/format'
import type { TransactionVO } from '@/types/domain'

type Tab = 'list' | 'stats'

export function FinancePage() {
  const [params, setParams] = useSearchParams()
  const showToast = useUiStore((state) => state.showToast)

  const [tab, setTab] = useState<Tab>('list')
  const [formOpen, setFormOpen] = useState(params.get('quick') === '1')
  const overview = useAsync(() => financeStatsApi.overview(), [])
  const transactions = useAsync(() => financeApi.transactions({ page: 1, size: 50 }), [])

  const closeForm = () => {
    setFormOpen(false)
    if (params.has('quick')) {
      params.delete('quick')
      setParams(params, { replace: true })
    }
  }

  const reload = () => {
    overview.reload()
    transactions.reload()
  }

  const remove = async (record: TransactionVO) => {
    if (!window.confirm(`删除这笔 ${record.categoryName ?? ''} ¥${formatMoney(record.amount)}？`)) return
    try {
      await financeApi.remove(record.id)
      showToast('已删除', 'success')
      reload()
    } catch {
      showToast('删除失败', 'danger')
    }
  }

  const stats = overview.data
  const items = transactions.data?.list ?? []
  const balance = stats?.monthBalance ?? 0

  return (
    <>
      <PageHeader
        title="记账"
        subtitle="收入、支出与本月结余"
        action={
          <Button size="sm" onClick={() => setFormOpen(true)}>
            <Plus size={16} />
            记一笔
          </Button>
        }
      />

      {overview.loading && !stats ? (
        <SplashScreen />
      ) : (
        <div className="flex flex-col gap-4">
          {/* 本月结余是主角，直接排在台面上 */}
          <section className="px-1">
            <p className="text-[11px] font-medium tracking-[0.14em] text-fg-subtle">本月结余</p>
            <p className="mt-2 flex items-baseline gap-1.5">
              <span className="text-lg text-fg-subtle">¥</span>
              <span
                className={cn(
                  'display text-[2.75rem] font-semibold',
                  balance < 0 ? 'text-danger' : 'text-fg',
                )}
              >
                {formatMoney(balance)}
              </span>
            </p>
          </section>

          {/* 四项读数压成一条带，而不是四张卡 */}
          <Card className="overflow-hidden">
            <div className="grid grid-cols-2">
              <Cell label="本月收入" value={`¥${formatMoney(stats?.monthIncome ?? 0)}`} tone="success" />
              <Cell
                label="本月支出"
                value={`¥${formatMoney(stats?.monthExpense ?? 0)}`}
                tone="danger"
                className="border-l border-line"
              />
              <Cell
                label="今日支出"
                value={`¥${formatMoney(stats?.todayExpense ?? 0)}`}
                className="border-t border-line"
              />
              <Cell
                label="本周支出"
                value={`¥${formatMoney(stats?.weekExpense ?? 0)}`}
                className="border-t border-l border-line"
              />
            </div>
          </Card>

          <Segmented
            value={tab}
            onChange={setTab}
            options={[
              { value: 'list' as Tab, label: '记录' },
              { value: 'stats' as Tab, label: '统计' },
            ]}
          />

          {tab === 'stats' ? (
            <FinanceStatsSection />
          ) : transactions.loading && !transactions.data ? (
            <SplashScreen />
          ) : items.length === 0 ? (
            <EmptyState
              icon={<Wallet size={28} />}
              title="还没有记账记录"
              description="点右上角「记一笔」，第一笔之后这里就有内容了。"
            />
          ) : (
            <Card className="overflow-hidden">
              {items.map((record, index) => (
                <div
                  key={record.id}
                  className={cn(
                    'flex items-center gap-3 px-4 py-3',
                    index > 0 && 'border-t border-line',
                  )}
                >
                  <div className="min-w-0 flex-1">
                    <p className="flex items-baseline gap-2 text-sm">
                      <span className="truncate">{record.categoryName ?? '未分类'}</span>
                      {record.note ? (
                        <span className="truncate text-[11px] text-fg-subtle">{record.note}</span>
                      ) : null}
                    </p>
                    <p className="mt-0.5 text-[11px] text-fg-subtle">
                      {isToday(record.occurDate) ? '今天' : formatMonthDay(record.occurDate)}
                      <span className="ml-1.5">{weekdayLabel(record.occurDate)}</span>
                      {record.occurTime ? (
                        <span className="ml-1.5">{record.occurTime.slice(0, 5)}</span>
                      ) : null}
                    </p>
                  </div>
                  <span
                    className={cn(
                      'tabular shrink-0 text-[15px] font-semibold',
                      record.type === 'INCOME' ? 'text-success' : 'text-fg',
                    )}
                  >
                    {record.type === 'INCOME' ? '+' : '−'}
                    {formatMoney(record.amount)}
                  </span>
                  {/* 删除做成独立按钮：之前整行都能点删，误触代价太大 */}
                  <button
                    type="button"
                    aria-label="删除这笔"
                    onClick={() => remove(record)}
                    className="-mr-1 flex size-8 shrink-0 items-center justify-center rounded-tile text-fg-subtle active:bg-surface-2"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              ))}
            </Card>
          )}
        </div>
      )}

      <FinanceForm open={formOpen} onClose={closeForm} onSaved={reload} />
    </>
  )
}

function Cell({
  label,
  value,
  tone = 'default',
  className,
}: {
  label: string
  value: string
  tone?: 'default' | 'success' | 'danger'
  className?: string
}) {
  const toneClass =
    tone === 'success' ? 'text-success' : tone === 'danger' ? 'text-danger' : 'text-fg'
  return (
    <div className={cn('px-4 py-3', className)}>
      <p className="text-[11px] text-fg-subtle">{label}</p>
      <p className={cn('tabular mt-1 text-base font-semibold', toneClass)}>{value}</p>
    </div>
  )
}
