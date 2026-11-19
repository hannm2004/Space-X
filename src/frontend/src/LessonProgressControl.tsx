import { useEffect, useState } from 'react'
import { ApiError } from './client'
import { recordProgress } from './learningApi'

const BUTTON =
  'btn btn-primary'

type State =
  | { kind: 'loading' } // đang ghi "đã mở bài"; chưa biết bài đã hoàn thành hay chưa
  | { kind: 'open' }
  | { kind: 'saving' }
  | { kind: 'done' }
  | { kind: 'open-error'; unauthorized: boolean }
  | { kind: 'save-error'; unauthorized: boolean }

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}

// Câu cố định: nội dung lỗi của máy chủ không đưa ra giao diện. Lỗi tiến độ không chặn việc đọc bài.
function failureText(unauthorized: boolean, fallback: string): string {
  return unauthorized ? 'Bạn cần đăng nhập để lưu tiến độ.' : fallback
}

// Chỉ được gắn khi bài đã tải được, mà bài chỉ tải được khi đã đăng nhập (D19). Trang bài đặt key={lessonId} để
// sang bài khác là gắn lại từ đầu. Trạng thái luôn lấy từ máy chủ: mở lại bài đã hoàn thành vẫn nhận COMPLETED.
export default function LessonProgressControl({ lessonId }: { lessonId: number }) {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    recordProgress(lessonId, 'IN_PROGRESS').then(
      (result) => {
        if (!cancelled) setState(result.status === 'COMPLETED' ? { kind: 'done' } : { kind: 'open' })
      },
      (error: unknown) => {
        if (!cancelled) setState({ kind: 'open-error', unauthorized: isUnauthorized(error) })
      },
    )
    return () => {
      cancelled = true
    }
  }, [lessonId, attempt])

  function retry() {
    setState({ kind: 'loading' })
    setAttempt((n) => n + 1)
  }

  function complete() {
    setState({ kind: 'saving' })
    recordProgress(lessonId, 'COMPLETED').then(
      () => setState({ kind: 'done' }),
      (error: unknown) => setState({ kind: 'save-error', unauthorized: isUnauthorized(error) }),
    )
  }

  if (state.kind === 'done') {
    return (
      <p role="status" className="font-medium text-success">
        <span aria-hidden="true">✓ </span>Bạn đã hoàn thành bài này.
      </p>
    )
  }

  if (state.kind === 'open-error') {
    return (
      <div role="alert" className="text-sm text-danger">
        <p>{failureText(state.unauthorized, 'Không lưu được tiến độ.')}</p>
        {!state.unauthorized && (
          <button type="button" onClick={retry} className="mt-2 underline">
            Thử lại
          </button>
        )}
      </div>
    )
  }

  const busy = state.kind === 'loading' || state.kind === 'saving'
  return (
    <div>
      {state.kind === 'save-error' && (
        <p role="alert" className="mb-2 text-sm text-danger">
          {failureText(state.unauthorized, 'Không ghi nhận được, hãy thử lại.')}
        </p>
      )}
      <button type="button" disabled={busy} aria-busy={busy} onClick={complete} className={BUTTON}>
        Hoàn thành bài
      </button>
    </div>
  )
}
