import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '@/api/client'

export interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: string | null
  reload: () => void
  setData: (value: T) => void
}

/**
 * 拉取一次数据并管理载入/错误状态。
 * 用递增的请求序号丢弃过期响应，避免快速切换筛选时旧结果覆盖新结果。
 */
export function useAsync<T>(loader: () => Promise<T>, deps: unknown[]): AsyncState<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)
  const [tick, setTick] = useState(0)

  const loaderRef = useRef(loader)
  loaderRef.current = loader

  useEffect(() => {
    const current = ++requestId.current
    setLoading(true)
    setError(null)

    loaderRef
      .current()
      .then((result) => {
        if (current === requestId.current) {
          setData(result)
        }
      })
      .catch((err: unknown) => {
        if (current === requestId.current) {
          setError(err instanceof ApiError ? err.message : '加载失败')
        }
      })
      .finally(() => {
        if (current === requestId.current) {
          setLoading(false)
        }
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((value) => value + 1), [])

  return { data, loading, error, reload, setData }
}
