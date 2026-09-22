import { useCallback, useEffect, useRef, useState } from 'react'
import { focusApi } from '@/api/focus'
import type { FocusSessionVO, FocusStartRequest } from '@/types/domain'

/**
 * 一场专注的客户端状态。
 *
 * 倒计时的权威在服务端：remainingSeconds 是每次调接口时服务端按真实时钟算好的，
 * 前端只在两次调用之间把它画出来。所以锁屏、切到别的 App、关掉页面都不影响结果，
 * 回到前台重新拉一次就对齐了。
 *
 * 本地递减不是「每秒减一」地累加，而是每次都拿「到点时刻 - 当前时钟」现算。
 * 累加式在后台标签页里会越走越慢——浏览器会把定时器降频。
 */
export function useFocusSession() {
  const [session, setSession] = useState<FocusSessionVO | null>(null)
  const [remaining, setRemaining] = useState(0)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  /** 本地时钟上的到点时刻，仅 RUNNING 时有意义 */
  const deadline = useRef(0)

  const refresh = useCallback(async () => {
    const active = await focusApi.active()
    setSession(active)
    return active
  }, [])

  useEffect(() => {
    let alive = true
    refresh()
      .catch(() => {
        // 拉不到就当没有进行中的场次，页面会显示开始按钮
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [refresh])

  // 服务端给了新的一份，重新对齐到点时刻
  useEffect(() => {
    if (!session) {
      deadline.current = 0
      setRemaining(0)
      return
    }
    deadline.current = Date.now() + session.remainingSeconds * 1000
    setRemaining(session.remainingSeconds)
  }, [session])

  useEffect(() => {
    if (session?.status !== 'RUNNING') return
    const tick = () =>
      setRemaining(Math.max(Math.round((deadline.current - Date.now()) / 1000), 0))
    tick()
    const timer = window.setInterval(tick, 250)
    return () => window.clearInterval(timer)
  }, [session])

  const run = useCallback(async (action: () => Promise<FocusSessionVO>) => {
    setBusy(true)
    try {
      const next = await action()
      setSession(next)
      return next
    } finally {
      setBusy(false)
    }
  }, [])

  const start = useCallback(
    (payload: FocusStartRequest) => run(() => focusApi.start(payload)),
    [run],
  )

  const pause = useCallback(() => run(() => focusApi.pause(requireId(session))), [run, session])
  const resume = useCallback(() => run(() => focusApi.resume(requireId(session))), [run, session])
  const complete = useCallback(() => run(() => focusApi.complete(requireId(session))), [run, session])
  const finishEarly = useCallback(
    () => run(() => focusApi.finishEarly(requireId(session))),
    [run, session],
  )
  const abandon = useCallback(() => run(() => focusApi.abandon(requireId(session))), [run, session])

  /** 时间走满就自动结算，不用再点一次「完成」 */
  useEffect(() => {
    if (session?.status !== 'RUNNING' || remaining > 0) return
    complete().catch(() => {
      // 网络抖动先不管：切回前台时会重新对齐，那时会再试一次
    })
  }, [session, remaining, complete])

  /** 切回前台时重新对齐。这一条是「离开期间时间照走」的落点 */
  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState !== 'visible') return
      refresh().catch(() => {})
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => document.removeEventListener('visibilitychange', onVisible)
  }, [refresh])

  return {
    session,
    remaining,
    loading,
    busy,
    refresh,
    start,
    pause,
    resume,
    complete,
    finishEarly,
    abandon,
  }
}

function requireId(session: FocusSessionVO | null): number {
  if (!session) throw new Error('没有进行中的专注')
  return session.id
}
