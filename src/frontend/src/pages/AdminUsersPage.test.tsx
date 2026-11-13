import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchAdminUsers, resetUserPassword, setUserEnabled, type AdminUser, type AdminUserPage } from '../adminApi'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser } from '../authApi'
import { ApiError } from '../client'
import AdminUsersPage from './AdminUsersPage'

vi.mock('../adminApi', () => ({
  fetchAdminUsers: vi.fn(),
  setUserEnabled: vi.fn(),
  resetUserPassword: vi.fn(),
}))
vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchAdminUsersMock = vi.mocked(fetchAdminUsers)
const setUserEnabledMock = vi.mocked(setUserEnabled)
const resetUserPasswordMock = vi.mocked(resetUserPassword)
const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)

const ADMIN_ME = { displayName: 'Quản trị viên', email: 'admin@example.com', role: 'ADMIN' as const }
const LEARNER_ME = { displayName: 'Học sinh', email: 'hs@example.com', role: 'LEARNER' as const }

const ADMIN_ROW: AdminUser = {
  id: 1,
  email: 'admin@example.com',
  displayName: 'Admin Gốc',
  role: 'ADMIN',
  authProvider: 'LOCAL',
  enabled: true,
  createdAt: '2026-10-01T08:00:00',
}
const HS: AdminUser = {
  id: 7,
  email: 'hs@example.com',
  displayName: 'Học sinh Một',
  role: 'LEARNER',
  authProvider: 'LOCAL',
  enabled: true,
  createdAt: '2026-11-01T08:00:00',
}
const GOOGLE: AdminUser = {
  id: 9,
  email: 'gg@example.com',
  displayName: 'Học sinh Google',
  role: 'LEARNER',
  authProvider: 'GOOGLE',
  enabled: true,
  createdAt: '2026-11-02T08:00:00',
}
const KHOA: AdminUser = { ...HS, id: 8, email: 'khoa@example.com', displayName: 'Học sinh Bị Khóa', enabled: false }

function userPage(overrides: Partial<AdminUserPage> = {}): AdminUserPage {
  return { items: [ADMIN_ROW, HS, GOOGLE, KHOA], page: 0, size: 20, totalItems: 4, ...overrides }
}

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

function renderPage(entry = '/quan-tri/nguoi-dung') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <AuthProvider>
        <Routes>
          <Route path="/quan-tri/nguoi-dung" element={<AdminUsersPage />} />
          <Route path="/dang-nhap" element={<LoginStub />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function rowOf(email: string) {
  const cell = await screen.findByText(email)
  return within(cell.closest('li') as HTMLElement)
}

beforeEach(() => {
  fetchAdminUsersMock.mockReset()
  setUserEnabledMock.mockReset()
  resetUserPasswordMock.mockReset()
  fetchCurrentUserMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(ADMIN_ME)
  fetchAdminUsersMock.mockResolvedValue(userPage())
})

describe('AdminUsersPage — quyền truy cập', () => {
  it('chưa đăng nhập thì chuyển sang trang đăng nhập, nhớ để quay lại và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(null)

    renderPage()

    expect(await screen.findByText('Trang đăng nhập, quay lại /quan-tri/nguoi-dung')).not.toBeNull()
    expect(fetchAdminUsersMock).not.toHaveBeenCalled()
  })

  it('học sinh đăng nhập thì báo không có quyền, không hiện danh sách và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(LEARNER_ME)

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(screen.queryByRole('heading', { level: 1, name: 'Quản lý người dùng' })).toBeNull()
    expect(fetchAdminUsersMock).not.toHaveBeenCalled()
  })

  it('máy chủ trả 403 thì hiện câu không có quyền thay vì nút Thử lại', async () => {
    fetchAdminUsersMock.mockRejectedValue(new ApiError(403, 'Bạn không có quyền thực hiện thao tác này'))

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })
})

