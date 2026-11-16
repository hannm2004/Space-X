import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchAdminDashboard, type AdminDashboard } from '../adminApi'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser } from '../authApi'
import { ApiError } from '../client'
import AdminDashboardPage from './AdminDashboardPage'

vi.mock('../adminApi', () => ({ fetchAdminDashboard: vi.fn() }))
vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchAdminDashboardMock = vi.mocked(fetchAdminDashboard)
const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)

const ADMIN_ME = { displayName: 'Quản trị viên', email: 'admin@example.com', role: 'ADMIN' as const }
const LEARNER_ME = { displayName: 'Học sinh', email: 'hs@example.com', role: 'LEARNER' as const }

const DASHBOARD: AdminDashboard = {
  users: { total: 12, newLast7Days: 3 },
  lessons: { total: 7, draft: 2, published: 5 },
  courses: [
    { courseId: 1, slug: 'tin-hoc-10', title: 'Tin học 10', draft: 1, published: 3 },
    { courseId: 2, slug: 'tin-hoc-11', title: 'Tin học 11', draft: 0, published: 0 },
  ],
  learning: { activeLearners: 4, completedLessons: 9 },
}

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/quan-tri']}>
      <AuthProvider>
        <Routes>
          <Route path="/quan-tri" element={<AdminDashboardPage />} />
          <Route path="/dang-nhap" element={<LoginStub />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  fetchAdminDashboardMock.mockReset()
  fetchCurrentUserMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(ADMIN_ME)
  fetchAdminDashboardMock.mockResolvedValue(DASHBOARD)
})

describe('AdminDashboardPage — quyền truy cập', () => {
  it('chưa đăng nhập thì chuyển sang trang đăng nhập, nhớ để quay lại và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(null)

    renderPage()

    expect(await screen.findByText('Trang đăng nhập, quay lại /quan-tri')).not.toBeNull()
    expect(fetchAdminDashboardMock).not.toHaveBeenCalled()
  })

  it('học sinh đăng nhập thì báo không có quyền và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(LEARNER_ME)

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(screen.queryByRole('heading', { level: 1, name: 'Tổng quan' })).toBeNull()
    expect(fetchAdminDashboardMock).not.toHaveBeenCalled()
  })

  it('máy chủ trả 403 thì hiện câu không có quyền thay vì nút Thử lại', async () => {
    fetchAdminDashboardMock.mockRejectedValue(new ApiError(403, 'Bạn không có quyền thực hiện thao tác này'))

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })
})

describe('AdminDashboardPage — số liệu', () => {
  it('Admin thấy tiêu đề và các số tổng hợp về người dùng, bài học, học tập', async () => {
    renderPage()

    // Tiêu đề hiện ngay; số liệu chỉ có sau khi tải xong nên chờ vùng đầu tiên.
    const users = within(await screen.findByRole('region', { name: 'Người dùng' }))
    expect(screen.getByRole('heading', { level: 1, name: 'Tổng quan' })).not.toBeNull()
    expect(users.getByText('12')).not.toBeNull()
    expect(users.getByText('3')).not.toBeNull()
    const lessons = within(screen.getByRole('region', { name: 'Bài học' }))
    expect(lessons.getByText('7')).not.toBeNull()
    expect(lessons.getByText('5')).not.toBeNull()
    expect(lessons.getByText('2')).not.toBeNull()
    const learning = within(screen.getByRole('region', { name: 'Học tập' }))
    expect(learning.getByText('4')).not.toBeNull()
    expect(learning.getByText('9')).not.toBeNull()
  })

  it('mỗi khóa một dòng với số bài Đã đăng và Nháp, kể cả khóa chưa có bài', async () => {
    renderPage()

    const table = await screen.findByRole('table', { name: 'Số bài theo khóa' })
    const rows = within(table).getAllByRole('row')
    // dòng đầu là tiêu đề cột
    expect(within(rows[1]).getAllByRole('cell').map((c) => c.textContent)).toEqual(['Tin học 10', '3', '1'])
    expect(within(rows[2]).getAllByRole('cell').map((c) => c.textContent)).toEqual(['Tin học 11', '0', '0'])
  })

  it('chưa có khóa nào thì nói rõ thay vì hiện bảng rỗng', async () => {
    fetchAdminDashboardMock.mockResolvedValue({ ...DASHBOARD, courses: [] })

    renderPage()

    expect(await screen.findByText('Chưa có khóa học nào.')).not.toBeNull()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('có liên kết sang trang quản lý người dùng', async () => {
    renderPage()

    const link = await screen.findByRole('link', { name: 'Quản lý người dùng' })
    expect(link.getAttribute('href')).toBe('/quan-tri/nguoi-dung')
  })

  it('tải lỗi thì hiện thông báo và nút Thử lại gọi lại API', async () => {
    fetchAdminDashboardMock.mockRejectedValueOnce(new Error('mạng lỗi'))

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('Không tải được số liệu')
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByRole('region', { name: 'Người dùng' })).not.toBeNull()
    expect(fetchAdminDashboardMock).toHaveBeenCalledTimes(2)
  })
})
