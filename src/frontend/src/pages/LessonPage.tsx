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
    return <p className="text-slate-600 dark:text-slate-400">Đang tải bài học...</p>
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
            className="mt-4 inline-block rounded-lg bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
          >
            Đăng nhập
          </Link>
        </>
      )
    }
    if (error instanceof ApiError && error.status === 404) {
      return (
        <>
          <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Không tìm thấy bài học</h1>
          <Link to="/" className="mt-6 inline-block text-blue-700 underline dark:text-blue-400">
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
    <article>
      <Link
        to={from ? `/khoa-hoc/${from.courseSlug}` : '/'}
        className="text-sm text-blue-700 underline dark:text-blue-400"
      >
        <span aria-hidden="true">← </span>
        {from ? `Quay lại ${from.courseTitle}` : 'Trang chủ'}
      </Link>
      <h1 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">{lesson.title}</h1>
      {lesson.summary && (
        <p data-testid="lesson-summary" className="mt-3 text-lg text-slate-600 dark:text-slate-400">
          {lesson.summary}
        </p>
      )}
      <div className="mt-6">
        <MarkdownContent source={withoutLeadingTitle(lesson.contentMd, lesson.title)} />
      </div>
      {/* Bài chỉ tải được khi đã đăng nhập nên ở đây chắc chắn có tài khoản. key để sang bài khác thì gắn lại từ đầu. */}
      <div className="mt-8 border-t border-slate-200 pt-6 dark:border-slate-800">
        <LessonProgressControl key={lesson.id} lessonId={lesson.id} />
      </div>
    </article>
  )
}
