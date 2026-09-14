import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Dumbbell, ChevronRight } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { Segmented } from '@/components/ui/Segmented'
import { WorkoutStatsSection } from './WorkoutStatsSection'
import { workoutApi } from '@/api/workout'
import { useAsync } from '@/hooks/useAsync'

type Tab = 'parts' | 'stats'

export function WorkoutPage() {
  const [tab, setTab] = useState<Tab>('parts')
  const parts = useAsync(() => workoutApi.parts(), [], { key: 'workout:parts' })

  return (
    <>
      <PageHeader title="健身" subtitle="按部位记录动作、重量与组次" />

      <Segmented
        value={tab}
        onChange={setTab}
        options={[
          { value: 'parts' as Tab, label: '部位' },
          { value: 'stats' as Tab, label: '统计' },
        ]}
        className="mb-4"
      />

      {tab === 'stats' ? (
        <WorkoutStatsSection />
      ) : parts.loading && !parts.data ? (
        <SplashScreen />
      ) : !parts.data || parts.data.length === 0 ? (
        <EmptyState
          icon={<Dumbbell size={30} />}
          title="部位字典是空的"
          description="请先执行 db/data.sql 播种字典数据。"
        />
      ) : (
        <div className="grid grid-cols-2 gap-2.5 md:grid-cols-3">
          {parts.data.map((part) => (
            <Link key={part.id} to={`/workout/part/${part.id}`} className="block">
              {/*
                没用满高的大卡片：一块只有名字和两行小字，撑成正方形只会留一片空白。
                练过的次数直接并进说明行，不练的时候就不提这回事，避免留个突兀的占位符。
              */}
              <Card className="px-4 py-3.5 transition-colors active:bg-surface-2">
                <div className="flex items-center justify-between">
                  <span className="text-base font-semibold">{part.name}</span>
                  <ChevronRight size={15} className="text-fg-subtle" />
                </div>
                <p className="mt-1 text-[11px] text-fg-subtle">
                  {part.exerciseCount} 个动作
                  {part.sessionCount > 0 ? (
                    <span className="text-primary"> · 练过 {part.sessionCount} 次</span>
                  ) : null}
                </p>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </>
  )
}
