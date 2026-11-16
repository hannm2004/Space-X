import { useId, type ReactNode } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { fetchAdminDashboard, type AdminDashboard } from '../adminApi'
import { ApiError } from '../client'
import LoadError from '../LoadError'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'

const NO_PERMISSION = 'Bạn không có quyền truy cập trang này.'
const LINK =
  'rounded text-blue-700 underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:text-blue-400'
const CELL = 'px-3 py-2'

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <dt className="text-sm text-slate-600 dark:text-slate-400">{label}</dt>
      <dd className="text-2xl font-bold tabular-nums">{value.toLocaleString('vi-VN')}</dd>
    </div>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  // Id sinh tự động: tiêu đề tiếng Việt có dấu cách nên không dùng được làm id cho aria-labelledby.
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className="rounded-lg border border-slate-200 p-4 dark:border-slate-700">
      <h2 id={headingId} className="text-lg font-semibold">
        {title}
      </h2>
      <dl className="mt-3 grid grid-cols-2 gap-4 sm:grid-cols-3">{children}</dl>
    </section>
  )
}

function CourseTable({ courses }: { courses: AdminDashboard['courses'] }) {
  if (courses.length === 0) {
    return <p className="mt-3">Chưa có khóa học nào.</p>
  }
  return (
    <div className="mt-3 overflow-x-auto">
      <table aria-label="Số bài theo khóa" className="w-full text-left">
        <thead>
          <tr className="border-b border-slate-300 dark:border-slate-600">
            <th scope="col" className={CELL}>
              Khóa
            </th>
            <th scope="col" className={`${CELL} text-right`}>
              Đã đăng
            </th>
            <th scope="col" className={`${CELL} text-right`}>
              Nháp
            </th>
          </tr>
        </thead>
        <tbody>
          {courses.map((course) => (
            <tr key={course.courseId} className="border-b border-slate-200 dark:border-slate-700">
              <td className={CELL}>{course.title}</td>
              <td className={`${CELL} text-right tabular-nums`}>{course.published}</td>
              <td className={`${CELL} text-right tabular-nums`}>{course.draft}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function DashboardContent() {
  const { state, reload } = useLoad(fetchAdminDashboard, 'dashboard')

  if (state.kind === 'loading') {
    return <p className="mt-6 text-slate-600 dark:text-slate-400">Đang tải...</p>
  }
  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 403) {
      return (
        <p role="alert" className="mt-6 font-medium text-red-700 dark:text-red-400">
          {NO_PERMISSION}
        </p>
      )
    }
    return <LoadError message="Không tải được số liệu tổng quan." onRetry={reload} />
  }

  const { users, lessons, courses, learning } = state.data
  return (
    <div className="mt-6 space-y-6">
      <Section title="Người dùng">
        <Stat label="Tổng số tài khoản" value={users.total} />
        <Stat label="Mới trong 7 ngày" value={users.newLast7Days} />
      </Section>
      <Section title="Bài học">
        <Stat label="Tổng số bài" value={lessons.total} />
        <Stat label="Đã đăng" value={lessons.published} />
        <Stat label="Nháp" value={lessons.draft} />
      </Section>
      <Section title="Học tập">
        <Stat label="Học sinh đã bắt đầu học" value={learning.activeLearners} />
        <Stat label="Lượt hoàn thành bài" value={learning.completedLessons} />
      </Section>
      <section className="rounded-lg border border-slate-200 p-4 dark:border-slate-700">
        <h2 className="text-lg font-semibold">Bài học theo khóa</h2>
        <CourseTable courses={courses} />
      </section>
      <p>
        <Link to="/quan-tri/nguoi-dung" className={LINK}>
          Quản lý người dùng
        </Link>
      </p>
    </div>
  )
}

export default function AdminDashboardPage() {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'loading') {
    return <p className="text-slate-600 dark:text-slate-400">Đang tải...</p>
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/dang-nhap" replace state={{ from: location.pathname }} />
  }
  if (state.user.role !== 'ADMIN') {
    return (
      <p role="alert" className="font-medium text-red-700 dark:text-red-400">
        {NO_PERMISSION}
      </p>
    )
  }

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Tổng quan</h1>
      <DashboardContent />
    </>
  )
}
