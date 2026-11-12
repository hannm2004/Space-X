import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from './client'
import { searchLessons, type LessonSummary } from './learningApi'

const FOCUS = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'

// Chờ người dùng dừng gõ rồi mới gọi API; từ khóa ngắn hơn 3 ký tự thì API cũng không khớp từ nào (xem D29).
const DEBOUNCE_MS = 300
const MIN_QUERY_LENGTH = 3
const SUGGESTION_COUNT = 5
const MAX_QUERY_LENGTH = 200

type Outcome =
  | { kind: 'ok'; items: LessonSummary[]; total: number }
  | { kind: 'empty' }
  | { kind: 'unauthorized' }
  | { kind: 'error' }

type Settled = { query: string; outcome: Outcome }
type Option = { key: string; label: string; to: string }

const STATUS_TEXT = {
  loading: 'Đang tìm...',
  empty: 'Không có bài phù hợp.',
  unauthorized: 'Bạn cần đăng nhập để tìm bài học.',
  error: 'Không tải được gợi ý. Nhấn Enter để tìm.',
} as const

function resultsHref(query: string): string {
  return `/tim-kiem?${new URLSearchParams({ q: query })}`
}

function buildOptions(outcome: Outcome | null, query: string): Option[] {
  if (outcome?.kind !== 'ok') return []
  const options = outcome.items.map((lesson) => ({
    key: String(lesson.id),
    label: lesson.title,
    to: `/bai/${encodeURIComponent(lesson.slug)}`,
  }))
  if (outcome.total > outcome.items.length) {
    options.push({ key: 'all', label: `Xem tất cả ${outcome.total} kết quả`, to: resultsHref(query) })
  }
  return options
}

function MagnifierIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      className="h-5 w-5"
    >
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
  )
}

type CloseHandler = (restoreFocus: boolean) => void

