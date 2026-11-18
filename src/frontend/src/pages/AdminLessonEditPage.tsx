import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import {
  createLesson,
  deleteImage,
  fetchAdminCourses,
  fetchAdminLesson,
  fetchLessonImages,
  setLessonStatus,
  updateLesson,
  uploadLessonImage,
  type AdminCourse,
  type AdminLesson,
  type LessonImage,
  type LessonInput,
  type LessonStatus,
} from '../adminApi'
import AdminGate from '../AdminGate'
import { CARD, DANGER, FOCUS, LINK, NO_PERMISSION, SECONDARY, SUCCESS_BOX } from '../adminUi'
import { ApiError } from '../client'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import LoadError from '../LoadError'
import MarkdownContent from '../MarkdownContent'
import { useLoad } from '../useLoad'

const LIST_PATH = '/quan-tri/bai-hoc'
const MAX_CONTENT_LENGTH = 100000
const MAX_IMAGE_BYTES = 1024 * 1024
const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']
const FIELD = `mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 ${FOCUS}`
const STATUS_LABEL: Record<LessonStatus, string> = { DRAFT: 'Nháp', PUBLISHED: 'Đã đăng' }

type FormValues = {
  courseId: string
  slug: string
  title: string
  summary: string
  sortOrder: string
  contentMd: string
}

const EMPTY_VALUES: FormValues = { courseId: '', slug: '', title: '', summary: '', sortOrder: '', contentMd: '' }

function toValues(lesson: AdminLesson): FormValues {
  return {
    courseId: String(lesson.courseId),
    slug: lesson.slug,
    title: lesson.title,
    summary: lesson.summary ?? '',
    sortOrder: String(lesson.sortOrder),
    contentMd: lesson.contentMd,
  }
}

// Kiểm nhanh ở trình duyệt để báo sớm; backend vẫn kiểm lại đầy đủ. Nội dung Markdown không bị cắt khoảng trắng
// (khoảng trắng đầu dòng có nghĩa), slug và tên thì cắt hai đầu như backend.
function parseValues(values: FormValues): { input: LessonInput } | { errors: string[] } {
  const errors: string[] = []
  if (values.courseId === '') errors.push('Hãy chọn khóa học.')
  const slug = values.slug.trim()
  const title = values.title.trim()
  if (slug === '' || title === '') errors.push('Hãy nhập slug và tên bài học.')
  const sortText = values.sortOrder.trim()
  if (!/^\d{1,9}$/.test(sortText)) errors.push('Thứ tự hiển thị phải là số nguyên không âm.')
  if (values.contentMd.length > MAX_CONTENT_LENGTH) errors.push(`Nội dung tối đa ${MAX_CONTENT_LENGTH} ký tự.`)
  if (errors.length > 0) return { errors }
  return {
    input: {
      courseId: Number(values.courseId),
      slug,
      title,
      summary: values.summary.trim(),
      contentMd: values.contentMd,
      sortOrder: Number(sortText),
    },
  }
}

function formatSize(bytes: number): string {
  return `${Math.max(1, Math.round(bytes / 1024))} KB`
}

// Cú pháp ảnh Markdown trỏ tới ảnh do hệ thống lưu; tên tệp bỏ đuôi và bỏ [ ] để không phá cú pháp.
function imageSnippet(image: LessonImage): string {
  const alt = image.fileName.replace(/\.[^.]*$/, '').replace(/[[\]]/g, '')
  return `![${alt}](${image.url})\n`
}

function appendSnippet(content: string, snippet: string): string {
  if (content === '') return snippet
  return `${content}${content.endsWith('\n') ? '\n' : '\n\n'}${snippet}`
}

