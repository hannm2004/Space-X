import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { register } from '../authApi'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import GoogleSignIn from '../GoogleSignIn'
import { useAuth } from '../useAuth'

export default function RegisterPage() {
  const { state } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [messages, setMessages] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)

  if (state.status === 'loading') {
    return <p className="text-muted">Đang tải...</p>
  }
  if (state.status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting) return
    if (displayName.trim() === '' || email.trim() === '' || password === '' || confirmPassword === '') {
      setMessages(['Hãy nhập đầy đủ các ô.'])
      return
    }
    // Kiểm nhanh cho tiện; backend vẫn là nơi kiểm bắt buộc (docs/API.md).
    if (password !== confirmPassword) {
      setMessages(['Xác nhận mật khẩu không khớp.'])
      return
    }
    setMessages([])
    setSubmitting(true)
    try {
      await register({ displayName, email, password, confirmPassword })
      navigate('/dang-nhap', { state: { registered: true } })
    } catch (error) {
      setMessages(errorMessages(error, 'Không đăng ký được. Hãy kiểm tra lại thông tin.'))
      setPassword('')
      setConfirmPassword('')
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md border border-line bg-surface p-6 sm:p-8">
      <h1 className="font-serif text-4xl font-bold leading-tight tracking-tight">Đăng ký</h1>
      <form noValidate onSubmit={handleSubmit} className="mt-6 space-y-4">
        <ErrorAlert messages={messages} />
        <TextField
          id="displayName"
          label="Tên hiển thị"
          value={displayName}
          onChange={setDisplayName}
          autoComplete="name"
        />
        <TextField id="email" label="Email" type="email" value={email} onChange={setEmail} autoComplete="username" />
        <TextField
          id="password"
          label="Mật khẩu"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
          hint="Tối thiểu 8 ký tự."
        />
        <TextField
          id="confirmPassword"
          label="Nhập lại mật khẩu"
          type="password"
          value={confirmPassword}
          onChange={setConfirmPassword}
          autoComplete="new-password"
        />
        <SubmitButton pending={submitting} label="Đăng ký" pendingLabel="Đang đăng ký..." />
      </form>
      <GoogleSignIn label="Đăng ký bằng Google" />
      <p className="mt-6 text-muted">
        Đã có tài khoản?{' '}
        <Link to="/dang-nhap" className="link">
          Đăng nhập
        </Link>
      </p>
    </div>
  )
}
