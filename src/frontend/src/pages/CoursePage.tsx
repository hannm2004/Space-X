import { Link, useParams } from 'react-router'
import { SECONDARY } from '../buttons'
import { ApiError } from '../client'
import CourseCover from '../CourseCover'
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
          <h1 className="text-4xl font-bold tracking-tight">Không tìm thấy khóa học</h1>
          <Link to="/" className={`${SECONDARY} mt-6`}>
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
      <Link to="/" className={SECONDARY}>
        <span aria-hidden="true">← </span>Tất cả khóa học
      </Link>
      {/* Đầu trang: tên và mô tả bên trái, ảnh bìa bên phải (xếp dọc ở màn hẹp). Ảnh bìa khóa công khai, khách cũng thấy. */}
      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_22rem] lg:items-center">
        <div>
          <h1 className="max-w-3xl text-4xl font-bold leading-tight tracking-tight sm:text-5xl">{title}</h1>
          {course?.description && <p className="mt-4 max-w-2xl text-lg text-muted">{course.description}</p>}
        </div>
        <CourseCover slug={courseSlug} coverImageUrl={course?.coverImageUrl} className="rounded-xl border border-line" priority />
      </div>

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
          <h2 className="text-2xl font-bold tracking-tight">Nội dung khóa học</h2>
          {lessons.length === 0 ? (
            <p className="mt-4 text-muted">Khóa học này chưa có bài học nào.</p>
          ) : (
            <ol className="mt-4 divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface shadow-card">
              {lessons.map((lesson, index) => (
                <li key={lesson.id}>
                  <Link
                    to={`/bai/${encodeURIComponent(lesson.slug)}`}
                    state={linkState}
                    className="group grid grid-cols-[2.25rem_1fr_auto] items-center gap-x-4 px-4 py-4 hover:bg-sunken sm:px-5"
                  >
                    <span
                      aria-hidden="true"
                      className="grid size-9 place-items-center rounded-full bg-accent-soft text-sm font-semibold tabular-nums text-accent"
                    >
                      {index + 1}
                    </span>
                    <span className="block">
                      <span className="font-semibold group-hover:text-accent">
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