function ImageItem({ image, onInsert, onDeleted }: { image: LessonImage; onInsert: () => void; onDeleted: () => void }) {
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [messages, setMessages] = useState<string[]>([])

  async function handleDelete() {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      await deleteImage(image.id)
      onDeleted()
    } catch (error) {
      setMessages(errorMessages(error, 'Không xóa được ảnh. Hãy thử lại.'))
    }
    setBusy(false)
  }

  return (
    <li className={CARD}>
      <div className="flex items-center gap-3">
        <img src={image.url} alt="" className="h-12 w-12 shrink-0 rounded object-cover" />
        <div className="min-w-0">
          <p className="break-all font-medium">{image.fileName}</p>
          <p className="text-sm text-slate-600 dark:text-slate-400">{formatSize(image.sizeBytes)}</p>
        </div>
      </div>
      {!confirming && (
        <div className="mt-3 flex flex-wrap gap-3">
          <button type="button" aria-label={`Chèn ảnh ${image.fileName} vào bài`} onClick={onInsert} className={SECONDARY}>
            Chèn vào bài
          </button>
          <button
            type="button"
            aria-label={`Xóa ảnh ${image.fileName}`}
            onClick={() => setConfirming(true)}
            className={SECONDARY}
          >
            Xóa
          </button>
        </div>
      )}
      {confirming && (
        <div className="mt-3 rounded-lg border border-slate-200 p-3 dark:border-slate-700">
          <p>Xóa ảnh này? Nếu bài còn chèn ảnh, ảnh sẽ không hiện nữa. Không hoàn tác được.</p>
          <div className="mt-3 flex gap-3">
            <button type="button" disabled={busy} onClick={handleDelete} className={DANGER}>
              Xác nhận xóa ảnh
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

function ImagePanel({ lessonId, onInsert }: { lessonId: number; onInsert: (snippet: string) => void }) {
  const { state, reload } = useLoad(() => fetchLessonImages(lessonId), `images:${lessonId}`)
  const [uploading, setUploading] = useState(false)
  const [messages, setMessages] = useState<string[]>([])
  const [notice, setNotice] = useState<string | null>(null)

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const input = event.target
    const file = input.files?.[0]
    if (!file || uploading) return
    setNotice(null)
    // Kiểm ở trình duyệt chỉ để báo sớm; máy chủ kiểm lại loại tệp thật và cỡ (415, 413).
    if (!IMAGE_TYPES.includes(file.type)) {
      setMessages(['Chỉ nhận ảnh PNG, JPEG hoặc WebP.'])
    } else if (file.size > MAX_IMAGE_BYTES) {
      setMessages(['Ảnh lớn hơn 1 MB. Hãy chọn ảnh nhỏ hơn.'])
    } else {
      setMessages([])
      setUploading(true)
      try {
        await uploadLessonImage(lessonId, file)
        setNotice('Đã tải ảnh. Bấm "Chèn vào bài" để thêm ảnh vào nội dung.')
        reload()
      } catch (error) {
        setMessages(errorMessages(error, 'Không tải được ảnh. Hãy thử lại.'))
      }
      setUploading(false)
    }
    input.value = ''
  }

  let list
  if (state.kind === 'loading') {
    list = <p className="mt-3 text-slate-600 dark:text-slate-400">Đang tải ảnh...</p>
  } else if (state.kind === 'error') {
    list = <LoadError message="Không tải được danh sách ảnh." onRetry={reload} />
  } else if (state.data.length === 0) {
    list = <p className="mt-3">Bài này chưa có ảnh.</p>
  } else {
    list = (
      <ul className="mt-3 space-y-3">
        {state.data.map((image) => (
          <ImageItem key={image.id} image={image} onInsert={() => onInsert(imageSnippet(image))} onDeleted={reload} />
        ))}
      </ul>
    )
  }

  return (
    <section aria-labelledby="images-heading" className="mt-8">
      <h2 id="images-heading" className="text-xl font-semibold">
        Ảnh của bài
      </h2>
      <div className="mt-3">
        <label htmlFor="image-upload" className="block text-sm font-medium">
          Tải ảnh lên (PNG, JPEG hoặc WebP, tối đa 1 MB)
        </label>
        <input
          id="image-upload"
          type="file"
          accept={IMAGE_TYPES.join(',')}
          disabled={uploading}
          onChange={handleFile}
          className={`mt-1 block w-full text-sm ${FOCUS}`}
        />
      </div>
      {uploading && <p className="mt-2 text-slate-600 dark:text-slate-400">Đang tải ảnh lên...</p>}
      {messages.length > 0 && (
        <div className="mt-3">
          <ErrorAlert messages={messages} />
        </div>
      )}
      {notice !== null && (
        <p role="status" className={SUCCESS_BOX}>
          {notice}
        </p>
      )}
      {list}
    </section>
  )
}

function LessonForm({ courses, lesson, flash }: { courses: AdminCourse[]; lesson: AdminLesson | null; flash: string | null }) {
  const navigate = useNavigate()
  const [values, setValues] = useState(lesson ? toValues(lesson) : EMPTY_VALUES)
  const [lessonStatus, setStatus] = useState<LessonStatus | null>(lesson?.status ?? null)
  const [preview, setPreview] = useState(false)
  const [busy, setBusy] = useState(false)
  const [statusBusy, setStatusBusy] = useState(false)
  const [messages, setMessages] = useState<string[]>([])
  const [saved, setSaved] = useState<string | null>(flash)

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
    setSaved(null)
    try {
      if (lesson === null) {
        const created = await createLesson(parsed.input)
        navigate(`${LIST_PATH}/${created.id}/sua`, {
          state: { flash: 'Đã tạo bài học (đang là Nháp). Bạn có thể thêm ảnh và đăng bài.' },
        })
        return
      }
      await updateLesson(lesson.id, parsed.input)
      setSaved('Đã lưu bài học.')
    } catch (error) {
      setMessages(errorMessages(error, 'Không lưu được bài học. Hãy thử lại.'))
    }
    setBusy(false)
  }

  async function changeStatus(next: LessonStatus) {
    if (lesson === null || statusBusy) return
    setStatusBusy(true)
    setMessages([])
    try {
      const updated = await setLessonStatus(lesson.id, next)
      setStatus(updated.status)
    } catch (error) {
      setMessages(errorMessages(error, 'Không đổi được trạng thái bài. Hãy thử lại.'))
    }
    setStatusBusy(false)
  }

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{lesson ? 'Sửa bài học' : 'Thêm bài học'}</h1>
      <p className="mt-3">
        <Link to={LIST_PATH} className={LINK}>
          Danh sách bài học
        </Link>
      </p>

      {saved !== null && (
        <p role="status" className={SUCCESS_BOX}>
          {saved}
        </p>
      )}

      {lessonStatus !== null && (
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <span>
            Trạng thái: <strong>{STATUS_LABEL[lessonStatus]}</strong>
          </span>
          {lessonStatus === 'DRAFT' ? (
            <button type="button" disabled={statusBusy} onClick={() => changeStatus('PUBLISHED')} className={SECONDARY}>
              Đăng bài
            </button>
          ) : (
            <button type="button" disabled={statusBusy} onClick={() => changeStatus('DRAFT')} className={SECONDARY}>
              Ẩn bài
            </button>
          )}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate className="mt-6 space-y-4">
        <div>
          <label htmlFor="lesson-course" className="block text-sm font-medium">
            Khóa học
          </label>
          <select
            id="lesson-course"
            value={values.courseId}
            onChange={(event) => set('courseId')(event.target.value)}
            className={FIELD}
          >
            <option value="">Chọn khóa học</option>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.title}
              </option>
            ))}
          </select>
          {courses.length === 0 && (
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
              Chưa có khóa học nào.{' '}
              <Link to="/quan-tri/khoa-hoc" className={LINK}>
                Tạo khóa học trước
              </Link>
              .
            </p>
          )}
        </div>
        <TextField
          id="lesson-slug"
          label="Slug"
          value={values.slug}
          onChange={set('slug')}
          hint="Chữ thường không dấu, số và dấu gạch ngang. Slug là duy nhất trên toàn bộ bài học."
        />
        <TextField id="lesson-title" label="Tên bài học" value={values.title} onChange={set('title')} />
        <TextField
          id="lesson-summary"
          label="Tóm tắt (không bắt buộc)"
          value={values.summary}
          onChange={set('summary')}
        />
        <TextField
          id="lesson-sort"
          label="Thứ tự hiển thị"
          value={values.sortOrder}
          onChange={set('sortOrder')}
          hint="Số nhỏ hiện trước trong khóa."
        />

        <div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-sm font-medium">Nội dung</span>
            {preview ? (
              <button type="button" onClick={() => setPreview(false)} className={SECONDARY}>
                Soạn
              </button>
            ) : (
              <button type="button" onClick={() => setPreview(true)} className={SECONDARY}>
                Xem trước
              </button>
            )}
          </div>
          {preview ? (
            <section aria-label="Xem trước nội dung" className={`mt-2 ${CARD}`}>
              {values.contentMd.trim() === '' ? (
                <p className="text-slate-600 dark:text-slate-400">Chưa có nội dung.</p>
              ) : (
                <MarkdownContent source={values.contentMd} />
              )}
            </section>
          ) : (
            <>
              <label htmlFor="lesson-content" className="sr-only">
                Nội dung (Markdown)
              </label>
              <textarea
                id="lesson-content"
                rows={18}
                value={values.contentMd}
                onChange={(event) => set('contentMd')(event.target.value)}
                className={`${FIELD} font-mono text-sm`}
              />
            </>
          )}
        </div>

        <ErrorAlert messages={messages} />
        <SubmitButton pending={busy} label="Lưu bài" pendingLabel="Đang lưu..." />
      </form>

      {lesson !== null ? (
        <ImagePanel
          lessonId={lesson.id}
          onInsert={(snippet) => {
            set('contentMd')(appendSnippet(values.contentMd, snippet))
            setPreview(false)
          }}
        />
      ) : (
        <p className="mt-8 text-slate-600 dark:text-slate-400">
          Lưu bài trước, rồi mới thêm được ảnh cho bài này.
        </p>
      )}
    </>
  )
}

