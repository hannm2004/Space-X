import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../AuthContext'
import { changePassword, fetchCurrentUser } from '../authApi'
import { ApiError } from '../client'
import ChangePasswordPage from './ChangePasswordPage'

vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)
const changePasswordMock = vi.mocked(changePassword)

const USER = { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' as const }

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/doi-mat-khau']}>
      <AuthProvider>
        <Routes>
          <Route path="/doi-mat-khau" element={<ChangePasswordPage />} />
          <Route path="/dang-nhap" element={<LoginStub />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function fill(current: string, next: string, confirm: string) {
  fireEvent.change(await screen.findByLabelText('Mật khẩu hiện tại'), { target: { value: current } })
  fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: next } })
  fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), { target: { value: confirm } })
}

beforeEach(() => {
  fetchCurrentUserMock.mockReset()
  changePasswordMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(USER)
})

describe('ChangePasswordPage — đổi mật khẩu', () => {
  it('chưa đăng nhập thì chuyển sang trang đăng nhập và nhớ để quay lại', async () => {
    fetchCurrentUserMock.mockResolvedValue(null)

    renderPage()

    expect(await screen.findByText('Trang đăng nhập, quay lại /doi-mat-khau')).not.toBeNull()
    expect(changePasswordMock).not.toHaveBeenCalled()
  })

  it('đã đăng nhập thì hiện 3 ô mật khẩu che ký tự và nút đổi mật khẩu', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { level: 1, name: 'Đổi mật khẩu' })).not.toBeNull()
    for (const label of ['Mật khẩu hiện tại', 'Mật khẩu mới', 'Nhập lại mật khẩu mới']) {
      expect(screen.getByLabelText(label).getAttribute('type')).toBe('password')
    }
    expect(screen.getByRole('button', { name: 'Đổi mật khẩu' })).not.toBeNull()
  })

  it('đổi thành công thì báo các thiết bị khác đã bị đăng xuất và xóa cả 3 ô', async () => {
    changePasswordMock.mockResolvedValue(undefined)
    renderPage()

    await fill('matkhau-cu-1', 'matkhau-moi-2', 'matkhau-moi-2')
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    const status = await screen.findByRole('status')
    expect(status.textContent).toContain('Đã đổi mật khẩu')
    expect(status.textContent).toContain('các thiết bị khác đã bị đăng xuất')
    expect(changePasswordMock).toHaveBeenCalledWith({
      currentPassword: 'matkhau-cu-1',
      newPassword: 'matkhau-moi-2',
      confirmNewPassword: 'matkhau-moi-2',
    })
    expect((screen.getByLabelText('Mật khẩu hiện tại') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('Mật khẩu mới') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('Nhập lại mật khẩu mới') as HTMLInputElement).value).toBe('')
  })

  it('mật khẩu mới và ô nhập lại không khớp thì không gọi API', async () => {
    renderPage()

    await fill('matkhau-cu-1', 'matkhau-moi-2', 'matkhau-khac-3')
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Xác nhận mật khẩu mới không khớp')
    expect(changePasswordMock).not.toHaveBeenCalled()
  })

  it('để trống thì không gọi API và nhắc nhập đủ', async () => {
    renderPage()

    await fill('', 'matkhau-moi-2', 'matkhau-moi-2')
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy nhập đầy đủ các ô')
    expect(changePasswordMock).not.toHaveBeenCalled()
  })

  it('sai mật khẩu hiện tại (400) hiện thông báo của máy chủ, không lộ mật khẩu và xóa các ô', async () => {
    changePasswordMock.mockRejectedValue(
      new ApiError(400, 'Mật khẩu hiện tại không đúng', { detail: 'Mật khẩu hiện tại không đúng' }),
    )
    renderPage()

    await fill('sai-mat-khau-cu', 'matkhau-moi-2', 'matkhau-moi-2')
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Mật khẩu hiện tại không đúng')
    expect(alert.textContent).not.toContain('sai-mat-khau-cu')
    expect((screen.getByLabelText('Mật khẩu hiện tại') as HTMLInputElement).value).toBe('')
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('máy chủ trả lỗi theo trường (mật khẩu mới trùng mật khẩu cũ) thì hiện câu lỗi', async () => {
    changePasswordMock.mockRejectedValue(
      new ApiError(400, 'Dữ liệu không hợp lệ', {
        fieldErrors: [
          { field: 'newPasswordDifferentFromCurrent', message: 'Mật khẩu mới phải khác mật khẩu hiện tại' },
        ],
      }),
    )
    renderPage()

    await fill('matkhau-cu-1', 'matkhau-cu-1', 'matkhau-cu-1')
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Mật khẩu mới phải khác mật khẩu hiện tại')
  })

  it('403 do thiếu CSRF hiện thông báo riêng bảo tải lại trang', async () => {
    changePasswordMock.mockRejectedValue(new ApiError(403, 'Máy chủ trả mã lỗi 403'))
    renderPage()

    await fill('matkhau-cu-1', 'matkhau-moi-2', 'matkhau-moi-2')
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy tải lại trang')
  })

  it('trong lúc chờ máy chủ thì nút bị vô hiệu để không gửi hai lần', async () => {
    changePasswordMock.mockReturnValue(new Promise(() => {}))
    renderPage()

    await fill('matkhau-cu-1', 'matkhau-moi-2', 'matkhau-moi-2')
    fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))

    const button = await screen.findByRole('button', { name: 'Đang đổi mật khẩu...' })
    expect((button as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(button)
    expect(changePasswordMock).toHaveBeenCalledTimes(1)
  })
})
