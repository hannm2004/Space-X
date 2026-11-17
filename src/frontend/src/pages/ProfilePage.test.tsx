import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser, fetchProfile, updateDisplayName, type Profile } from '../authApi'
import { ApiError } from '../client'
import { useAuth } from '../useAuth'
import ProfilePage from './ProfilePage'

vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
  fetchProfile: vi.fn(),
  updateDisplayName: vi.fn(),
}))

const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)
const fetchProfileMock = vi.mocked(fetchProfile)
const updateDisplayNameMock = vi.mocked(updateDisplayName)

const USER = { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' as const }
const PROFILE: Profile = { ...USER, authProvider: 'LOCAL', createdAt: '2026-11-01T09:30:00' }

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

// Tên người dùng mà thanh đầu trang đọc từ AuthContext; dùng để kiểm sau khi lưu tên thì cả ứng dụng thấy tên mới.
function HeaderName() {
  const { state } = useAuth()
  return <p data-testid="header-name">{state.status === 'authenticated' ? state.user.displayName : ''}</p>
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/ho-so']}>
      <AuthProvider>
        <HeaderName />
        <Routes>
          <Route path="/ho-so" element={<ProfilePage />} />
          <Route path="/dang-nhap" element={<LoginStub />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function nameInput() {
  return (await screen.findByLabelText('Tên hiển thị')) as HTMLInputElement
}

beforeEach(() => {
  fetchCurrentUserMock.mockReset()
  fetchProfileMock.mockReset()
  updateDisplayNameMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(USER)
  fetchProfileMock.mockResolvedValue(PROFILE)
})

describe('ProfilePage — xem hồ sơ', () => {
  it('chưa đăng nhập thì chuyển sang trang đăng nhập, nhớ để quay lại và không tải hồ sơ', async () => {
    fetchCurrentUserMock.mockResolvedValue(null)

    renderPage()

    expect(await screen.findByText('Trang đăng nhập, quay lại /ho-so')).not.toBeNull()
    expect(fetchProfileMock).not.toHaveBeenCalled()
  })

  it('hiện email, vai trò, cách đăng nhập và ngày tham gia (ngày/tháng/năm)', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { level: 1, name: 'Hồ sơ cá nhân' })).not.toBeNull()
    expect(await screen.findByText('a@example.com')).not.toBeNull()
    expect(screen.getByText('Học sinh')).not.toBeNull()
    expect(screen.getByText('Email và mật khẩu')).not.toBeNull()
    expect(screen.getByText('01/11/2026')).not.toBeNull()
  })

  it('Admin được ghi là Quản trị viên', async () => {
    fetchProfileMock.mockResolvedValue({ ...PROFILE, role: 'ADMIN' })

    renderPage()

    expect(await screen.findByText('Quản trị viên')).not.toBeNull()
  })

  it('email chỉ để xem: không có ô nhập email', async () => {
    renderPage()

    await screen.findByText('a@example.com')
    expect(screen.queryByLabelText('Email')).toBeNull()
  })

  it('ô tên hiển thị có sẵn tên hiện tại', async () => {
    renderPage()

    expect((await nameInput()).value).toBe('Nguyễn Văn A')
  })

  it('không tải được hồ sơ thì báo lỗi và có nút Thử lại tải lại được', async () => {
    fetchProfileMock.mockRejectedValueOnce(new ApiError(0, 'Không kết nối được tới máy chủ'))
    renderPage()

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Không tải được hồ sơ')

    fireEvent.click(within(alert).getByRole('button', { name: 'Thử lại' }))

    expect(await nameInput()).not.toBeNull()
    expect(fetchProfileMock).toHaveBeenCalledTimes(2)
  })
})

describe('ProfilePage — mật khẩu theo cách đăng nhập', () => {
  it('tài khoản mật khẩu có liên kết sang trang đổi mật khẩu', async () => {
    renderPage()

    const link = await screen.findByRole('link', { name: 'Đổi mật khẩu' })
    expect(link.getAttribute('href')).toBe('/doi-mat-khau')
  })

  it('tài khoản Google không có liên kết đổi mật khẩu, thay bằng lời giải thích', async () => {
    fetchProfileMock.mockResolvedValue({ ...PROFILE, authProvider: 'GOOGLE' })

    renderPage()

    expect(await screen.findByText('Google')).not.toBeNull()
    expect(screen.getByText(/không có mật khẩu trên Space-X/)).not.toBeNull()
    expect(screen.queryByRole('link', { name: 'Đổi mật khẩu' })).toBeNull()
  })
})