function LessonLoader({ id, flash }: { id: number | null; flash: string | null }) {
  const { state, reload } = useLoad(async () => {
    const [courses, lesson] = await Promise.all([fetchAdminCourses(), id === null ? null : fetchAdminLesson(id)])
    return { courses, lesson }
  }, `lesson:${id}`)

  if (state.kind === 'loading') {
    return <p className="text-slate-600 dark:text-slate-400">Đang tải...</p>
  }
  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 403) {
      return (
        <p role="alert" className="font-medium text-red-700 dark:text-red-400">
          {NO_PERMISSION}
        </p>
      )
    }
    if (state.error instanceof ApiError && state.error.status === 404) {
      return <NotFound />
    }
    return <LoadError message="Không tải được bài học." onRetry={reload} />
  }
  return <LessonForm courses={state.data.courses} lesson={state.data.lesson} flash={flash} />
}

function NotFound() {
  return (
    <>
      <p className="text-lg">Không tìm thấy bài học.</p>
      <p className="mt-3">
        <Link to={LIST_PATH} className={LINK}>
          Về danh sách bài học
        </Link>
      </p>
    </>
  )
}

function LessonEditor() {
  const { lessonId } = useParams()
  const location = useLocation()
  const flash = (location.state as { flash?: string } | null)?.flash ?? null

  if (lessonId === undefined) return <LessonLoader id={null} flash={flash} />
  if (!/^\d{1,9}$/.test(lessonId)) return <NotFound />
  return <LessonLoader id={Number(lessonId)} flash={flash} />
}

export default function AdminLessonEditPage() {
  return (
    <AdminGate>
      <LessonEditor />
    </AdminGate>
  )
}
