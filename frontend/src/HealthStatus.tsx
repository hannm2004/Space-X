import { useEffect, useState } from 'react'
import { fetchHealth } from './api'

type State = { kind: 'loading' } | { kind: 'ok'; status: string } | { kind: 'error' }

export default function HealthStatus() {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    fetchHealth()
      .then((status) => {
        if (!cancelled) setState({ kind: 'ok', status })
      })
      .catch(() => {
        if (!cancelled) setState({ kind: 'error' })
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  function retry() {
    setState({ kind: 'loading' })
    setAttempt((n) => n + 1)
  }

  return (
    <div role="status" className="mt-2 text-base">
      {state.kind === 'loading' && (
        <p className="text-slate-600 dark:text-slate-400">Đang kiểm tra hệ thống...</p>
      )}
      {state.kind === 'ok' && state.status === 'UP' && (
        <p className="font-medium text-green-700 dark:text-green-400">
          Hệ thống hoạt động bình thường
        </p>
      )}
      {state.kind === 'ok' && state.status !== 'UP' && (
        <p className="font-medium text-amber-700 dark:text-amber-400">
          Trạng thái hệ thống: {state.status}
        </p>
      )}
      {state.kind === 'error' && (
        <div>
          <p className="font-medium text-red-700 dark:text-red-400">
            Không kết nối được tới máy chủ.
          </p>
          <button
            type="button"
            onClick={retry}
            className="mt-2 rounded-lg border border-slate-300 px-3 py-1.5 text-slate-900 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-600 dark:text-slate-100 dark:hover:bg-slate-800"
          >
            Thử lại
          </button>
        </div>
      )}
    </div>
  )
}
