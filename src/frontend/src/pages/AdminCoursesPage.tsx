import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import {
  createCourse,
  deleteCourse,
  deleteCourseCover,
  fetchAdminCourses,
  fetchCategories,
  updateCourse,
  uploadCourseCover,
  type AdminCourse,
  type Category,
  type CourseInput,
  type CourseLevel,
  type CourseStatus,
} from '../adminApi'
import AdminGate from '../AdminGate'
import { CARD, DANGER, NO_PERMISSION, SECONDARY, SUCCESS_BOX } from '../adminUi'
import { ApiError } from '../client'
import CourseCover from '../CourseCover'
import FilePickButton from '../FilePickButton'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import { IMAGE_ACCEPT, IMAGE_HINT, imageProblem } from '../imageUpload'
import LoadError from '../LoadError'
import { useLoad } from '../useLoad'

type FormValues = {
  slug: string
  title: string
  description: string
  sortOrder: string
  status: CourseStatus
  tuition: string
  categoryId: string
  level: CourseLevel
  totalHours: string
}

// Khóa mới mặc định là bản nháp, cơ bản, chưa có học phí và số giờ; nhóm là nhóm đứng đầu danh sách.
function emptyValues(categories: Category[]): FormValues {
  return {
    slug: '',
    title: '',
    description: '',
    sortOrder: '',
    status: 'DRAFT',
    tuition: '0',
    categoryId: String(categories[0]?.id ?? ''),
    level: 'BASIC',
    totalHours: '0',
  }
}

const LEVEL_LABELS: Record<CourseLevel, string> = {
  BASIC: 'Cơ bản',
  ADVANCED: 'Nâng cao',
  EXPERT: 'Chuyên sâu',
}

// Cùng mức trần với backend.
const MAX_HOURS = 10_000

const STATUS_LABELS: Record<CourseStatus, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã đăng',
  ARCHIVED: 'Ngừng bán',
}

// Cùng mức trần với backend.
const MAX_TUITION = 1_000_000_000

function toValues(course: AdminCourse): FormValues {
  return {
    slug: course.slug,
    title: course.title,
    description: course.description ?? '',
    sortOrder: String(course.sortOrder),
    status: course.status,
    tuition: String(course.tuitionVnd),
    categoryId: String(course.categoryId),
    level: course.level,
    totalHours: String(course.totalHours),
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
  const tuitionText = values.tuition.trim()
  if (!/^\d{1,10}$/.test(tuitionText) || Number(tuitionText) > MAX_TUITION) {
    errors.push('Học phí phải là số nguyên từ 0 đến 1.000.000.000 đồng, không có dấu chấm.')
  }
  const hoursText = values.totalHours.trim()
  if (!/^\d{1,5}$/.test(hoursText) || Number(hoursText) > MAX_HOURS) {
    errors.push('Số giờ học phải là số nguyên từ 0 đến 10000.')
  }
  if (errors.length > 0) return { errors }
  return {
    input: {
      slug,
      title,
      description: values.description.trim(),
      sortOrder: Number(sortText),
      status: values.status,
      tuitionVnd: Number(tuitionText),
      categoryId: Number(values.categoryId),
      level: values.level,
      totalHours: Number(hoursText),
    },
  }
}

type SelectFieldProps = {
  id: string
  label: string
  value: string
  options: [value: string, label: string][]
  onChange: (value: string) => void
}

function SelectField({ id, label, value, options, onChange }: SelectFieldProps) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 field">
        {options.map(([optionValue, optionLabel]) => (
          <option key={optionValue} value={optionValue}>
            {optionLabel}
          </option>
        ))}
      </select>
    </div>
  )
}

type CourseFormProps = {
  categories: Category[]
  idPrefix: string
  initial: FormValues
  submitLabel: string
  pendingLabel: string
  clearOnSuccess?: boolean
  onSubmit: (input: CourseInput) => Promise<void>
  onCancel?: () => void
}

