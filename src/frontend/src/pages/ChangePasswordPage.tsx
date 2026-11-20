import { useState, type FormEvent } from 'react'
import { Navigate, useLocation } from 'react-router'
import { changePassword } from '../authApi'
import { errorMessages } from '../formErrors'
import { ErrorAlert, SubmitButton, TextField } from '../FormParts'
import { useAuth } from '../useAuth'

export default function ChangePasswordPage() {
  const { state } = useAuth()
  const location = useLocation()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [messages, setMessages] = useState<string[]>([])
  const [done, setDone] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  if (state.status === 'loading') {
    return <p className="text-muted">Đang tải...</p>
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/dang-nhap" replace state={{ from: location.pathname }} />
  }

  function clearFields() {
    setCurrentPassword('')
    setNewPassword('')
    setConfirmNewPassword('')
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting) return
    setDone(false)
    if (currentPassword === '' || newPassword === '' || confirmNewPassword === '') {
      setMessages(['Hãy nhập đầy đủ các ô.'])
      return
    }
    // Kiểm nhanh cho tiện; backend vẫn là nơi kiểm bắt buộc (docs/API.md).
    if (newPassword !== confirmNewPassword) {
      setMessages(['Xác nhận mật khẩu mới không khớp.'])
      return
    }
    setMessages([])
    setSubmitting(true)
    try {
      await changePassword({ currentPassword, newPassword, confirmNewPassword })
      setDone(true)
    } catch (error) {
      setMessages(errorMessages(error, 'Không đổi được mật khẩu. Hãy thử lại.'))
    }
    // Mật khẩu không nằm lại trong ô nhập dù thành công hay thất bại.
    clearFields()
    setSubmitting(false)
  }

  return (
    <div className="mx-auto max-w-md rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-8">
      <h1 className="text-4xl font-bold leading-tight tracking-tight">Đổi mật khẩu</h1>
      <form noValidate onSubmit={handleSubmit} className="mt-6 space-y-4">
        {done && (
          <p
            role="status"
            className="rounded-lg border border-success/40 bg-success-soft px-4 py-3 text-success"
          >
            Đã đổi mật khẩu thành công; các thiết bị khác đã bị đăng xuất.
          </p>
        )}
        <ErrorAlert messages={messages} />
        {/* Ô username ẩn để trình quản lý mật khẩu biết mật khẩu mới thuộc tài khoản nào. */}
        <input type="text" hidden readOnly autoComplete="username" value={state.user.email} />
        <TextField
          id="currentPassword"
          label="Mật khẩu hiện tại"
          type="password"
          value={currentPassword}
          onChange={setCurrentPassword}
          autoComplete="current-password"
        />
        <TextField
          id="newPassword"
          label="Mật khẩu mới"
          type="password"
          value={newPassword}
          onChange={setNewPassword}
          autoComplete="new-password"
          hint="Tối thiểu 8 ký tự và phải khác mật khẩu hiện tại."
        />
        <TextField
          id="confirmNewPassword"
          label="Nhập lại mật khẩu mới"
          type="password"
          value={confirmNewPassword}
          onChange={setConfirmNewPassword}
          autoComplete="new-password"
        />
        <SubmitButton pending={submitting} label="Đổi mật khẩu" pendingLabel="Đang đổi mật khẩu..." />
      </form>
    </div>
  )
}
