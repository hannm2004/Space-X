import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser, register } from '../authApi'
import { ApiError } from '../client'
import LoginPage from './LoginPage'
import RegisterPage from './RegisterPage'

vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)
const registerMock = vi.mocked(register)

function renderRegister() {
  return render(
    <MemoryRouter initialEntries={['/dang-ky']}>
      <AuthProvider>
        <Routes>
          <Route path="/dang-ky" element={<RegisterPage />} />
          <Route path="/dang-nhap" element={<LoginPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function fill(values: { name?: string; email?: string; password?: string; confirm?: string }) {
  fireEvent.change(await screen.findByLabelText('Tên hiển thị'), { target: { value: values.name ?? '' } })
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: values.email ?? '' } })
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: values.password ?? '' } })
  fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu'), { target: { value: values.confirm ?? '' } })
}

const VALID = {
  name: 'Nguyễn Văn A',
  email: 'a@example.com',
  password: 'matkhau123',
  confirm: 'matkhau123',
}

beforeEach(() => {
  fetchCurrentUserMock.mockReset()
  registerMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(null)
})

describe('RegisterPage — đăng ký', () => {
  it('hiện 4 ô nhập, nút đăng ký và liên kết sang đăng nhập', async () => {
    renderRegister()

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng ký' })).not.toBeNull()
    expect(screen.getByLabelText('Mật khẩu').getAttribute('type')).toBe('password')
    expect(screen.getByLabelText('Nhập lại mật khẩu').getAttribute('type')).toBe('password')
    expect(screen.getByRole('link', { name: 'Đăng nhập' }).getAttribute('href')).toBe('/dang-nhap')
  })

  it('gửi đủ 4 trường rồi chuyển sang trang đăng nhập kèm lời nhắn (đăng ký không tự đăng nhập)', async () => {
    registerMock.mockResolvedValue(undefined)
    renderRegister()

    await fill(VALID)
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
    expect(screen.getByRole('status').textContent).toContain('Đăng ký thành công')
    expect(registerMock).toHaveBeenCalledWith({
      displayName: 'Nguyễn Văn A',
      email: 'a@example.com',
      password: 'matkhau123',
      confirmPassword: 'matkhau123',
    })
  })

  it('hai mật khẩu không khớp thì không gọi API và báo ngay', async () => {
    renderRegister()

    await fill({ ...VALID, confirm: 'matkhau-khac' })
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Xác nhận mật khẩu không khớp')
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('để trống ô nào đó thì không gọi API và nhắc nhập đủ', async () => {
    renderRegister()

    await fill({ ...VALID, name: '' })
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy nhập đầy đủ các ô')
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('email đã tồn tại (409) hiện thông báo của máy chủ và xóa hai ô mật khẩu', async () => {
    registerMock.mockRejectedValue(
      new ApiError(409, 'Email đã được đăng ký', { detail: 'Email đã được đăng ký' }),
    )
    renderRegister()

    await fill(VALID)
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Email đã được đăng ký')
    expect((screen.getByLabelText('Mật khẩu') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('Nhập lại mật khẩu') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe('a@example.com')
  })

  it('máy chủ trả lỗi theo trường (400) thì hiện từng câu lỗi', async () => {
    registerMock.mockRejectedValue(
      new ApiError(400, 'Dữ liệu không hợp lệ', {
        fieldErrors: [
          { field: 'email', message: 'Email không đúng định dạng' },
          { field: 'password', message: 'Mật khẩu tối thiểu 8 ký tự' },
        ],
      }),
    )
    renderRegister()

    await fill({ ...VALID, email: 'khong-phai-email' })
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Email không đúng định dạng')
    expect(alert.textContent).toContain('Mật khẩu tối thiểu 8 ký tự')
  })

  it('403 do thiếu CSRF hiện thông báo riêng bảo tải lại trang', async () => {
    registerMock.mockRejectedValue(new ApiError(403, 'Máy chủ trả mã lỗi 403'))
    renderRegister()

    await fill(VALID)
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy tải lại trang')
  })

  it('trong lúc chờ máy chủ thì nút bị vô hiệu để không gửi hai lần', async () => {
    registerMock.mockReturnValue(new Promise(() => {}))
    renderRegister()

    await fill(VALID)
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))

    const button = await screen.findByRole('button', { name: 'Đang đăng ký...' })
    expect((button as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(button)
    expect(registerMock).toHaveBeenCalledTimes(1)
  })
})