// Ô nhập kèm danh sách gợi ý theo mẫu combobox của ARIA: focus luôn ở ô nhập, dòng đang chọn báo qua
// aria-activedescendant. Chỉ được gắn vào trang khi đang mở nên đóng lại là xóa sạch chữ, gợi ý và lần gọi đang chờ.
function SearchBox({ onClose }: { onClose: CloseHandler }) {
  const navigate = useNavigate()
  const listboxId = useId()
  const containerRef = useRef<HTMLFormElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const [value, setValue] = useState('')
  const [activeIndex, setActiveIndex] = useState(-1)
  const [pendingQuery, setPendingQuery] = useState<string | null>(null)
  const [settled, setSettled] = useState<Settled | null>(null)

  const query = value.trim()

  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  // Bấm ra ngoài thì đóng. Bấm vào gợi ý không làm ô nhập mất focus (onMouseDown của dòng gợi ý).
  useEffect(() => {
    function handleMouseDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) onClose(false)
    }
    document.addEventListener('mousedown', handleMouseDown)
    return () => document.removeEventListener('mousedown', handleMouseDown)
  }, [onClose])

  useEffect(() => {
    if (query.length < MIN_QUERY_LENGTH) return
    // `cancelled` bỏ kết quả của lần gõ cũ đến muộn, để gợi ý luôn thuộc về từ khóa mới nhất.
    let cancelled = false
    const timer = setTimeout(() => {
      setPendingQuery(query)
      searchLessons(query, 0, SUGGESTION_COUNT).then(
        (result) => {
          if (cancelled) return
          const outcome: Outcome =
            result.items.length === 0
              ? { kind: 'empty' }
              : { kind: 'ok', items: result.items, total: result.totalItems }
          setSettled({ query, outcome })
        },
        (error: unknown) => {
          if (cancelled) return
          // Chỉ phân biệt 401; nội dung lỗi khác không đưa ra giao diện.
          const unauthorized = error instanceof ApiError && error.status === 401
          setSettled({ query, outcome: { kind: unauthorized ? 'unauthorized' : 'error' } })
        },
      )
    }, DEBOUNCE_MS)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query])

  // Đang gõ dở thì giữ danh sách gợi ý gần nhất (đỡ nháy); các kết quả khác chỉ hiện khi đúng từ khóa đang gõ.
  let outcome: Outcome | null = null
  let status: string | null = null
  if (query.length >= MIN_QUERY_LENGTH) {
    if (settled !== null && (settled.query === query || settled.outcome.kind === 'ok')) {
      outcome = settled.outcome
    } else if (pendingQuery === query) {
      status = STATUS_TEXT.loading
    }
  }
  if (outcome !== null && outcome.kind !== 'ok') status = STATUS_TEXT[outcome.kind]

  const options = buildOptions(outcome, query)
  const active = activeIndex < options.length ? activeIndex : -1

  function choose(option: Option) {
    navigate(option.to)
    onClose(true)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (query === '') return
    navigate(resultsHref(query))
    onClose(true)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Escape') {
      onClose(true)
    } else if (event.key === 'ArrowDown' && options.length > 0) {
      event.preventDefault()
      setActiveIndex(active + 1 >= options.length ? 0 : active + 1)
    } else if (event.key === 'ArrowUp' && options.length > 0) {
      event.preventDefault()
      setActiveIndex(active <= 0 ? options.length - 1 : active - 1)
    } else if (event.key === 'Enter' && active >= 0) {
      // Đã chọn một gợi ý thì đi thẳng tới đó; chưa chọn thì để biểu mẫu gửi từ khóa sang trang kết quả.
      event.preventDefault()
      choose(options[active])
    }
  }

  return (
    <form
      ref={containerRef}
      role="search"
      onSubmit={handleSubmit}
      onBlur={(event) => {
        // Tab sang phần tử ngoài ô tìm thì đóng. relatedTarget rỗng (bấm chỗ không focus được) để mousedown lo.
        const next = event.relatedTarget
        if (next instanceof Node && !event.currentTarget.contains(next)) onClose(false)
      }}
      className="relative"
    >
      <input
        ref={inputRef}
        type="search"
        role="combobox"
        aria-label="Tìm bài học"
        aria-autocomplete="list"
        aria-expanded={options.length > 0}
        aria-controls={listboxId}
        aria-activedescendant={active >= 0 ? `${listboxId}-${active}` : undefined}
        autoComplete="off"
        value={value}
        maxLength={MAX_QUERY_LENGTH}
        placeholder="Tìm bài học..."
        onChange={(event) => {
          setValue(event.target.value)
          setActiveIndex(-1)
        }}
        onKeyDown={handleKeyDown}
        className={`w-44 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 sm:w-64 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 ${FOCUS}`}
      />
      {(options.length > 0 || status !== null) && (
        <div className="absolute right-0 top-full z-20 mt-2 w-[min(20rem,calc(100vw-2rem))] overflow-hidden rounded-lg border border-slate-200 bg-white text-sm shadow-lg dark:border-slate-700 dark:bg-slate-900">
          {status !== null && (
            <p role="status" className="px-3 py-2 text-slate-600 dark:text-slate-400">
              {status}
            </p>
          )}
          {options.length > 0 && (
            <ul id={listboxId} role="listbox" aria-label="Gợi ý bài học">
              {options.map((option, index) => (
                <li
                  key={option.key}
                  id={`${listboxId}-${index}`}
                  role="option"
                  aria-selected={index === active}
                  // Giữ focus ở ô nhập khi bấm chuột; click mới là lúc chọn.
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => choose(option)}
                  className={`cursor-pointer px-3 py-2 ${
                    index === active
                      ? 'bg-blue-700 text-white'
                      : 'text-slate-900 hover:bg-slate-100 dark:text-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  {option.label}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </form>
  )
}

// Kính lúp ở thanh đầu trang: bấm thì mở ô nhập có gợi ý; Enter sang trang kết quả `/tim-kiem`.
// Cùng kiểu viền với nút đổi giao diện để hai nút đứng cạnh nhau cân đối.
export default function HeaderSearch() {
  const [open, setOpen] = useState(false)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const restoreFocus = useRef(false)

  // Đóng bằng bàn phím (Esc, Enter) thì trả focus về nút kính lúp; nút chỉ có lại sau khi ô nhập biến mất.
  useEffect(() => {
    if (!open && restoreFocus.current) {
      restoreFocus.current = false
      buttonRef.current?.focus()
    }
  }, [open])

  function close(restore: boolean) {
    restoreFocus.current = restore
    setOpen(false)
  }

  if (open) return <SearchBox onClose={close} />

  return (
    <button
      ref={buttonRef}
      type="button"
      aria-label="Tìm bài học"
      title="Tìm bài học"
      onClick={() => setOpen(true)}
      className={`rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 hover:bg-slate-100 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700 ${FOCUS}`}
    >
      <MagnifierIcon />
    </button>
  )
}
