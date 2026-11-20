import { useEffect, useRef } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { SECONDARY } from '../buttons'
import { ApiError } from '../client'
import LessonProgressControl from '../LessonProgressControl'
import LoadError from '../LoadError'
import { fetchLesson, type Lesson } from '../learningApi'
import MarkdownContent from '../MarkdownContent'
import { readingMinutes } from '../readingTime'
import { useLoad } from '../useLoad'

// Bài viết thường mở đầu bằng "# Tên bài"; tên bài đã là h1 của trang nên bỏ dòng trùng đó để không lặp tiêu đề.
function withoutLeadingTitle(contentMd: string, title: string): string {
  const match = /^\s*#[ \t]+(.+?)[ \t]*(?:\r?\n|$)/.exec(contentMd)
  if (match && match[1].normalize('NFC') === title.trim().normalize('NFC')) {
    return contentMd.slice(match[0].length)
  }
  return contentMd
}

const NAV_LINK =
  'group flex flex-col rounded-xl border border-line bg-surface px-4 py-3 shadow-card transition hover:border-accent'

// Đường dẫn (breadcrumb) là mẫu điều hướng riêng, không phải liên kết trong câu hay nút hành động, nên dùng cùng kiểu chữ
// với liên kết ở chân trang: màu phụ, đậm lên và gạch chân khi trỏ vào.
const CRUMB_LINK = 'hover:text-ink hover:underline'

function Breadcrumb({ lesson }: { lesson: Lesson }) {
  return (
    <nav aria-label="Đường dẫn" className="text-sm text-muted">
      <ol className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <li>
          <Link to="/" className={CRUMB_LINK}>
            Trang chủ
          </Link>
        </li>
        <li aria-hidden="true">›</li>
        <li>
          <Link to={`/khoa-hoc/${encodeURIComponent(lesson.course.slug)}`} className={CRUMB_LINK}>
            {lesson.course.title}
          </Link>
        </li>
        <li aria-hidden="true">›</li>
        <li className="min-w-0">
          <span aria-current="page" className="line-clamp-1 text-ink">
            {lesson.title}
          </span>
        </li>
      </ol>
    </nav>
  )
}

// Bài trước/sau cùng khóa. Bài cuối khóa không có "Bài sau" nên thay bằng đường về trang khóa, để người học không cụt đường.
function LessonPager({ lesson }: { lesson: Lesson }) {
  const { previous, next, course } = lesson
  return (
    <nav aria-label="Chuyển bài" className="mt-8 grid gap-3 sm:grid-cols-2">
      {previous ? (
        <Link to={`/bai/${encodeURIComponent(previous.slug)}`} className={NAV_LINK}>
          <span className="text-xs font-medium text-muted">
            <span aria-hidden="true">← </span>Bài trước
          </span>{' '}
          <span className="mt-1 font-semibold group-hover:text-accent">{previous.title}</span>
        </Link>
      ) : (
        <span aria-hidden="true" className="hidden sm:block" />
      )}
      {next ? (
        <Link to={`/bai/${encodeURIComponent(next.slug)}`} className={`${NAV_LINK} sm:col-start-2 sm:text-right`}>
          <span className="text-xs font-medium text-muted">
            Bài sau<span aria-hidden="true"> →</span>
          </span>{' '}
          <span className="mt-1 font-semibold group-hover:text-accent">{next.title}</span>
        </Link>
      ) : (
        <Link to={`/khoa-hoc/${encodeURIComponent(course.slug)}`} className={`${NAV_LINK} sm:col-start-2 sm:text-right`}>
          <span className="text-xs font-medium text-muted">Hết khóa</span>{' '}
          <span className="mt-1 font-semibold group-hover:text-accent">{`Về khóa ${course.title}`}</span>
        </Link>
      )}
    </nav>
  )
}

function LessonArticle({ lesson }: { lesson: Lesson }) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  const body = withoutLeadingTitle(lesson.contentMd, lesson.title)

  // Chuyển sang bài khác trong cùng trang: đưa về đầu trang và đặt focus vào tên bài để người dùng bàn phím và trình đọc
  // màn hình biết nội dung đã đổi (nếu không, trang vẫn giữ vị trí cuộn của bài trước).
  useEffect(() => {
    window.scrollTo(0, 0)
    headingRef.current?.focus({ preventScroll: true })
  }, [])

  return (
    <article className="mx-auto max-w-2xl">
      <Breadcrumb lesson={lesson} />
      <h1
        ref={headingRef}
        tabIndex={-1}
        className="mt-5 text-4xl font-bold leading-tight tracking-tight focus:outline-none sm:text-5xl"
      >
        {lesson.title}
      </h1>
      <p className="mt-3 text-sm text-muted">{`Khoảng ${readingMinutes(body)} phút đọc`}</p>
      {lesson.summary && (
        <p data-testid="lesson-summary" className="mt-4 text-xl leading-relaxed text-muted">
          {lesson.summary}
        </p>
      )}
      {lesson.coverImageUrl && (
        <img
          data-testid="lesson-cover"
          src={lesson.coverImageUrl}
          alt=""
          className="mt-6 max-h-80 w-full rounded-xl border border-line object-cover"
        />
      )}
      <div className="mt-6">
        <MarkdownContent source={body} />
      </div>
      {/* Bài chỉ tải được khi đã đăng nhập nên ở đây chắc chắn có tài khoản. key để sang bài khác thì gắn lại từ đầu. */}
      <div className="mt-12 border-t border-line pt-6">
        <LessonProgressControl key={lesson.id} lessonId={lesson.id} />
      </div>
      <LessonPager lesson={lesson} />
    </article>
  )
}

export default function LessonPage() {
  const { lessonSlug = '' } = useParams()
  const location = useLocation()
  const { state, reload } = useLoad(() => fetchLesson(lessonSlug), lessonSlug)

  if (state.kind === 'loading') {
    return <p className="text-muted">Đang tải bài học...</p>
  }

  if (state.kind === 'error') {
    const error = state.error
    if (error instanceof ApiError && error.status === 401) {
      return (
        <>
          <p className="text-lg">Bạn cần đăng nhập để đọc bài học này.</p>
          <Link
            to="/dang-nhap"
            state={{ from: location.pathname }}
            className="btn btn-primary mt-4"
          >
            Đăng nhập
          </Link>
        </>
      )
    }
    if (error instanceof ApiError && error.status === 404) {
      return (
        <>
          <h1 className="text-4xl font-bold leading-tight tracking-tight">Không tìm thấy bài học</h1>
          <Link to="/" className={`${SECONDARY} mt-6`}>
            Về trang chủ
          </Link>
        </>
      )
    }
    return <LoadError message="Không tải được bài học." onRetry={reload} />
  }

  return <LessonArticle key={state.data.slug} lesson={state.data} />
}
