import { useEffect, useState } from 'react'

export type LoadState<T> =
  | { kind: 'loading' }
  | { kind: 'ok'; data: T }
  | { kind: 'error'; error: unknown }

type Settled<T> = { key: string; attempt: number; state: Exclude<LoadState<T>, { kind: 'loading' }> }

// Tải dữ liệu khi trang mở. `key` là thứ quyết định dữ liệu nào cần tải (ví dụ slug của bài): đổi key thì quay lại
// trạng thái "đang tải" và tải lại, kết quả của lần tải cũ đến muộn sẽ bị bỏ. `reload` dùng cho nút "Thử lại".
export function useLoad<T>(load: () => Promise<T>, key: string) {
  const [settled, setSettled] = useState<Settled<T> | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    load()
      .then((data) => {
        if (!cancelled) setSettled({ key, attempt, state: { kind: 'ok', data } })
      })
      .catch((error: unknown) => {
        if (!cancelled) setSettled({ key, attempt, state: { kind: 'error', error } })
      })
    return () => {
      cancelled = true
    }
    // `load` được tạo lại mỗi lần render nên chỉ theo dõi key và số lần thử.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, attempt])

  const current: LoadState<T> =
    settled && settled.key === key && settled.attempt === attempt ? settled.state : { kind: 'loading' }

  return { state: current, reload: () => setAttempt((n) => n + 1) }
}
