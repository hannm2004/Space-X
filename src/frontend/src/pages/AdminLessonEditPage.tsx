import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import {
  createLesson,
  clearImageCover,
  deleteImage,
  setImageCover,
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
import { CARD, DANGER, NO_PERMISSION, SECONDARY, SUCCESS_BOX } from '../adminUi'
import { ApiError } from '../client'
import FilePickButton from '../FilePickButton'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import { IMAGE_ACCEPT, IMAGE_HINT, imageProblem } from '../imageUpload'
import LoadError from '../LoadError'
import MarkdownContent from '../MarkdownContent'
import { useLoad } from '../useLoad'

const LIST_PATH = '/quan-tri/bai-hoc'
const MAX_CONTENT_LENGTH = 100000
const FIELD = `mt-1 field`
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

function ImageItem({ image, onInsert, onChanged }: { image: LessonImage; onInsert: () => void; onChanged: () => void }) {
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [messages, setMessages] = useState<string[]>([])

  async function handleDelete() {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      await deleteImage(image.id)
      onChanged()
    } catch (error) {
      setMessages(errorMessages(error, 'Không xóa được ảnh. Hãy thử lại.'))
    }
    setBusy(false)
  }

  // Mỗi bài tối đa một ảnh bìa: đặt ảnh này thì máy chủ tự bỏ ảnh bìa cũ. Ảnh bìa hiện ở danh sách bài, kết quả tìm kiếm
  // và đầu trang bài của người học.
  async function handleCover(makeCover: boolean) {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      await (makeCover ? setImageCover(image.id) : clearImageCover(image.id))
      onChanged()
    } catch {
      setMessages([makeCover ? 'Không đặt được ảnh bìa. Hãy thử lại.' : 'Không bỏ được ảnh bìa. Hãy thử lại.'])
    }
    setBusy(false)
  }

  return (
    <li className={CARD}>
      <div className="flex items-center gap-3">
        <img src={image.url} alt="" className="h-12 w-12 shrink-0 rounded object-cover" />
        <div className="min-w-0">
          <p className="break-all font-medium">
            {image.fileName}
            {image.cover && <span className="ml-2 border border-accent px-1.5 py-0.5 align-middle text-xs font-semibold text-accent">Ảnh bìa</span>}
          </p>
          <p className="text-sm text-muted">{formatSize(image.sizeBytes)}</p>
        </div>
      </div>
      {!confirming && (
        <div className="mt-3 flex flex-wrap gap-3">
          <button type="button" aria-label={`Chèn ảnh ${image.fileName} vào bài`} onClick={onInsert} className={SECONDARY}>
            Chèn vào bài
          </button>
          {image.cover ? (
            <button
              type="button"
              disabled={busy}
              aria-label={`Bỏ ảnh bìa của ${image.fileName}`}
              onClick={() => handleCover(false)}
              className={SECONDARY}
            >
              Bỏ ảnh bìa
            </button>
          ) : (
            <button
              type="button"
              disabled={busy}
              aria-label={`Đặt ảnh ${image.fileName} làm ảnh bìa`}
              onClick={() => handleCover(true)}
              className={SECONDARY}
            >
              Đặt làm ảnh bìa
            </button>
          )}
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
        <div className="mt-3 rounded-lg border border-line bg-paper p-3">
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
  const sectionRef = useRef<HTMLElement>(null)
  const { hash } = useLocation()

  // Liên kết "Ảnh" ở danh sách bài mở trang này kèm #anh-cua-bai: mục ảnh nằm cuối trang nên cuộn tới đó cho người dùng.
  // ImagePanel chỉ hiện khi bài đã tải xong, nên lúc nó gắn vào trang là lúc có chỗ để cuộn tới.
  useEffect(() => {
    if (hash === '#anh-cua-bai') sectionRef.current?.scrollIntoView?.({ block: 'start' })
  }, [hash])

  async function handleFile(file: File) {
    if (uploading) return
    setNotice(null)
    // Kiểm ở trình duyệt chỉ để báo sớm; máy chủ kiểm lại loại tệp thật và cỡ (415, 413).
    const problem = imageProblem(file)
    if (problem !== null) {
      setMessages([problem])
      return
    }
    setMessages([])
    setUploading(true)
    try {
      await uploadLessonImage(lessonId, file)
      setNotice('Đã tải ảnh. Bấm "Chèn vào bài" để thêm ảnh vào nội dung, hoặc "Đặt làm ảnh bìa" để ảnh hiện ở danh sách bài và kết quả tìm kiếm.')
      reload()
    } catch (error) {
      setMessages(errorMessages(error, 'Không tải được ảnh. Hãy thử lại.'))
    }
    setUploading(false)
  }

  let list
  if (state.kind === 'loading') {
    list = <p className="mt-3 text-muted">Đang tải ảnh...</p>
  } else if (state.kind === 'error') {
    list = <LoadError message="Không tải được danh sách ảnh." onRetry={reload} />
  } else if (state.data.length === 0) {
    list = <p className="mt-3">Bài này chưa có ảnh.</p>
  } else {
    list = (
      <ul className="mt-3 space-y-3">
        {state.data.map((image) => (
          <ImageItem key={image.id} image={image} onInsert={() => onInsert(imageSnippet(image))} onChanged={reload} />
        ))}
      </ul>
    )
  }

  return (
    <section ref={sectionRef} id="anh-cua-bai" aria-labelledby="images-heading" className="mt-8 scroll-mt-6">
      <h2 id="images-heading" className="text-xl font-semibold">
        Ảnh của bài
      </h2>
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2">
        <FilePickButton
          label="Chọn ảnh từ máy"
          inputLabel={`Tải ảnh lên (${IMAGE_HINT})`}
          accept={IMAGE_ACCEPT}
          disabled={uploading}
          onFile={handleFile}
        />
        <p className="text-sm text-muted">{IMAGE_HINT}.</p>
      </div>
      {uploading && <p className="mt-2 text-muted">Đang tải ảnh lên...</p>}
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
      <h1 className="text-4xl font-bold leading-tight tracking-tight">{lesson ? 'Sửa bài học' : 'Thêm bài học'}</h1>
      <p className="mt-3">
        <Link to={LIST_PATH} className={SECONDARY}>
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
            <p className="mt-1 text-sm text-muted">
              Chưa có khóa học nào.{' '}
              <Link to="/quan-tri/khoa-hoc" className={SECONDARY}>
                Tạo khóa học trước
              </Link>
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
                <p className="text-muted">Chưa có nội dung.</p>
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
        <p className="mt-8 text-muted">
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
    return <p className="text-muted">Đang tải...</p>
  }
  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 403) {
      return (
        <p role="alert" className="font-medium text-danger">
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
        <Link to={LIST_PATH} className={SECONDARY}>
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
