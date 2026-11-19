import { Link, useLocation, useParams } from 'react-router'
import { ApiError } from '../client'
import LessonProgressControl from '../LessonProgressControl'
import LoadError from '../LoadError'
import { fetchLesson } from '../learningApi'
import { readLessonLinkState } from '../lessonLinkState'
import MarkdownContent from '../MarkdownContent'
import { useLoad } from '../useLoad'

// Bài viết thường mở đầu bằng "# Tên bài"; tên bài đã là h1 của trang nên bỏ dòng trùng đó để không lặp tiêu đề.
function withoutLeadingTitle(contentMd: string, title: string): string {
  const match = /^\s*#[ \t]+(.+?)[ \t]*(?:\r?\n|$)/.exec(contentMd)
  if (match && match[1].normalize('NFC') === title.trim().normalize('NFC')) {
    return contentMd.slice(match[0].length)
  }
  return contentMd
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
            className="mt-4 inline-block btn btn-primary"
          >
            Đăng nhập
          </Link>
        </>
      )
    }
    if (error instanceof ApiError && error.status === 404) {
      return (
        <>
          <h1 className="font-serif text-4xl font-bold leading-tight tracking-tight">Không tìm thấy bài học</h1>
          <Link to="/" className="link mt-6 inline-block">
            Về trang chủ
          </Link>
        </>
      )
    }
    return <LoadError message="Không tải được bài học." onRetry={reload} />
  }

  const lesson = state.data
  const from = readLessonLinkState(location.state)

  return (
    <article className="mx-auto max-w-2xl">
      <Link
        to={from ? `/khoa-hoc/${from.courseSlug}` : '/'}
        className="link text-sm"
      >
        <span aria-hidden="true">← </span>
        {from ? `Quay lại ${from.courseTitle}` : 'Trang chủ'}
      </Link>
      <h1 className="mt-5 font-serif text-4xl font-bold leading-tight tracking-tight sm:text-5xl">{lesson.title}</h1>
      {lesson.summary && (
        <p data-testid="lesson-summary" className="mt-4 font-serif text-xl leading-relaxed text-muted">
          {lesson.summary}
        </p>
      )}
      {lesson.coverImageUrl && (
        <img
          data-testid="lesson-cover"
          src={lesson.coverImageUrl}
          alt=""
          className="mt-6 max-h-80 w-full border border-line object-cover"
        />
      )}
      <div className="mt-6">
        <MarkdownContent source={withoutLeadingTitle(lesson.contentMd, lesson.title)} />
      </div>
      {/* Bài chỉ tải được khi đã đăng nhập nên ở đây chắc chắn có tài khoản. key để sang bài khác thì gắn lại từ đầu. */}
      <div className="mt-12 border-t-2 border-ink pt-6">
        <LessonProgressControl key={lesson.id} lessonId={lesson.id} />
      </div>
    </article>
  )
}