describe('AdminUsersPage — danh sách', () => {
  it('Admin thấy tiêu đề, và mỗi người dùng có tên, email, vai trò và trạng thái', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { level: 1, name: 'Quản lý người dùng' })).not.toBeNull()
    expect(fetchAdminUsersMock).toHaveBeenCalledWith('', 0)

    const hs = await rowOf('hs@example.com')
    expect(hs.getByText('Học sinh Một')).not.toBeNull()
    expect(hs.getByText('Học sinh')).not.toBeNull()
    expect(hs.getByText('Đang hoạt động')).not.toBeNull()

    const admin = await rowOf('admin@example.com')
    expect(admin.getByText('Admin Gốc')).not.toBeNull()
    expect(admin.getByText('Quản trị viên')).not.toBeNull()

    const khoa = await rowOf('khoa@example.com')
    expect(khoa.getByText('Đã khóa')).not.toBeNull()
    expect(screen.getByText('Tìm thấy 4 người dùng')).not.toBeNull()
  })

  it('đánh dấu tài khoản Google', async () => {
    renderPage()

    const google = await rowOf('gg@example.com')
    expect(google.getByText('Google')).not.toBeNull()
  })

  it('hiện "đang tải" trong lúc chờ', () => {
    fetchAdminUsersMock.mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByText('Đang tải...')).not.toBeNull()
  })

  it('lỗi tải (không phải 403) hiện câu lỗi và nút Thử lại gọi lại API', async () => {
    fetchAdminUsersMock.mockRejectedValueOnce(new ApiError(500, 'Máy chủ trả mã lỗi 500'))

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('Không tải được danh sách người dùng')
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByText('hs@example.com')).not.toBeNull()
    expect(fetchAdminUsersMock).toHaveBeenCalledTimes(2)
  })

  it('không có kết quả thì báo không có người dùng phù hợp', async () => {
    fetchAdminUsersMock.mockResolvedValue(userPage({ items: [], totalItems: 0 }))

    renderPage('/quan-tri/nguoi-dung?q=khongco')

    expect(await screen.findByText(/Không có người dùng phù hợp/)).not.toBeNull()
  })

  it('không bao giờ in mật khẩu hay mã băm ra trang', async () => {
    renderPage()

    await screen.findByText('hs@example.com')
    expect(document.body.textContent).not.toMatch(/password|mật khẩu tạm|\$2a\$/i)
  })
})

describe('AdminUsersPage — tìm kiếm và phân trang', () => {
  it('đọc từ khóa và trang từ địa chỉ rồi gọi API', async () => {
    renderPage('/quan-tri/nguoi-dung?q=nguyen&page=2')

    await screen.findByText('hs@example.com')
    expect(fetchAdminUsersMock).toHaveBeenCalledWith('nguyen', 2)
    expect((screen.getByRole('searchbox', { name: 'Tìm theo email hoặc tên' }) as HTMLInputElement).value).toBe(
      'nguyen',
    )
  })

  it('gửi ô tìm thì cắt khoảng trắng và tải lại từ trang 0', async () => {
    renderPage('/quan-tri/nguoi-dung?page=3')
    await screen.findByText('hs@example.com')

    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm theo email hoặc tên' }), {
      target: { value: '  nguyen  ' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))

    await screen.findByText('hs@example.com')
    expect(fetchAdminUsersMock).toHaveBeenLastCalledWith('nguyen', 0)
  })

  it('gửi ô tìm trống thì quay về danh sách đầy đủ', async () => {
    renderPage('/quan-tri/nguoi-dung?q=nguyen')
    await screen.findByText('hs@example.com')

    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm theo email hoặc tên' }), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))

    await screen.findByText('hs@example.com')
    expect(fetchAdminUsersMock).toHaveBeenLastCalledWith('', 0)
  })

  it('nhiều trang thì có liên kết trang sau giữ nguyên từ khóa, và không có trang trước ở trang đầu', async () => {
    fetchAdminUsersMock.mockResolvedValue(userPage({ totalItems: 45 }))

    renderPage('/quan-tri/nguoi-dung?q=nguyen')

    const nav = await screen.findByRole('navigation', { name: 'Phân trang' })
    expect(within(nav).getByText('Trang 1 / 3')).not.toBeNull()
    expect(within(nav).getByRole('link', { name: 'Trang sau' }).getAttribute('href')).toBe(
      '/quan-tri/nguoi-dung?q=nguyen&page=1',
    )
    expect(within(nav).queryByRole('link', { name: 'Trang trước' })).toBeNull()
  })

  it('trang cuối có trang trước nhưng không có trang sau', async () => {
    fetchAdminUsersMock.mockResolvedValue(userPage({ totalItems: 45, page: 2 }))

    renderPage('/quan-tri/nguoi-dung?page=2')

    const nav = await screen.findByRole('navigation', { name: 'Phân trang' })
    expect(within(nav).getByRole('link', { name: 'Trang trước' }).getAttribute('href')).toBe(
      '/quan-tri/nguoi-dung?page=1',
    )
    expect(within(nav).queryByRole('link', { name: 'Trang sau' })).toBeNull()
  })

  it('một trang thì không hiện phân trang; số trang sai trong địa chỉ coi như trang 0', async () => {
    renderPage('/quan-tri/nguoi-dung?page=abc')

    await screen.findByText('hs@example.com')
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
    expect(fetchAdminUsersMock).toHaveBeenCalledWith('', 0)
  })
})