describe('ProfilePage — sửa tên hiển thị', () => {
  it('lưu thành công: gửi tên đã cắt khoảng trắng, báo đã lưu và cập nhật tên ở thanh đầu trang', async () => {
    updateDisplayNameMock.mockResolvedValue({ ...PROFILE, displayName: 'Trần Thị B' })
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: '  Trần Thị B  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect((await screen.findByRole('status')).textContent).toContain('Đã lưu tên hiển thị')
    expect(updateDisplayNameMock).toHaveBeenCalledWith('Trần Thị B')
    expect(screen.getByTestId('header-name').textContent).toBe('Trần Thị B')
    expect((await nameInput()).value).toBe('Trần Thị B')
  })

  it('tên để trống hoặc chỉ có khoảng trắng thì không gọi API và nhắc nhập tên', async () => {
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: '    ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy nhập tên hiển thị')
    expect(updateDisplayNameMock).not.toHaveBeenCalled()
  })

  it('tên dài quá 100 ký tự thì không gọi API; đúng 100 ký tự thì được gửi', async () => {
    updateDisplayNameMock.mockResolvedValue({ ...PROFILE, displayName: 'a'.repeat(100) })
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: 'a'.repeat(101) } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect((await screen.findByRole('alert')).textContent).toContain('tối đa 100 ký tự')
    expect(updateDisplayNameMock).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText('Tên hiển thị'), { target: { value: 'a'.repeat(100) } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect(await screen.findByRole('status')).not.toBeNull()
    expect(updateDisplayNameMock).toHaveBeenCalledWith('a'.repeat(100))
  })

  it('máy chủ từ chối (400 theo trường) thì hiện câu lỗi, không báo đã lưu và tên ở thanh đầu trang giữ nguyên', async () => {
    updateDisplayNameMock.mockRejectedValue(
      new ApiError(400, 'Dữ liệu không hợp lệ', {
        fieldErrors: [{ field: 'displayName', message: 'Tên hiển thị không được để trống' }],
      }),
    )
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: 'Tên mới' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Tên hiển thị không được để trống')
    expect(screen.queryByRole('status')).toBeNull()
    expect(screen.getByTestId('header-name').textContent).toBe('Nguyễn Văn A')
  })

  it('mất kết nối thì báo không kết nối được và giữ tên người dùng đã nhập', async () => {
    updateDisplayNameMock.mockRejectedValue(new ApiError(0, 'Không kết nối được tới máy chủ'))
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: 'Tên mới' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Không kết nối được tới máy chủ')
    expect((screen.getByLabelText('Tên hiển thị') as HTMLInputElement).value).toBe('Tên mới')
  })

  it('403 do thiếu CSRF hiện thông báo riêng bảo tải lại trang', async () => {
    updateDisplayNameMock.mockRejectedValue(new ApiError(403, 'Máy chủ trả mã lỗi 403'))
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: 'Tên mới' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy tải lại trang')
  })

  it('sửa lại tên sau khi lưu thì ẩn thông báo "đã lưu" cũ', async () => {
    updateDisplayNameMock.mockResolvedValue({ ...PROFILE, displayName: 'Trần Thị B' })
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: 'Trần Thị B' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
    await screen.findByRole('status')

    fireEvent.change(screen.getByLabelText('Tên hiển thị'), { target: { value: 'Trần Thị BC' } })

    expect(screen.queryByRole('status')).toBeNull()
  })

  it('trong lúc chờ máy chủ thì nút bị vô hiệu để không gửi hai lần', async () => {
    updateDisplayNameMock.mockReturnValue(new Promise(() => {}))
    renderPage()

    fireEvent.change(await nameInput(), { target: { value: 'Tên mới' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    const button = await screen.findByRole('button', { name: 'Đang lưu...' })
    expect((button as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(button)
    expect(updateDisplayNameMock).toHaveBeenCalledTimes(1)
  })
})
