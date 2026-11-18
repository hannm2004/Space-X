import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { createCourse, deleteCourse, fetchAdminCourses, updateCourse, type AdminCourse, type CourseInput } from '../adminApi'
import AdminGate from '../AdminGate'
import { CARD, DANGER, LINK, NO_PERMISSION, SECONDARY, SUCCESS_BOX } from '../adminUi'
import { ApiError } from '../client'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import LoadError from '../LoadError'
import { useLoad } from '../useLoad'

type FormValues = { slug: string; title: string; description: string; sortOrder: string }

const EMPTY_VALUES: FormValues = { slug: '', title: '', description: '', sortOrder: '' }

function toValues(course: AdminCourse): FormValues {
  return {
    slug: course.slug,
    title: course.title,
    description: course.description ?? '',
    sortOrder: String(course.sortOrder),
  }
}

// Kiểm nhanh ở trình duyệt để báo sớm; backend vẫn kiểm lại đầy đủ (độ dài, dạng slug, trùng slug).
function parseValues(values: FormValues): { input: CourseInput } | { errors: string[] } {
  const slug = values.slug.trim()
  const title = values.title.trim()
  const errors: string[] = []
  if (slug === '' || title === '') errors.push('Hãy nhập slug và tên khóa học.')
  const sortText = values.sortOrder.trim()
  if (!/^\d{1,9}$/.test(sortText)) errors.push('Thứ tự hiển thị phải là số nguyên không âm.')
  if (errors.length > 0) return { errors }
  return { input: { slug, title, description: values.description.trim(), sortOrder: Number(sortText) } }
}

type CourseFormProps = {
  idPrefix: string
  initial: FormValues
  submitLabel: string
  pendingLabel: string
  clearOnSuccess?: boolean
  onSubmit: (input: CourseInput) => Promise<void>
  onCancel?: () => void
}

