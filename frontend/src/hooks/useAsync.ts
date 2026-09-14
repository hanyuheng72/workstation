import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '@/api/client'

/**
 * 接口结果的内存缓存。
 *
 * 为什么要缓存：后端在云上，一次请求的往返就是 0.4 秒起步，改不了。
 * 与其让用户每次切页都盯着转圈，不如先把上次的数据显示出来，
 * 同时在后台刷新——切页因此是瞬时的。
 *
 * 只在内存里，不做持久化：应用冷启动仍然是真实的网络请求，
 * 这样不会出现「打开就是过期数据」的错觉。
 */
const cache = new Map<string, unknown>()

export function clearAsyncCache() {
  cache.clear()
}

export interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: string | null
  reload: () => void
  setData: (value: T) => void
}

interface AsyncOptions {
  /**
   * 缓存键。相同的键共享同一份缓存，所以带筛选条件的请求要把条件写进键里
   * （例如 `finance:category:EXPENSE:2026-09`）。
   * 不传就不缓存。
   */
  key?: string
}

/**
 * 拉取一次数据并管理载入/错误状态。
 * 用递增的请求序号丢弃过期响应，避免快速切换筛选时旧结果覆盖新结果。
 */
export function useAsync<T>(
  loader: () => Promise<T>,
  deps: unknown[],
  options: AsyncOptions = {},
): AsyncState<T> {
  const { key } = options

  // 命中缓存时同步给出数据，连一帧的转圈都不出现
  const [data, setData] = useState<T | null>(() =>
    key !== undefined && cache.has(key) ? (cache.get(key) as T) : null,
  )
  const [loading, setLoading] = useState(
    () => !(key !== undefined && cache.has(key)),
  )
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)
  const [tick, setTick] = useState(0)

  const loaderRef = useRef(loader)
  loaderRef.current = loader

  useEffect(() => {
    const current = ++requestId.current
    const cached = key !== undefined && cache.has(key)

    // 有缓存就不显示加载态：界面保持旧数据，后台安静地换新
    if (!cached) {
      setLoading(true)
    }
    setError(null)

    loaderRef
      .current()
      .then((result) => {
        if (current !== requestId.current) return
        setData(result)
        if (key !== undefined) {
          cache.set(key, result)
        }
      })
      .catch((err: unknown) => {
        if (current !== requestId.current) return
        setError(err instanceof ApiError ? err.message : '加载失败')
      })
      .finally(() => {
        if (current === requestId.current) {
          setLoading(false)
        }
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick, key])

  const reload = useCallback(() => setTick((value) => value + 1), [])

  /** 本地改动用它直接更新，省掉一次往返（列表里删一条之后就不用重拉整页） */
  const update = useCallback(
    (value: T) => {
      setData(value)
      if (key !== undefined) {
        cache.set(key, value)
      }
    },
    [key],
  )

  return { data, loading, error, reload, setData: update }
}
