import { Link, useLocation, useSearchParams } from 'react-router'
import { ApiError } from '../client'
import LoadError from '../LoadError'
import { searchLessons } from '../learningApi'
import SearchForm from '../SearchForm'
import { useLoad } from '../useLoad'

const LINK = 'text-blue-700 underline dark:text-blue-400'

// Chỉ nhận số nguyên không âm; chữ, số âm hay số thập phân coi như trang 0.
function parsePage(raw: string | null): number {
  return raw !== null && /^\d{1,6}$/.test(raw) ? Number(raw) : 0
}

function pageHref(query: string, page: number): string {
  return `/tim-kiem?${new URLSearchParams({ q: query, page: String(page) })}`
}

function SearchResults({ query, page }: { query: string; page: number }) {
  const location = useLocation()
  const { state, reload } = useLoad(() => searchLessons(query, page), `${page}:${query}`)

  if (state.kind === 'loading') {
    return <p className="mt-6 text-slate-600 dark:text-slate-400">Đang tìm...</p>
  }

  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 401) {
      return (
        <>
          <p className="mt-6 text-lg">Bạn cần đăng nhập để tìm bài học.</p>
          <Link
            to="/dang-nhap"
            state={{ from: location.pathname + location.search }}
            className="mt-4 inline-block rounded-lg bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
          >
            Đăng nhập
          </Link>
        </>
      )
    }
    return <LoadError message="Không tìm kiếm được." onRetry={reload} />
  }

  const result = state.data
  if (result.totalItems === 0) {
    return (
      <>
        <p className="mt-6 text-lg">{`Không tìm thấy bài phù hợp với “${query}”.`}</p>
        <p className="mt-2 text-slate-600 dark:text-slate-400">
          Gợi ý: dùng từ khóa có ít nhất 3 ký tự hoặc ít từ hơn.
        </p>
      </>
    )
  }
  // Có kết quả nhưng trang này rỗng (địa chỉ chứa số trang quá lớn).
  if (result.items.length === 0) {
    return (
      <p className="mt-6">
        Không có kết quả ở trang này.{' '}
        <Link to={pageHref(query, 0)} className={LINK}>
          Về trang đầu
        </Link>
      </p>
    )
  }

  const totalPages = Math.ceil(result.totalItems / result.size)
  return (
    <>
      <p className="mt-6 text-slate-600 dark:text-slate-400">{`Tìm thấy ${result.totalItems} bài`}</p>
      <ul className="mt-4 space-y-3">
        {result.items.map((lesson) => (
          <li key={lesson.id}>
            <Link
              to={`/bai/${encodeURIComponent(lesson.slug)}`}
              className="block rounded-xl border border-slate-200 bg-white p-4 hover:border-blue-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-blue-400"
            >
              <span className="block text-lg font-semibold">{lesson.title}</span>
              {lesson.summary && (
                <span className="mt-1 block text-sm text-slate-600 dark:text-slate-400">{lesson.summary}</span>
              )}
            </Link>
          </li>
        ))}
      </ul>
      {totalPages > 1 && (
        <nav aria-label="Phân trang" className="mt-6 flex items-center gap-4">
          {result.page > 0 && (
            <Link to={pageHref(query, result.page - 1)} className={LINK}>
              Trang trước
            </Link>
          )}
          <span>{`Trang ${result.page + 1} / ${totalPages}`}</span>
          {result.page + 1 < totalPages && (
            <Link to={pageHref(query, result.page + 1)} className={LINK}>
              Trang sau
            </Link>
          )}
        </nav>
      )}
    </>
  )
}

export default function SearchPage() {
  const [params] = useSearchParams()
  const query = (params.get('q') ?? '').trim()
  const page = parsePage(params.get('page'))

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Tìm bài học</h1>
      <div className="mt-6">
        {/* key theo từ khóa để ô nhập đồng bộ lại khi từ khóa trong địa chỉ đổi */}
        <SearchForm key={query} initialQuery={query} />
      </div>
      {query === '' ? (
        <p className="mt-6 text-slate-600 dark:text-slate-400">Nhập từ khóa để tìm bài học.</p>
      ) : (
        <SearchResults query={query} page={page} />
      )}
    </>
  )
}
