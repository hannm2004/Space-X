import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { readRedirectPath } from '../authRedirect'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import GoogleSignIn from '../GoogleSignIn'
import { useAuth } from '../useAuth'

// Trang đăng ký gửi `registered: true` để trang này hiện lời nhắn (đăng ký không tự đăng nhập, D24).
function justRegistered(state: unknown): boolean {
  return typeof state === 'object' && state !== null && (state as Record<string, unknown>).registered === true
}

// Sau khi đăng nhập Google, máy chủ chuyển về /dang-nhap?loi=<mã> (docs/API.md). Chỉ nhận mã có trong bảng này: giá trị
// lạ trên địa chỉ không bao giờ được chép lên màn hình.
const GOOGLE_ERRORS = new Map([
  ['google-huy', 'Bạn đã hủy đăng nhập bằng Google.'],
  ['google-chua-xac-minh', 'Email Google của bạn chưa được xác minh.'],
  ['google-email-mat-khau', 'Email này đã đăng ký bằng mật khẩu. Hãy đăng nhập bằng email và mật khẩu.'],
  ['google-bi-khoa', 'Tài khoản này đã bị khóa.'],
  ['google-that-bai', 'Không đăng nhập được bằng Google. Hãy thử lại.'],
])

function googleErrorMessages(search: string): string[] {
  const code = new URLSearchParams(search).get('loi')
  const message = code === null ? undefined : GOOGLE_ERRORS.get(code)
  return message === undefined ? [] : [message]
}

export default function LoginPage() {
  const { state, signIn } = useAuth()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [messages, setMessages] = useState<string[]>(() => googleErrorMessages(location.search))
  const [submitting, setSubmitting] = useState(false)

  if (state.status === 'loading') {
    return <p className="text-muted">Đang tải...</p>
  }
  // Đã đăng nhập (kể cả vừa đăng nhập xong) thì không cần form: quay lại trang đang đọc dở, hoặc về trang chủ.
  if (state.status === 'authenticated') {
    return <Navigate to={readRedirectPath(location.state) ?? '/'} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting) return
    if (email.trim() === '' || password === '') {
      setMessages(['Hãy nhập email và mật khẩu.'])
      return
    }
    setMessages([])
    setSubmitting(true)
    try {
      await signIn(email, password)
    } catch (error) {
      setMessages(errorMessages(error, 'Không đăng nhập được. Hãy thử lại.'))
      setPassword('')
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-8">
      <h1 className="text-4xl font-bold leading-tight tracking-tight">Đăng nhập</h1>
      {justRegistered(location.state) && (
        <p
          role="status"
          className="mt-4 rounded-lg border border-success/40 bg-success-soft px-4 py-3 text-success"
        >
          Đăng ký thành công. Hãy đăng nhập để bắt đầu học.
        </p>
      )}
      <form noValidate onSubmit={handleSubmit} className="mt-6 space-y-4">
        <ErrorAlert messages={messages} />
        <TextField id="email" label="Email" type="email" value={email} onChange={setEmail} autoComplete="username" />
        <TextField
          id="password"
          label="Mật khẩu"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
        />
        <SubmitButton pending={submitting} label="Đăng nhập" pendingLabel="Đang đăng nhập..." />
      </form>
      <GoogleSignIn label="Đăng nhập bằng Google" />
      <p className="mt-6 text-muted">
        Chưa có tài khoản?{' '}
        <Link to="/dang-ky" className="link">
          Đăng ký
        </Link>
      </p>
    </div>
  )
}
