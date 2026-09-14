import { useEffect, useState } from 'react'
import { LogOut, Ruler, ShieldCheck, User } from 'lucide-react'
import { PageHeader } from '@/components/ui/PageHeader'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { IconWell } from '@/components/ui/IconWell'
import { Field, Input } from '@/components/ui/Field'
import { SplashScreen } from '@/components/ui/SplashScreen'
import { profileApi } from '@/api/profile'
import { ApiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useAuthStore } from '@/store/authStore'
import { useUiStore } from '@/store/uiStore'
import { parseNumber, trimNumber } from '@/utils/format'

export function SettingsPage() {
  const logout = useAuthStore((state) => state.logout)
  const authRequired = useAuthStore((state) => state.authRequired)
  const showToast = useUiStore((state) => state.showToast)

  const profile = useAsync(() => profileApi.get(), [], { key: 'profile' })

  const [nickname, setNickname] = useState('')
  const [height, setHeight] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!profile.data) return
    setNickname(profile.data.nickname)
    setHeight(trimNumber(profile.data.heightCm, 1))
  }, [profile.data])

  const dirty =
    profile.data !== null &&
    (nickname !== profile.data.nickname || height !== trimNumber(profile.data.heightCm, 1))

  const save = async () => {
    const heightValue = parseNumber(height)
    if (heightValue === null) {
      setError('请填写身高')
      return
    }

    setSaving(true)
    setError(null)
    try {
      await profileApi.update({ nickname: nickname.trim() || null, heightCm: heightValue })
      showToast('已保存', 'success')
      profile.reload()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <PageHeader title="设置" subtitle="个人资料与登录状态" />

      {profile.loading && !profile.data ? (
        <SplashScreen />
      ) : (
        <>
          <Card className="p-5">
            <div className="mb-5 flex items-center gap-3">
              <IconWell icon={User} tone="primary" size="sm" />
              <p className="text-[15px] font-medium">个人资料</p>
            </div>

            <div className="flex flex-col gap-4">
              <Field label="昵称">
                <Input
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                  placeholder="我"
                />
              </Field>

              <Field label="身高" hint="BMI 与健康体重区间都按这个身高计算">
                <div className="relative">
                  <Input
                    type="number"
                    inputMode="decimal"
                    step="0.5"
                    value={height}
                    onChange={(event) => setHeight(event.target.value)}
                    className="pr-12"
                  />
                  <span className="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-sm text-fg-subtle">
                    cm
                  </span>
                </div>
              </Field>

              {error ? <p className="text-sm text-danger">{error}</p> : null}

              <Button block loading={saving} disabled={!dirty} onClick={save}>
                保存资料
              </Button>
            </div>
          </Card>

          {/* 说明性文字用凹陷块，和首页的 AI 解读保持同一种「这不是你录的数据」的质感 */}
          <Card variant="inset" className="mt-3 flex items-start gap-3 p-4">
            <Ruler size={15} className="mt-1 shrink-0 text-fg-subtle" />
            <p className="text-xs leading-relaxed text-fg-muted">
              本项目不设「目标体重」——当前体重始终显示你最新录入的那一条，
              不做目标差值。想评估体重是否合适，看体重页的 BMI 刻度尺。
            </p>
          </Card>

          {authRequired ? (
            <Card className="mt-3 flex items-center gap-3 p-4">
              <IconWell icon={ShieldCheck} tone="success" size="sm" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">登录状态</p>
                <p className="mt-0.5 text-[11px] text-fg-subtle">令牌保存在本机，30 天内免登录</p>
              </div>
              <Button variant="secondary" size="sm" onClick={logout}>
                <LogOut size={15} />
                退出
              </Button>
            </Card>
          ) : (
            // 登录已关闭时不该出现「退出」——点了会跳去登录页，可那个页面根本用不上
            <Card variant="inset" className="mt-3 flex items-start gap-3 p-4">
              <ShieldCheck size={15} className="mt-1 shrink-0 text-warning" />
              <div>
                <p className="text-sm font-medium text-warning">登录已关闭</p>
                <p className="mt-1 text-xs leading-relaxed text-fg-muted">
                  当前部署任何人都能直接打开，不需要口令。如果是部署在公网，建议在{' '}
                  <code className="rounded-tile bg-surface px-1.5 py-0.5 text-[11px] shadow-inset">
                    backend/.env
                  </code>{' '}
                  里把 <code className="rounded-tile bg-surface px-1.5 py-0.5 text-[11px] shadow-inset">APP_AUTH_ENABLED</code>{' '}
                  改回 true。
                </p>
              </div>
            </Card>
          )}

          <Card className="mt-3 p-5">
            <p className="text-sm font-medium">数据导出</p>
            <p className="mt-1.5 text-xs leading-relaxed text-fg-muted">
              JSON 全量导出与 CSV 分模块导出在 P5 阶段提供，用于长期数据的备份与迁移。
            </p>
          </Card>
        </>
      )}
    </>
  )
}