describe('AdminUsersPage — khóa và mở khóa', () => {
  it('bấm Khóa thì phải xác nhận, chưa gọi API cho tới khi bấm xác nhận', async () => {
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Khóa hs@example.com' }))

    expect(hs.getByText(/bị đăng xuất khỏi mọi thiết bị/)).not.toBeNull()
    expect(setUserEnabledMock).not.toHaveBeenCalled()
  })

  it('xác nhận khóa thì gọi API với id của người đó, rồi dòng hiện "Đã khóa" và nút Mở khóa', async () => {
    setUserEnabledMock.mockResolvedValue({ ...HS, enabled: false })
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Khóa hs@example.com' }))
    fireEvent.click(hs.getByRole('button', { name: 'Xác nhận khóa' }))

    expect(await hs.findByText('Đã khóa')).not.toBeNull()
    expect(setUserEnabledMock).toHaveBeenCalledTimes(1)
    expect(setUserEnabledMock).toHaveBeenCalledWith(7, false)
    expect(hs.getByRole('button', { name: 'Mở khóa hs@example.com' })).not.toBeNull()
    expect(hs.queryByRole('button', { name: 'Xác nhận khóa' })).toBeNull()
  })

  it('bấm Hủy thì không khóa và đóng phần xác nhận', async () => {
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Khóa hs@example.com' }))
    fireEvent.click(hs.getByRole('button', { name: 'Hủy' }))

    expect(setUserEnabledMock).not.toHaveBeenCalled()
    expect(hs.queryByRole('button', { name: 'Xác nhận khóa' })).toBeNull()
    expect(hs.getByText('Đang hoạt động')).not.toBeNull()
  })

  it('bấm xác nhận hai lần liên tiếp chỉ gửi một yêu cầu', async () => {
    setUserEnabledMock.mockReturnValue(new Promise(() => {}))
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Khóa hs@example.com' }))
    const confirm = hs.getByRole('button', { name: 'Xác nhận khóa' })
    fireEvent.click(confirm)
    fireEvent.click(confirm)

    expect(setUserEnabledMock).toHaveBeenCalledTimes(1)
  })

  it('mở khóa tài khoản đang khóa thì gọi API ngay không cần xác nhận', async () => {
    setUserEnabledMock.mockResolvedValue({ ...KHOA, enabled: true })
    renderPage()
    const khoa = await rowOf('khoa@example.com')

    fireEvent.click(khoa.getByRole('button', { name: 'Mở khóa khoa@example.com' }))

    expect(await khoa.findByText('Đang hoạt động')).not.toBeNull()
    expect(setUserEnabledMock).toHaveBeenCalledWith(8, true)
    expect(khoa.getByRole('button', { name: 'Khóa khoa@example.com' })).not.toBeNull()
  })

  it('máy chủ từ chối (409) thì hiện đúng lý do, trạng thái không đổi và vẫn thử lại được', async () => {
    setUserEnabledMock.mockRejectedValue(
      new ApiError(409, 'Không thể khóa Admin cuối cùng còn hoạt động', {
        detail: 'Không thể khóa Admin cuối cùng còn hoạt động',
      }),
    )
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Khóa hs@example.com' }))
    fireEvent.click(hs.getByRole('button', { name: 'Xác nhận khóa' }))

    expect((await hs.findByRole('alert')).textContent).toContain('Không thể khóa Admin cuối cùng còn hoạt động')
    expect(hs.getByText('Đang hoạt động')).not.toBeNull()
    expect((hs.getByRole('button', { name: 'Xác nhận khóa' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('mất kết nối thì báo lỗi kết nối, không đổi trạng thái', async () => {
    setUserEnabledMock.mockRejectedValue(new ApiError(0, 'Không kết nối được tới máy chủ'))
    renderPage()
    const khoa = await rowOf('khoa@example.com')

    fireEvent.click(khoa.getByRole('button', { name: 'Mở khóa khoa@example.com' }))

    expect((await khoa.findByRole('alert')).textContent).toContain('Không kết nối được')
    expect(khoa.getByText('Đã khóa')).not.toBeNull()
  })

  it('không có nút Khóa hay Đặt lại mật khẩu cho chính tài khoản đang đăng nhập, và có nhãn "bạn"', async () => {
    renderPage()
    const me = await rowOf('admin@example.com')

    expect(me.getByText('(bạn)')).not.toBeNull()
    expect(me.queryByRole('button', { name: /Khóa/ })).toBeNull()
    expect(me.queryByRole('button', { name: /Đặt lại mật khẩu/ })).toBeNull()
  })

  it('so sánh email không phân biệt hoa thường khi nhận ra chính mình', async () => {
    fetchCurrentUserMock.mockResolvedValue({ ...ADMIN_ME, email: 'Admin@Example.com' })

    renderPage()
    const me = await rowOf('admin@example.com')

    expect(me.getByText('(bạn)')).not.toBeNull()
  })
})

describe('AdminUsersPage — đặt lại mật khẩu', () => {
  it('bấm Đặt lại mật khẩu thì phải xác nhận và nói rõ hậu quả, chưa gọi API', async () => {
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Đặt lại mật khẩu hs@example.com' }))

    expect(hs.getByText(/Mật khẩu cũ sẽ không dùng được/)).not.toBeNull()
    expect(resetUserPasswordMock).not.toHaveBeenCalled()
  })

  it('xác nhận thì hiện mật khẩu tạm một lần kèm lời dặn, và gọi API đúng id', async () => {
    resetUserPasswordMock.mockResolvedValue({ id: 7, email: 'hs@example.com', temporaryPassword: 'Abcd2345Efgh6789' })
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Đặt lại mật khẩu hs@example.com' }))
    fireEvent.click(hs.getByRole('button', { name: 'Xác nhận đặt lại' }))

    const status = await hs.findByRole('status')
    expect(status.textContent).toContain('Abcd2345Efgh6789')
    expect(status.textContent).toContain('chỉ hiện một lần')
    expect(resetUserPasswordMock).toHaveBeenCalledTimes(1)
    expect(resetUserPasswordMock).toHaveBeenCalledWith(7)
  })

  it('bấm Đóng thì mật khẩu tạm biến mất khỏi trang', async () => {
    resetUserPasswordMock.mockResolvedValue({ id: 7, email: 'hs@example.com', temporaryPassword: 'Abcd2345Efgh6789' })
    renderPage()
    const hs = await rowOf('hs@example.com')
    fireEvent.click(hs.getByRole('button', { name: 'Đặt lại mật khẩu hs@example.com' }))
    fireEvent.click(hs.getByRole('button', { name: 'Xác nhận đặt lại' }))
    await hs.findByRole('status')

    fireEvent.click(hs.getByRole('button', { name: 'Đóng' }))

    expect(document.body.textContent).not.toContain('Abcd2345Efgh6789')
    expect(hs.queryByRole('status')).toBeNull()
  })

  it('đổi sang trang khác của danh sách thì mật khẩu tạm không còn trên trang', async () => {
    resetUserPasswordMock.mockResolvedValue({ id: 7, email: 'hs@example.com', temporaryPassword: 'Abcd2345Efgh6789' })
    fetchAdminUsersMock.mockResolvedValue(userPage({ totalItems: 45 }))
    renderPage()
    const hs = await rowOf('hs@example.com')
    fireEvent.click(hs.getByRole('button', { name: 'Đặt lại mật khẩu hs@example.com' }))
    fireEvent.click(hs.getByRole('button', { name: 'Xác nhận đặt lại' }))
    await hs.findByRole('status')

    fireEvent.click(screen.getByRole('link', { name: 'Trang sau' }))

    await screen.findByText('hs@example.com')
    expect(document.body.textContent).not.toContain('Abcd2345Efgh6789')
  })

  it('bấm xác nhận hai lần liên tiếp chỉ đặt lại một lần', async () => {
    resetUserPasswordMock.mockReturnValue(new Promise(() => {}))
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Đặt lại mật khẩu hs@example.com' }))
    const confirm = hs.getByRole('button', { name: 'Xác nhận đặt lại' })
    fireEvent.click(confirm)
    fireEvent.click(confirm)

    expect(resetUserPasswordMock).toHaveBeenCalledTimes(1)
  })

  it('máy chủ từ chối thì hiện lý do và không hiện mật khẩu nào', async () => {
    resetUserPasswordMock.mockRejectedValue(
      new ApiError(409, 'Tài khoản Google không có mật khẩu để đặt lại', {
        detail: 'Tài khoản Google không có mật khẩu để đặt lại',
      }),
    )
    renderPage()
    const hs = await rowOf('hs@example.com')

    fireEvent.click(hs.getByRole('button', { name: 'Đặt lại mật khẩu hs@example.com' }))
    fireEvent.click(hs.getByRole('button', { name: 'Xác nhận đặt lại' }))

    expect((await hs.findByRole('alert')).textContent).toContain('Tài khoản Google không có mật khẩu để đặt lại')
    expect(hs.queryByRole('status')).toBeNull()
  })

  it('tài khoản Google không có nút Đặt lại mật khẩu nhưng vẫn khóa được', async () => {
    renderPage()
    const google = await rowOf('gg@example.com')

    expect(google.queryByRole('button', { name: /Đặt lại mật khẩu/ })).toBeNull()
    expect(google.getByRole('button', { name: 'Khóa gg@example.com' })).not.toBeNull()
  })
})