function CourseForm({
  categories,
  idPrefix,
  initial,
  submitLabel,
  pendingLabel,
  clearOnSuccess,
  onSubmit,
  onCancel,
}: CourseFormProps) {
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
      if (clearOnSuccess) setValues(initial)
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
      <SelectField
        id={`${idPrefix}-category`}
        label="Nhóm chương trình"
        value={values.categoryId}
        options={categories.map((category) => [String(category.id), category.name])}
        onChange={set('categoryId')}
      />
      <SelectField
        id={`${idPrefix}-level`}
        label="Trình độ"
        value={values.level}
        options={Object.entries(LEVEL_LABELS)}
        onChange={set('level')}
      />
      <TextField
        id={`${idPrefix}-hours`}
        label="Số giờ học"
        value={values.totalHours}
        onChange={set('totalHours')}
        hint="Tổng số giờ của khóa. Để 0 nếu chưa chốt."
      />
      <SelectField
        id={`${idPrefix}-status`}
        label="Trạng thái"
        value={values.status}
        options={Object.entries(STATUS_LABELS)}
        onChange={set('status')}
      />
      <TextField
        id={`${idPrefix}-tuition`}
        label="Học phí (đồng)"
        value={values.tuition}
        onChange={set('tuition')}
        hint="Chỉ nhập chữ số, ví dụ 1500000. Để 0 nếu chưa có học phí."
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

type Mode = 'view' | 'edit' | 'delete' | 'removeCover'

type CourseRowProps = { course: AdminCourse; categories: Category[]; onChanged: (message: string) => void }

function CourseRow({ course, categories, onChanged }: CourseRowProps) {
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

  // Ảnh bìa khóa: kiểm ở trình duyệt chỉ để báo sớm, máy chủ kiểm lại loại tệp thật và cỡ. Tải ảnh mới là thay ảnh cũ.
  async function handleCoverFile(file: File) {
    if (busy) return
    const problem = imageProblem(file)
    if (problem !== null) {
      setMessages([problem])
      return
    }
    setBusy(true)
    setMessages([])
    try {
      await uploadCourseCover(course.id, file)
      onChanged('Đã đặt ảnh bìa.')
    } catch (error) {
      setMessages(errorMessages(error, 'Không đặt được ảnh bìa. Hãy thử lại.'))
    }
    setBusy(false)
  }

  async function handleRemoveCover() {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      await deleteCourseCover(course.id)
      onChanged('Đã bỏ ảnh bìa.')
    } catch (error) {
      setMessages(errorMessages(error, 'Không bỏ được ảnh bìa. Hãy thử lại.'))
    }
    setMode('view')
    setBusy(false)
  }

  const hasCover = Boolean(course.coverImageUrl)
  const coverLabel = hasCover ? 'Đổi ảnh bìa' : 'Tải ảnh bìa'

  return (
    <li className={CARD}>
      {mode === 'edit' ? (
        <CourseForm
          categories={categories}
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
        <div className="flex flex-col gap-4 sm:flex-row">
          {/* Ảnh bìa hiện cho mọi khóa: ảnh đã tải lên, hoặc hình mặc định theo lớp khi chưa có. */}
          <CourseCover
            slug={course.slug}
            coverImageUrl={course.coverImageUrl}
            className="w-full shrink-0 rounded-lg border border-line sm:w-44 sm:self-start"
          />
          <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
            <span className="font-semibold">{course.title}</span>
            <code className="text-sm text-muted">{course.slug}</code>
            <span className="text-sm text-muted">{`Thứ tự ${course.sortOrder}`}</span>
            <span className="text-sm font-medium">{STATUS_LABELS[course.status]}</span>
            <span className="text-sm text-muted tabular-nums">
              {course.tuitionVnd > 0 ? `${course.tuitionVnd.toLocaleString('vi-VN')} đ` : 'Chưa có học phí'}
            </span>
          </div>
          <div className="mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-1 text-sm text-muted">
            <span>{categories.find((category) => category.id === course.categoryId)?.name}</span>
            <span>{LEVEL_LABELS[course.level]}</span>
            <span>{course.totalHours > 0 ? `${course.totalHours} giờ` : 'Chưa có số giờ'}</span>
          </div>
          {course.description && <p className="mt-1 text-ink">{course.description}</p>}
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
              <FilePickButton
                label={coverLabel}
                ariaLabel={`${coverLabel} ${course.title}`}
                inputLabel={`Tệp ảnh bìa ${course.title} (${IMAGE_HINT})`}
                accept={IMAGE_ACCEPT}
                disabled={busy}
                onFile={handleCoverFile}
              />
              {hasCover && (
                <button
                  type="button"
                  aria-label={`Bỏ ảnh bìa ${course.title}`}
                  disabled={busy}
                  onClick={() => setMode('removeCover')}
                  className={SECONDARY}
                >
                  Bỏ ảnh bìa
                </button>
              )}
            </div>
          )}
          {/* Bỏ ảnh bìa là xóa ảnh đã tải lên (không hoàn tác được), nên cũng phải xác nhận như xóa khóa. */}
          {mode === 'removeCover' && (
            <div className="mt-3 rounded-lg border border-line bg-paper p-3">
              <p>{`Bỏ ảnh bìa của khóa "${course.title}"? Ảnh đã tải lên sẽ bị xóa và khóa quay về hình mặc định theo lớp. Việc này không hoàn tác được.`}</p>
              <div className="mt-3 flex gap-3">
                <button type="button" disabled={busy} onClick={handleRemoveCover} className={DANGER}>
                  Xác nhận bỏ ảnh bìa
                </button>
                <button type="button" disabled={busy} onClick={() => setMode('view')} className={SECONDARY}>
                  Hủy
                </button>
              </div>
            </div>
          )}
          {mode === 'delete' && (
            <div className="mt-3 rounded-lg border border-line bg-paper p-3">
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
          </div>
        </div>
      )}
    </li>
  )
}

function CourseManager() {
  const { state, reload } = useLoad(fetchAdminCourses, 'courses')
  // Tải riêng: reload() sau mỗi thay đổi không làm form thêm khóa bị tháo ra rồi dựng lại.
  const { state: categories, reload: reloadCategories } = useLoad(fetchCategories, 'categories')
  const [status, setStatus] = useState<string | null>(null)

  function changed(message: string) {
    setStatus(message)
    reload()
  }

  let list
  if (categories.kind === 'error') {
    list = <LoadError message="Không tải được danh sách nhóm chương trình." onRetry={reloadCategories} />
  } else if (state.kind === 'loading' || categories.kind === 'loading') {
    list = <p className="mt-6 text-muted">Đang tải...</p>
  } else if (state.kind === 'error') {
    list =
      state.error instanceof ApiError && state.error.status === 403 ? (
        <p role="alert" className="mt-6 font-medium text-danger">
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
          <CourseRow key={course.id} course={course} categories={categories.data} onChanged={changed} />
        ))}
      </ul>
    )
  }

  return (
    <>
      <h1 className="text-4xl font-bold leading-tight tracking-tight">Quản lý khóa học</h1>
      <p className="mt-3">
        <Link to="/quan-tri/bai-hoc" className={SECONDARY}>
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
          {categories.kind === 'ok' && (
            <CourseForm
              categories={categories.data}
              idPrefix="new-course"
              initial={emptyValues(categories.data)}
              submitLabel="Thêm khóa học"
              pendingLabel="Đang thêm..."
              clearOnSuccess
              onSubmit={async (input) => {
                await createCourse(input)
                changed('Đã thêm khóa học.')
              }}
            />
          )}
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