function CourseForm({ idPrefix, initial, submitLabel, pendingLabel, clearOnSuccess, onSubmit, onCancel }: CourseFormProps) {
  const [values, setValues] = useState(initial)
  const [busy, setBusy] = useState(false)
  const [messages, setMessages] = useState<string[]>([])

  function set(field: keyof FormValues) {
    return (value: string) => setValues((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    const parsed = parseValues(values)
    if ('errors' in parsed) {
      setMessages(parsed.errors)
      return
    }
    setBusy(true)
    setMessages([])
    try {
      await onSubmit(parsed.input)
      if (clearOnSuccess) setValues(EMPTY_VALUES)
    } catch (error) {
      setMessages(errorMessages(error, 'Không lưu được khóa học. Hãy thử lại.'))
    }
    setBusy(false)
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-3">
      <TextField
        id={`${idPrefix}-slug`}
        label="Slug"
        value={values.slug}
        onChange={set('slug')}
        hint="Chữ thường không dấu, số và dấu gạch ngang, ví dụ tin-hoc-10."
      />
      <TextField id={`${idPrefix}-title`} label="Tên khóa học" value={values.title} onChange={set('title')} />
      <TextField
        id={`${idPrefix}-description`}
        label="Mô tả (không bắt buộc)"
        value={values.description}
        onChange={set('description')}
      />
      <TextField
        id={`${idPrefix}-sort`}
        label="Thứ tự hiển thị"
        value={values.sortOrder}
        onChange={set('sortOrder')}
        hint="Số nhỏ hiện trước."
      />
      <ErrorAlert messages={messages} />
      <div className="flex gap-3">
        <SubmitButton pending={busy} label={submitLabel} pendingLabel={pendingLabel} />
        {onCancel && (
          <button type="button" disabled={busy} onClick={onCancel} className={SECONDARY}>
            Hủy
          </button>
        )}
      </div>
    </form>
  )
}

type Mode = 'view' | 'edit' | 'delete'

function CourseRow({ course, onChanged }: { course: AdminCourse; onChanged: (message: string) => void }) {
  const [mode, setMode] = useState<Mode>('view')
  const [busy, setBusy] = useState(false)
  const [messages, setMessages] = useState<string[]>([])

  async function handleDelete() {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      await deleteCourse(course.id)
      onChanged('Đã xóa khóa học.')
    } catch (error) {
      setMessages(errorMessages(error, 'Không xóa được khóa học. Hãy thử lại.'))
    }
    setBusy(false)
  }

  return (
    <li className={CARD}>
      {mode === 'edit' ? (
        <CourseForm
          idPrefix={`course-${course.id}`}
          initial={toValues(course)}
          submitLabel="Lưu thay đổi"
          pendingLabel="Đang lưu..."
          onSubmit={async (input) => {
            await updateCourse(course.id, input)
            onChanged('Đã lưu khóa học.')
          }}
          onCancel={() => setMode('view')}
        />
      ) : (
        <>
          <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
            <span className="font-semibold">{course.title}</span>
            <code className="text-sm text-slate-600 dark:text-slate-400">{course.slug}</code>
            <span className="text-sm text-slate-600 dark:text-slate-400">{`Thứ tự ${course.sortOrder}`}</span>
          </div>
          {course.description && <p className="mt-1 text-slate-700 dark:text-slate-300">{course.description}</p>}
          {mode === 'view' && (
            <div className="mt-3 flex flex-wrap gap-3">
              <button
                type="button"
                aria-label={`Sửa ${course.title}`}
                onClick={() => setMode('edit')}
                className={SECONDARY}
              >
                Sửa
              </button>
              <button
                type="button"
                aria-label={`Xóa ${course.title}`}
                onClick={() => setMode('delete')}
                className={SECONDARY}
              >
                Xóa
              </button>
            </div>
          )}
          {mode === 'delete' && (
            <div className="mt-3 rounded-lg border border-slate-200 p-3 dark:border-slate-700">
              <p>{`Xóa khóa học "${course.title}"? Chỉ xóa được khi khóa không còn bài học. Việc này không hoàn tác được.`}</p>
              <div className="mt-3 flex gap-3">
                <button type="button" disabled={busy} onClick={handleDelete} className={DANGER}>
                  Xác nhận xóa
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => {
                    setMode('view')
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
        </>
      )}
    </li>
  )
}

function CourseManager() {
  const { state, reload } = useLoad(fetchAdminCourses, 'courses')
  const [status, setStatus] = useState<string | null>(null)

  function changed(message: string) {
    setStatus(message)
    reload()
  }

  let list
  if (state.kind === 'loading') {
    list = <p className="mt-6 text-slate-600 dark:text-slate-400">Đang tải...</p>
  } else if (state.kind === 'error') {
    list =
      state.error instanceof ApiError && state.error.status === 403 ? (
        <p role="alert" className="mt-6 font-medium text-red-700 dark:text-red-400">
          {NO_PERMISSION}
        </p>
      ) : (
        <LoadError message="Không tải được danh sách khóa học." onRetry={reload} />
      )
  } else if (state.data.length === 0) {
    list = <p className="mt-6 text-lg">Chưa có khóa học nào.</p>
  } else {
    list = (
      <ul className="mt-6 space-y-3">
        {state.data.map((course) => (
          <CourseRow key={course.id} course={course} onChanged={changed} />
        ))}
      </ul>
    )
  }

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Quản lý khóa học</h1>
      <p className="mt-3">
        <Link to="/quan-tri/bai-hoc" className={LINK}>
          Quản lý bài học
        </Link>
      </p>

      {status !== null && (
        <p role="status" className={SUCCESS_BOX}>
          {status}
        </p>
      )}

      <section aria-labelledby="add-course-heading" className={`mt-6 ${CARD}`}>
        <h2 id="add-course-heading" className="text-xl font-semibold">
          Thêm khóa học
        </h2>
        <div className="mt-3">
          <CourseForm
            idPrefix="new-course"
            initial={EMPTY_VALUES}
            submitLabel="Thêm khóa học"
            pendingLabel="Đang thêm..."
            clearOnSuccess
            onSubmit={async (input) => {
              await createCourse(input)
              changed('Đã thêm khóa học.')
            }}
          />
        </div>
      </section>

      {list}
    </>
  )
}

export default function AdminCoursesPage() {
  return (
    <AdminGate>
      <CourseManager />
    </AdminGate>
  )
}
