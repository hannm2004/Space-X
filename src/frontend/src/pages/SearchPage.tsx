import { Link, useLocation, useSearchParams } from 'react-router'
import { SECONDARY } from '../buttons'
import { ApiError } from '../client'
import LessonThumb from '../LessonThumb'
import LoadError from '../LoadError'
import { searchLessons } from '../learningApi'
import SearchForm from '../SearchForm'
import { useLoad } from '../useLoad'

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
    return <p className="mt-6 text-muted">Đang tìm...</p>
  }

  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 401) {
      return (
        <>
          <p className="mt-6 text-lg">Bạn cần đăng nhập để tìm bài học.</p>
          <Link
            to="/dang-nhap"
            state={{ from: location.pathname + location.search }}
            className="btn btn-primary mt-4"
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
        <p className="mt-2 text-muted">
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
        <Link to={pageHref(query, 0)} className={SECONDARY}>
          Về trang đầu
        </Link>
      </p>
    )
  }

  const totalPages = Math.ceil(result.totalItems / result.size)
  return (
    <>
      <p className="mt-6 text-muted">{`Tìm thấy ${result.totalItems} bài`}</p>
      <ul className="mt-4 divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface shadow-card">
        {result.items.map((lesson) => (
          <li key={lesson.id}>
            <Link
              to={`/bai/${encodeURIComponent(lesson.slug)}`}
              className="group flex items-center gap-4 px-4 py-4 hover:bg-sunken sm:px-5"
            >
              <span className="min-w-0 flex-1">
                <span className="block text-lg font-semibold group-hover:text-accent">
                  {lesson.title}
                </span>
                {lesson.summary && <span className="mt-1 block text-sm text-muted">{lesson.summary}</span>}
              </span>
              <LessonThumb url={lesson.coverImageUrl} />
            </Link>
          </li>
        ))}
      </ul>
      {totalPages > 1 && (
        <nav aria-label="Phân trang" className="mt-6 flex items-center gap-4 text-sm">
          {result.page > 0 && (
            <Link to={pageHref(query, result.page - 1)} className={SECONDARY}>
              Trang trước
            </Link>
          )}
          <span>{`Trang ${result.page + 1} / ${totalPages}`}</span>
          {result.page + 1 < totalPages && (
            <Link to={pageHref(query, result.page + 1)} className={SECONDARY}>
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
    <div className="max-w-3xl">
      <h1 className="text-4xl font-bold leading-tight tracking-tight">Tìm bài học</h1>
      <div className="mt-6">
        {/* key theo từ khóa để ô nhập đồng bộ lại khi từ khóa trong địa chỉ đổi */}
        <SearchForm key={query} initialQuery={query} />
      </div>
      {query === '' ? (
        <p className="mt-6 text-muted">Nhập từ khóa để tìm bài học.</p>
      ) : (
        <SearchResults query={query} page={page} />
      )}
    </div>
  )
}
