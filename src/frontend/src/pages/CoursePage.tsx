import { Link, useParams } from 'react-router'
import { ApiError } from '../client'
import CourseProgressPanel from '../CourseProgressPanel'
import LoadError from '../LoadError'
import { fetchCourses, fetchLessons } from '../learningApi'
import type { LessonLinkState } from '../lessonLinkState'
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

  if (state.kind === 'loading') {
    return <p className="text-slate-600 dark:text-slate-400">Đang tải khóa học...</p>
  }
  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 404) {
      return (
        <>
          <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Không tìm thấy khóa học</h1>
          <Link to="/" className="mt-6 inline-block text-blue-700 underline dark:text-blue-400">
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
      <Link to="/" className="text-sm text-blue-700 underline dark:text-blue-400">
        <span aria-hidden="true">← </span>Tất cả khóa học
      </Link>
      <h1 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">{title}</h1>
      {course?.description && (
        <p className="mt-3 text-slate-600 dark:text-slate-400">{course.description}</p>
      )}

      <CourseProgressPanel
        progress={progress.state}
        courseSlug={courseSlug}
        linkState={linkState}
        onChanged={progress.reload}
      />

      {lessons.length === 0 ? (
        <p className="mt-8 text-slate-600 dark:text-slate-400">Khóa học này chưa có bài học nào.</p>
      ) : (
        <ol className="mt-8 space-y-3">
          {lessons.map((lesson) => (
            <li key={lesson.id}>
              <Link
                to={`/bai/${encodeURIComponent(lesson.slug)}`}
                state={linkState}
                className="block rounded-xl border border-slate-200 bg-white p-4 hover:border-blue-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-blue-400"
              >
                <span className="font-semibold">{lesson.title}</span>
                {lesson.summary && (
                  <span className="mt-1 block text-sm text-slate-600 dark:text-slate-400">
                    {lesson.summary}
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ol>
      )}
    </>
  )
}
