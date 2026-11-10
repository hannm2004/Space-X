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

export default function LoginPage() {
  const { state, signIn } = useAuth()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [messages, setMessages] = useState<string[]>([])
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
      <p className="mt-6 text-slate-600 dark:text-slate-400">
        Chưa có tài khoản?{' '}
        <Link to="/dang-ky" className="text-blue-700 underline dark:text-blue-400">
          Đăng ký
        </Link>
      </p>
    </>
  )
}
