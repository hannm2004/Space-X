import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'

const FOCUS = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'

// Ô tìm bài học dùng ở trang chủ và trang kết quả. Từ khóa nằm trong địa chỉ (?q=) để chia sẻ và tải lại được.
// 200 ký tự là giới hạn của API; từ khóa trống thì không làm gì (API sẽ trả 400).
export default function SearchForm({ initialQuery = '' }: { initialQuery?: string }) {
  const navigate = useNavigate()
  const [value, setValue] = useState(initialQuery)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const query = value.trim()
    if (query === '') return
    navigate(`/tim-kiem?${new URLSearchParams({ q: query })}`)
  }

  return (
    <form role="search" onSubmit={handleSubmit} className="flex gap-2">
      <label htmlFor="search-query" className="sr-only">
        Từ khóa
      </label>
      <input
        id="search-query"
        type="search"
        value={value}
        maxLength={200}
        placeholder="Tìm bài học, ví dụ: thuật toán"
        onChange={(event) => setValue(event.target.value)}
        className={`min-w-0 flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 ${FOCUS}`}
      />
      <button
        type="submit"
        className={`rounded-lg bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 ${FOCUS}`}
      >
        Tìm
      </button>
    </form>
  )
}
