import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { readRedirectPath } from '../authRedirect'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
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

// Chữ "G" bốn màu theo bộ nhận diện của Google (đường vẽ SVG chính thức, giữ nguyên màu và tỉ lệ, không đổi màu theo
// chế độ tối). aria-hidden vì chữ bên cạnh đã nói rõ nút làm gì; trình đọc màn hình không đọc hình trang trí.
function GoogleLogo() {
  return (
    <svg viewBox="0 0 48 48" width="18" height="18" aria-hidden="true" focusable="false">
      <path
        fill="#EA4335"
        d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
      />
      <path
        fill="#4285F4"
        d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
      />
      <path
        fill="#FBBC05"
        d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
      />
      <path
        fill="#34A853"
        d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
      />
    </svg>
  )
}

export default function LoginPage() {
  const { state, signIn } = useAuth()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [messages, setMessages] = useState<string[]>(() => googleErrorMessages(location.search))
  const [submitting, setSubmitting] = useState(false)

  if (state.status === 'loading') {
    return <p className="text-slate-600 dark:text-slate-400">Đang tải...</p>
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
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Đăng nhập</h1>
      {justRegistered(location.state) && (
        <p
          role="status"
          className="mt-4 rounded-lg border border-green-300 bg-green-50 px-3 py-2 text-green-900 dark:border-green-800 dark:bg-green-950 dark:text-green-200"
        >
          Đăng ký thành công. Hãy đăng nhập để bắt đầu học.
        </p>
      )}
      <form noValidate onSubmit={handleSubmit} className="mt-6 max-w-md space-y-4">
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
      <div className="mt-4 max-w-md">
        <p className="mb-4 text-center text-sm text-slate-500 dark:text-slate-400">hoặc</p>
        {/* Thẻ <a> thường, không dùng <Link>: đây là yêu cầu GET chuyển hướng sang Google do máy chủ xử lý, phải tải trang đầy đủ. */}
        <a
          href="/oauth2/authorization/google"
          className="inline-flex w-full items-center justify-center gap-3 rounded-lg border border-slate-300 px-4 py-2 font-medium hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-800"
        >
          <GoogleLogo />
          Đăng nhập bằng Google
        </a>
      </div>
      <p className="mt-6 text-slate-600 dark:text-slate-400">
        Chưa có tài khoản?{' '}
        <Link to="/dang-ky" className="text-blue-700 underline dark:text-blue-400">
          Đăng ký
        </Link>
      </p>
    </>
  )
}
