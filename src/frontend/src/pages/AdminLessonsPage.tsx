import { useState, type ChangeEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import {
  deleteLesson,
  fetchAdminCourses,
  fetchAdminLessons,
  setLessonStatus,
  type AdminCourse,
  type AdminLessonSummary,
  type LessonFilter,
  type LessonStatus,
} from '../adminApi'
import AdminGate from '../AdminGate'
import { CARD, DANGER, LINK, NO_PERMISSION, SECONDARY, SUCCESS_BOX } from '../adminUi'
import { ApiError } from '../client'
import { errorMessages } from '../formErrors'
import { ErrorAlert } from '../FormParts'
import LoadError from '../LoadError'
import { useLoad } from '../useLoad'

const PATH = '/quan-tri/bai-hoc'
const SELECT = `field w-auto`
const STATUS_LABEL: Record<LessonStatus, string> = { DRAFT: 'Nháp', PUBLISHED: 'Đã đăng' }

// Tham số lạ trong địa chỉ coi như không có, không gửi lên máy chủ.
function parseFilter(params: URLSearchParams): LessonFilter {
  const course = params.get('khoa')
  const status = params.get('trang-thai')
  const page = params.get('trang')
  return {
    courseId: course !== null && /^\d{1,9}$/.test(course) ? Number(course) : null,
    status: status === 'DRAFT' || status === 'PUBLISHED' ? status : null,
    page: page !== null && /^\d{1,6}$/.test(page) ? Number(page) : 0,
  }
}

function hrefFor(filter: Pick<LessonFilter, 'courseId' | 'status'>, page: number | null): string {
  const params = new URLSearchParams()
  if (filter.courseId !== null) params.set('khoa', String(filter.courseId))
  if (filter.status !== null) params.set('trang-thai', filter.status)
  if (page !== null) params.set('trang', String(page))
  const query = params.toString()
  return query === '' ? PATH : `${PATH}?${query}`
}

function FilterBar({ filter, courses }: { filter: LessonFilter; courses: AdminCourse[] }) {
  const navigate = useNavigate()

  function changeCourse(event: ChangeEvent<HTMLSelectElement>) {
    const value = event.target.value
    navigate(hrefFor({ courseId: value === '' ? null : Number(value), status: filter.status }, null))
  }

  function changeStatus(event: ChangeEvent<HTMLSelectElement>) {
    const value = event.target.value
    navigate(hrefFor({ courseId: filter.courseId, status: value === '' ? null : (value as LessonStatus) }, null))
  }

  return (
    <div className="mt-6 flex flex-wrap gap-4">
      <div>
        <label htmlFor="filter-course" className="block text-sm font-medium">
          Khóa học
        </label>
        <select id="filter-course" value={filter.courseId ?? ''} onChange={changeCourse} className={`mt-1 ${SELECT}`}>
          <option value="">Tất cả khóa học</option>
          {courses.map((course) => (
            <option key={course.id} value={course.id}>
              {course.title}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor="filter-status" className="block text-sm font-medium">
          Trạng thái
        </label>
        <select id="filter-status" value={filter.status ?? ''} onChange={changeStatus} className={`mt-1 ${SELECT}`}>
          <option value="">Tất cả trạng thái</option>
          <option value="DRAFT">{STATUS_LABEL.DRAFT}</option>
          <option value="PUBLISHED">{STATUS_LABEL.PUBLISHED}</option>
        </select>
      </div>
    </div>
  )
}

type RowProps = { initial: AdminLessonSummary; courseTitle: string; onDeleted: (message: string) => void }

// Mỗi dòng giữ trạng thái riêng (đang xác nhận xóa, lỗi của dòng) để một thao tác lỗi không ảnh hưởng dòng khác.
function LessonRow({ initial, courseTitle, onDeleted }: RowProps) {
  const [lesson, setLesson] = useState(initial)
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [messages, setMessages] = useState<string[]>([])

  async function changeStatus(status: LessonStatus) {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      const updated = await setLessonStatus(lesson.id, status)
      setLesson((current) => ({ ...current, status: updated.status }))
    } catch (error) {
      setMessages(errorMessages(error, 'Không đổi được trạng thái bài. Hãy thử lại.'))
    }
    setBusy(false)
  }

  async function handleDelete() {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      await deleteLesson(lesson.id)
      onDeleted('Đã xóa bài học.')
    } catch (error) {
      setMessages(errorMessages(error, 'Không xóa được bài học. Hãy thử lại.'))
    }
    setBusy(false)
  }

  const published = lesson.status === 'PUBLISHED'
  return (
    <li className={CARD}>
      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <span className="font-semibold">{lesson.title}</span>
        {/* Trạng thái bằng chữ, không chỉ bằng màu. */}
        <span
          className={
            published
              ? 'text-sm font-medium text-success'
              : 'text-sm font-medium text-warn'
          }
        >
          {STATUS_LABEL[lesson.status]}
        </span>
      </div>
      <p className="mt-1 text-sm text-muted">
        <span>{courseTitle}</span>
        {' · '}
        <code>{lesson.slug}</code>
        {' · '}
        <span>{`Thứ tự ${lesson.sortOrder}`}</span>
      </p>

      {!confirming && (
        <div className="mt-3 flex flex-wrap gap-3">
          <Link to={`${PATH}/${lesson.id}/sua`} aria-label={`Sửa ${lesson.title}`} className={SECONDARY}>
            Sửa
          </Link>
          {published ? (
            <button
              type="button"
              aria-label={`Ẩn bài ${lesson.title}`}
              disabled={busy}
              onClick={() => changeStatus('DRAFT')}
              className={SECONDARY}
            >
              Ẩn bài
            </button>
          ) : (
            <button
              type="button"
              aria-label={`Đăng bài ${lesson.title}`}
              disabled={busy}
              onClick={() => changeStatus('PUBLISHED')}
              className={SECONDARY}
            >
              Đăng bài
            </button>
          )}
          <button
            type="button"
            aria-label={`Xóa ${lesson.title}`}
            onClick={() => setConfirming(true)}
            className={SECONDARY}
          >
            Xóa
          </button>
        </div>
      )}

      {confirming && (
        <div className="mt-3 border border-line bg-surface p-3">
          <p>{`Xóa bài học "${lesson.title}"? Ảnh, tiến độ học của học sinh và nội dung bài sẽ mất, không hoàn tác được.`}</p>
          <div className="mt-3 flex gap-3">
            <button type="button" disabled={busy} onClick={handleDelete} className={DANGER}>
              Xác nhận xóa
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => {
                setConfirming(false)
                setMessages([])
              }}
              className={SECONDARY}
            >
              Hủy
            </button>
          </div>
        </div>
      )}

      {messages.length > 0 && (
        <div className="mt-3">
          <ErrorAlert messages={messages} />
        </div>
      )}
    </li>
  )
}

function LessonList({ filter, courses }: { filter: LessonFilter; courses: AdminCourse[] }) {
  const { state, reload } = useLoad(() => fetchAdminLessons(filter), `${filter.courseId}:${filter.status}:${filter.page}`)
  const [status, setStatus] = useState<string | null>(null)

  function deleted(message: string) {
    setStatus(message)
    reload()
  }

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
    return <LoadError message="Không tải được danh sách bài học." onRetry={reload} />
  }

  const result = state.data
  const courseTitles = new Map(courses.map((course) => [course.id, course.title]))
  const totalPages = Math.ceil(result.totalItems / result.size)
  return (
    <>
      {status !== null && (
        <p role="status" className={SUCCESS_BOX}>
          {status}
        </p>
      )}
      {result.totalItems === 0 ? (
        <p className="mt-6 text-lg">Không có bài học phù hợp.</p>
      ) : result.items.length === 0 ? (
        <p className="mt-6">
          Không có kết quả ở trang này.{' '}
          <Link to={hrefFor(filter, 0)} className={LINK}>
            Về trang đầu
          </Link>
        </p>
      ) : (
        <>
          <p className="mt-6 text-muted">{`Có ${result.totalItems} bài học`}</p>
          <ul className="mt-4 space-y-3">
            {result.items.map((lesson) => (
              <LessonRow
                key={lesson.id}
                initial={lesson}
                courseTitle={courseTitles.get(lesson.courseId) ?? `Khóa #${lesson.courseId}`}
                onDeleted={deleted}
              />
            ))}
          </ul>
        </>
      )}
      {totalPages > 1 && (
        <nav aria-label="Phân trang" className="mt-6 flex items-center gap-4">
          {result.page > 0 && (
            <Link to={hrefFor(filter, result.page - 1)} className={LINK}>
              Trang trước
            </Link>
          )}
          <span>{`Trang ${result.page + 1} / ${totalPages}`}</span>
          {result.page + 1 < totalPages && (
            <Link to={hrefFor(filter, result.page + 1)} className={LINK}>
              Trang sau
            </Link>
          )}
        </nav>
      )}
    </>
  )
}

function LessonManager() {
  const [params] = useSearchParams()
  const filter = parseFilter(params)
  // Danh sách khóa chỉ để đổ bộ lọc và tên khóa; tải lỗi thì vẫn dùng được trang (bộ lọc chỉ có "Tất cả").
  const courses = useLoad(fetchAdminCourses, 'courses')
  const courseList = courses.state.kind === 'ok' ? courses.state.data : []

  return (
    <>
      <h1 className="font-serif text-4xl font-bold leading-tight tracking-tight">Quản lý bài học</h1>
      <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2">
        <Link to={`${PATH}/moi`} className={LINK}>
          Thêm bài học
        </Link>
        <Link to="/quan-tri/khoa-hoc" className={LINK}>
          Quản lý khóa học
        </Link>
      </div>
      <FilterBar filter={filter} courses={courseList} />
      <LessonList filter={filter} courses={courseList} />
    </>
  )
}

export default function AdminLessonsPage() {
  return (
    <AdminGate>
      <LessonManager />
    </AdminGate>
  )
}
