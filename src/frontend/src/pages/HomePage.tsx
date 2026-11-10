import { Link } from 'react-router'
import HealthStatus from '../HealthStatus'
import LoadError from '../LoadError'
import { fetchCourses } from '../learningApi'
import { useLoad } from '../useLoad'

function CourseList() {
  const { state, reload } = useLoad(fetchCourses, 'courses')

  if (state.kind === 'loading') {
    return <p className="mt-3 text-slate-600 dark:text-slate-400">Đang tải danh sách khóa học...</p>
  }
  if (state.kind === 'error') {
    return <LoadError message="Không tải được danh sách khóa học." onRetry={reload} />
  }
  if (state.data.length === 0) {
    return <p className="mt-3 text-slate-600 dark:text-slate-400">Chưa có khóa học nào.</p>
  }
  return (
    <ul className="mt-4 grid gap-4 sm:grid-cols-3">
      {state.data.map((course) => (
        <li key={course.id}>
          <Link
            to={`/khoa-hoc/${encodeURIComponent(course.slug)}`}
            className="block h-full rounded-xl border border-slate-200 bg-white p-5 hover:border-blue-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-blue-400"
          >
            <h3 className="text-lg font-semibold">{course.title}</h3>
            {course.description && (
              <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{course.description}</p>
            )}
          </Link>
        </li>
      ))}
    </ul>
  )
}

export default function HomePage() {
  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Học Tin học 10, 11, 12</h1>
      <p className="mt-3 text-slate-600 dark:text-slate-400">
        Bài học Tin học THPT bằng tiếng Việt, kèm chatbot và AI Agent trả lời có trích dẫn nguồn.
      </p>

      <section aria-labelledby="courses-title" className="mt-8">
        <h2 id="courses-title" className="text-xl font-semibold tracking-tight">
          Khóa học
        </h2>
        <CourseList />
      </section>

      <section
        aria-labelledby="health-title"
        className="mt-10 rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900"
      >
        <h2
          id="health-title"
          className="text-sm font-semibold uppercase tracking-wide text-slate-600 dark:text-slate-400"
        >
          Trạng thái hệ thống
        </h2>
        <HealthStatus />
      </section>
    </>
  )
}
