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
    return <p className="text-slate-600 dark:text-slate-400">Đang tải...</p>
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
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Đổi mật khẩu</h1>
      <form noValidate onSubmit={handleSubmit} className="mt-6 max-w-md space-y-4">
        {done && (
          <p
            role="status"
            className="rounded-lg border border-green-300 bg-green-50 px-3 py-2 text-green-900 dark:border-green-800 dark:bg-green-950 dark:text-green-200"
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
    </>
  )
}
