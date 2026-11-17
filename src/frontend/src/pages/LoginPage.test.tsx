import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser, login } from '../authApi'
import { ApiError } from '../client'
import LoginPage from './LoginPage'

vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)
const loginMock = vi.mocked(login)

const USER = { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' as const }

function Where() {
  return <p>Đang ở {useLocation().pathname}</p>
}

function renderLogin(entry: string | { pathname: string; state: unknown } = '/dang-nhap') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <AuthProvider>
        <Routes>
          <Route path="/dang-nhap" element={<LoginPage />} />
          <Route path="*" element={<Where />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function fillAndSubmit(email: string, password: string) {
  fireEvent.change(await screen.findByLabelText('Email'), { target: { value: email } })
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))
}

beforeEach(() => {
  fetchCurrentUserMock.mockReset()
  loginMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(null)
})

describe('LoginPage — đăng nhập', () => {
  it('hiện tiêu đề, ô email, ô mật khẩu (che ký tự), nút đăng nhập và liên kết sang đăng ký', async () => {
    renderLogin()

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
    expect(screen.getByLabelText('Email').getAttribute('type')).toBe('email')
    expect(screen.getByLabelText('Mật khẩu').getAttribute('type')).toBe('password')
    expect(screen.getByRole('link', { name: 'Đăng ký' }).getAttribute('href')).toBe('/dang-ky')
  })

  it('gửi đúng email và mật khẩu rồi về trang chủ khi thành công', async () => {
    loginMock.mockResolvedValue(USER)
    renderLogin()

    await fillAndSubmit('a@example.com', 'matkhau123')

    expect(await screen.findByText('Đang ở /')).not.toBeNull()
    expect(loginMock).toHaveBeenCalledWith('a@example.com', 'matkhau123')
  })

  it('quay lại trang đang đọc dở nếu có state.from hợp lệ', async () => {
    loginMock.mockResolvedValue(USER)
    renderLogin({ pathname: '/dang-nhap', state: { from: '/bai/thong-tin-va-du-lieu' } })

    await fillAndSubmit('a@example.com', 'matkhau123')

    expect(await screen.findByText('Đang ở /bai/thong-tin-va-du-lieu')).not.toBeNull()
  })

  it('state.from trỏ sang trang khác thì bỏ qua, về trang chủ', async () => {
    loginMock.mockResolvedValue(USER)
    renderLogin({ pathname: '/dang-nhap', state: { from: '//evil.example' } })

    await fillAndSubmit('a@example.com', 'matkhau123')

    expect(await screen.findByText('Đang ở /')).not.toBeNull()
  })

  it('đăng nhập sai (401) hiện thông báo của máy chủ, ở lại trang và xóa ô mật khẩu', async () => {
    loginMock.mockRejectedValue(
      new ApiError(401, 'Email hoặc mật khẩu không chính xác', {
        detail: 'Email hoặc mật khẩu không chính xác',
      }),
    )
    renderLogin()

    await fillAndSubmit('a@example.com', 'sai-mat-khau')

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Email hoặc mật khẩu không chính xác')
    expect(alert.textContent).not.toContain('sai-mat-khau')
    expect((screen.getByLabelText('Mật khẩu') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe('a@example.com')
  })

  it('403 do thiếu CSRF hiện thông báo riêng bảo tải lại trang', async () => {
    loginMock.mockRejectedValue(new ApiError(403, 'Máy chủ trả mã lỗi 403'))
    renderLogin()

    await fillAndSubmit('a@example.com', 'matkhau123')

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy tải lại trang')
  })

  it('mất kết nối thì báo kiểm tra mạng', async () => {
    loginMock.mockRejectedValue(new ApiError(0, 'Không kết nối được tới máy chủ'))
    renderLogin()

    await fillAndSubmit('a@example.com', 'matkhau123')

    expect((await screen.findByRole('alert')).textContent).toContain('Kiểm tra mạng')
  })

  it('để trống thì không gọi API và nhắc nhập đủ', async () => {
    renderLogin()

    await fillAndSubmit('', '')

    expect((await screen.findByRole('alert')).textContent).toContain('Hãy nhập email và mật khẩu')
    expect(loginMock).not.toHaveBeenCalled()
  })

  it('trong lúc chờ máy chủ thì nút bị vô hiệu để không gửi hai lần', async () => {
    loginMock.mockReturnValue(new Promise(() => {}))
    renderLogin()

    await fillAndSubmit('a@example.com', 'matkhau123')

    const button = await screen.findByRole('button', { name: 'Đang đăng nhập...' })
    expect((button as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(button)
    expect(loginMock).toHaveBeenCalledTimes(1)
  })

  it('sau khi đăng ký xong thì hiện lời nhắn thành công', async () => {
    renderLogin({ pathname: '/dang-nhap', state: { registered: true } })

    expect((await screen.findByRole('status')).textContent).toContain('Đăng ký thành công')
  })

  it('đã đăng nhập rồi thì không hiện form mà chuyển về trang chủ', async () => {
    fetchCurrentUserMock.mockResolvedValue(USER)

    renderLogin()

    expect(await screen.findByText('Đang ở /')).not.toBeNull()
    expect(screen.queryByLabelText('Email')).toBeNull()
  })
})

describe('LoginPage — đăng nhập bằng Google (UC03)', () => {
  it('có liên kết "Đăng nhập bằng Google" trỏ tới đường dẫn của máy chủ (tải trang đầy đủ, không qua React Router)', async () => {
    renderLogin()

    const link = await screen.findByRole('link', { name: 'Đăng nhập bằng Google' })
    expect(link.getAttribute('href')).toBe('/oauth2/authorization/google')
  })

  it('nút có logo Google, đánh dấu trang trí nên tên của liên kết vẫn chỉ là chữ', async () => {
    renderLogin()

    const link = await screen.findByRole('link', { name: 'Đăng nhập bằng Google' })
    const logo = link.querySelector('svg')
    expect(logo).not.toBeNull()
    expect(logo?.getAttribute('aria-hidden')).toBe('true')
  })

  it.each([
    ['google-huy', 'Bạn đã hủy đăng nhập bằng Google'],
    ['google-chua-xac-minh', 'chưa được xác minh'],
    ['google-email-mat-khau', 'đăng nhập bằng email và mật khẩu'],
    ['google-bi-khoa', 'đã bị khóa'],
    ['google-that-bai', 'Không đăng nhập được bằng Google'],
  ])('?loi=%s thì hiện đúng thông báo', async (code, expected) => {
    renderLogin(`/dang-nhap?loi=${code}`)

    expect((await screen.findByRole('alert')).textContent).toContain(expected)
  })

  it('?loi= lạ (không nằm trong danh sách) thì không hiện gì, không chép nguyên giá trị lên màn hình', async () => {
    renderLogin('/dang-nhap?loi=<b>xin-chao</b>')

    await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })
    expect(screen.queryByRole('alert')).toBeNull()
    expect(screen.queryByText(/xin-chao/)).toBeNull()
  })
})
