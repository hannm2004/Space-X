import { Link, useParams } from 'react-router'
import { ApiError } from '../client'
import CourseProgressPanel from '../CourseProgressPanel'
import LessonThumb from '../LessonThumb'
import LoadError from '../LoadError'
import { fetchCourses, fetchLessons } from '../learningApi'
import type { LessonLinkState } from '../lessonLinkState'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'
import { useMyProgress } from '../useMyProgress'

export default function CoursePage() {
  const { courseSlug = '' } = useParams()
  const { state, reload } = useLoad(async () => {
    const [courses, lessons] = await Promise.all([fetchCourses(), fetchLessons(courseSlug)])
    return { course: courses.find((c) => c.slug === courseSlug) ?? null, lessons }
  }, courseSlug)
  // Hook phải gọi trước các return sớm bên dưới. Tiến độ tải riêng: lỗi tiến độ không làm mất danh sách bài.
  const progress = useMyProgress()
  // Ảnh chỉ xem được khi đã đăng nhập (D19), nên khách không thấy ảnh bìa thay vì thấy ảnh hỏng.
  const { state: auth } = useAuth()
  const showCovers = auth.status === 'authenticated'

  if (state.kind === 'loading') {
    return <p className="text-muted">Đang tải khóa học...</p>
  }
  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 404) {
      return (
        <>
          <h1 className="font-serif text-4xl font-bold tracking-tight">Không tìm thấy khóa học</h1>
          <Link to="/" className="link mt-6 inline-block">
            Về trang chủ
          </Link>
        </>
      )
    }
    return <LoadError message="Không tải được khóa học." onRetry={reload} />
  }

  const { course, lessons } = state.data
  const title = course?.title ?? courseSlug
  const linkState: LessonLinkState = { courseSlug, courseTitle: title }

  return (
    <>
      <Link to="/" className="link text-sm">
        <span aria-hidden="true">← </span>Tất cả khóa học
      </Link>
      <h1 className="mt-5 max-w-3xl font-serif text-4xl font-bold leading-tight tracking-tight sm:text-5xl">{title}</h1>
      {course?.description && <p className="mt-4 max-w-2xl text-lg text-muted">{course.description}</p>}

      {/* Hai cột ở màn rộng: mục lục bài bên trái, tiến độ bên phải. Ở màn hẹp tiến độ lên trước danh sách. */}
      <div className="mt-10 grid gap-8 lg:grid-cols-[1fr_19rem]">
        <div className="lg:col-start-2 lg:row-start-1">
          <div className="lg:sticky lg:top-6">
            <CourseProgressPanel
              progress={progress.state}
              courseSlug={courseSlug}
              linkState={linkState}
              onChanged={progress.reload}
            />
          </div>
        </div>

        <div className="lg:col-start-1 lg:row-start-1">
          <h2 className="mb-2 border-b-2 border-ink pb-2 font-serif text-2xl font-bold tracking-tight">
            Nội dung khóa học
          </h2>
          {lessons.length === 0 ? (
            <p className="mt-4 text-muted">Khóa học này chưa có bài học nào.</p>
          ) : (
            <ol className="divide-y divide-line border-b border-line">
              {lessons.map((lesson, index) => (
                <li key={lesson.id}>
                  <Link
                    to={`/bai/${encodeURIComponent(lesson.slug)}`}
                    state={linkState}
                    className="group grid grid-cols-[2.5rem_1fr_auto] items-center gap-x-4 py-4 hover:bg-sunken sm:px-2"
                  >
                    <span aria-hidden="true" className="font-serif text-xl font-bold tabular-nums text-accent">
                      {String(index + 1).padStart(2, '0')}
                    </span>
                    <span className="block">
                      <span className="font-semibold group-hover:underline group-hover:underline-offset-4">
                        {lesson.title}
                      </span>
                      {lesson.summary && <span className="mt-0.5 block text-sm text-muted">{lesson.summary}</span>}
                    </span>
                    {showCovers && <LessonThumb url={lesson.coverImageUrl} />}
                  </Link>
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>
    </>
  )
}
