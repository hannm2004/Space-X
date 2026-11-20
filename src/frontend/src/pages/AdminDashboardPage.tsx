import { useId, type ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { fetchAdminDashboard, type AdminDashboard } from '../adminApi'
import { ApiError } from '../client'
import LoadError from '../LoadError'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'

const NO_PERMISSION = 'Bạn không có quyền truy cập trang này.'
const CELL = 'px-3 py-2'

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
      <dt className="text-sm font-medium text-muted">{label}</dt>
      <dd className="mt-1 text-4xl font-bold tabular-nums">{value.toLocaleString('vi-VN')}</dd>
    </div>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  // Id sinh tự động: tiêu đề tiếng Việt có dấu cách nên không dùng được làm id cho aria-labelledby.
  const headingId = useId()
  return (
    <section aria-labelledby={headingId}>
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
    <div className="mt-3 overflow-x-auto rounded-xl border border-line bg-surface shadow-card">
      <table aria-label="Số bài theo khóa" className="w-full text-left">
        <thead>
          <tr className="border-b border-line bg-sunken">
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
            <tr key={course.courseId} className="border-b border-line last:border-b-0">
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
    return <p className="mt-6 text-muted">Đang tải...</p>
  }
  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 403) {
      return (
        <p role="alert" className="mt-6 font-medium text-danger">
          {NO_PERMISSION}
        </p>
      )
    }
    return <LoadError message="Không tải được số liệu tổng quan." onRetry={reload} />
  }

  const { users, lessons, courses, learning } = state.data
  return (
    <div className="mt-8 space-y-10">
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
      <section>
        <h2 className="text-lg font-semibold">Bài học theo khóa</h2>
        <CourseTable courses={courses} />
      </section>
    </div>
  )
}

export default function AdminDashboardPage() {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'loading') {
    return <p className="text-muted">Đang tải...</p>
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/dang-nhap" replace state={{ from: location.pathname }} />
  }
  if (state.user.role !== 'ADMIN') {
    return (
      <p role="alert" className="font-medium text-danger">
        {NO_PERMISSION}
      </p>
    )
  }

  return (
    <>
      <h1 className="text-4xl font-bold leading-tight tracking-tight">Tổng quan</h1>
      <DashboardContent />
    </>
  )
}
