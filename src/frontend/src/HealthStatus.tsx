import { useEffect, useState } from 'react'
import { fetchHealth } from './api'

type State = { kind: 'loading' } | { kind: 'ok'; status: string } | { kind: 'error' }

// Chấm màu + một dòng chữ: gọn để đặt ở chân trang. Chữ luôn nói rõ trạng thái, màu chỉ là phụ (không dựa vào màu).
function Dot({ tone }: { tone: 'success' | 'warn' | 'danger' }) {
  const color = tone === 'success' ? 'bg-success' : tone === 'warn' ? 'bg-warn' : 'bg-danger'
  return <span aria-hidden="true" className={`inline-block h-2 w-2 rounded-full ${color}`} />
}

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
    <div role="status" className="text-sm">
      {state.kind === 'loading' && <p className="text-muted">Đang kiểm tra hệ thống...</p>}
      {state.kind === 'ok' && state.status === 'UP' && (
        <p className="flex items-center gap-2">
          <Dot tone="success" />
          <span>Hệ thống hoạt động bình thường</span>
        </p>
      )}
      {state.kind === 'ok' && state.status !== 'UP' && (
        <p className="flex items-center gap-2">
          <Dot tone="warn" />
          <span>Trạng thái hệ thống: {state.status}</span>
        </p>
      )}
      {state.kind === 'error' && (
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <span className="flex items-center gap-2">
            <Dot tone="danger" />
            <span>Không kết nối được tới máy chủ.</span>
          </span>
          <button type="button" onClick={retry} className="link">
            Thử lại
          </button>
        </p>
      )}
    </div>
  )
}
