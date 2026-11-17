import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { fetchProfile, updateDisplayName, type LoginMethod, type Profile, type Role } from '../authApi'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import LoadError from '../LoadError'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'

const MAX_NAME_LENGTH = 100

const ROLE_LABEL: Record<Role, string> = { LEARNER: 'Học sinh', ADMIN: 'Quản trị viên' }
const METHOD_LABEL: Record<LoginMethod, string> = { LOCAL: 'Email và mật khẩu', GOOGLE: 'Google' }

const LINK =
  'rounded text-blue-700 underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:text-blue-400'

// Cắt thẳng từ chuỗi ISO của máy chủ (yyyy-MM-dd...) để không phụ thuộc múi giờ hay ICU của trình duyệt.
function formatDate(iso: string): string | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : null
}

function ProfileDetails({ profile }: { profile: Profile }) {
  const { updateUser } = useAuth()
  const [name, setName] = useState(profile.displayName)
  const [messages, setMessages] = useState<string[]>([])
  const [saved, setSaved] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const joined = formatDate(profile.createdAt)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting) return
    setSaved(false)
    const trimmed = name.trim()
    // Kiểm nhanh cho tiện; backend vẫn là nơi kiểm bắt buộc (docs/API.md).
    if (trimmed === '') {
      setMessages(['Hãy nhập tên hiển thị.'])
      return
    }
    if (trimmed.length > MAX_NAME_LENGTH) {
      setMessages([`Tên hiển thị tối đa ${MAX_NAME_LENGTH} ký tự.`])
      return
    }
    setMessages([])
    setSubmitting(true)
    try {
      const updated = await updateDisplayName(trimmed)
      // Dùng giá trị máy chủ trả về (đã chuẩn hóa) làm nguồn sự thật cho cả ô nhập lẫn thanh đầu trang.
      updateUser({ displayName: updated.displayName, email: updated.email, role: updated.role })
      setName(updated.displayName)
      setSaved(true)
    } catch (error) {
      setMessages(errorMessages(error, 'Không lưu được tên hiển thị. Hãy thử lại.'))
    }
    setSubmitting(false)
  }

  return (
    <>
      <dl className="mt-6 grid max-w-md grid-cols-[auto_1fr] gap-x-6 gap-y-2">
        <dt className="text-slate-600 dark:text-slate-400">Email</dt>
        <dd className="break-all">{profile.email}</dd>
        <dt className="text-slate-600 dark:text-slate-400">Vai trò</dt>
        <dd>{ROLE_LABEL[profile.role]}</dd>
        <dt className="text-slate-600 dark:text-slate-400">Đăng nhập bằng</dt>
        <dd>{METHOD_LABEL[profile.authProvider]}</dd>
        {joined && (
          <>
            <dt className="text-slate-600 dark:text-slate-400">Ngày tham gia</dt>
            <dd>{joined}</dd>
          </>
        )}
      </dl>

      <form noValidate onSubmit={handleSubmit} className="mt-8 max-w-md space-y-4">
        {saved && (
          <p
            role="status"
            className="rounded-lg border border-green-300 bg-green-50 px-3 py-2 text-green-900 dark:border-green-800 dark:bg-green-950 dark:text-green-200"
          >
            Đã lưu tên hiển thị.
          </p>
        )}
        <ErrorAlert messages={messages} />
        <TextField
          id="displayName"
          label="Tên hiển thị"
          value={name}
          onChange={(value) => {
            setName(value)
            setSaved(false)
          }}
          autoComplete="name"
          hint={`Tối đa ${MAX_NAME_LENGTH} ký tự.`}
        />
        <SubmitButton pending={submitting} label="Lưu thay đổi" pendingLabel="Đang lưu..." />
      </form>

      <section className="mt-8 max-w-md">
        <h2 className="text-xl font-semibold">Mật khẩu</h2>
        {profile.authProvider === 'LOCAL' ? (
          <p className="mt-2">
            <Link to="/doi-mat-khau" className={LINK}>
              Đổi mật khẩu
            </Link>
          </p>
        ) : (
          <p className="mt-2 text-slate-600 dark:text-slate-400">
            Tài khoản này đăng nhập bằng Google nên không có mật khẩu trên Space-X.
          </p>
        )}
      </section>
    </>
  )
}

function ProfileContent() {
  const { state, reload } = useLoad(fetchProfile, 'profile')

  if (state.kind === 'loading') {
    return <p className="mt-6 text-slate-600 dark:text-slate-400">Đang tải...</p>
  }
  if (state.kind === 'error') {
    return <LoadError message="Không tải được hồ sơ." onRetry={reload} />
  }
  return <ProfileDetails profile={state.data} />
}

export default function ProfilePage() {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'loading') {
    return <p className="text-slate-600 dark:text-slate-400">Đang tải...</p>
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/dang-nhap" replace state={{ from: location.pathname }} />
  }

  // Tách ProfileContent để chỉ tải hồ sơ khi đã chắc chắn đăng nhập (khách bị chuyển đi mà không gọi API).
  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Hồ sơ cá nhân</h1>
      <ProfileContent />
    </>
  )
}
