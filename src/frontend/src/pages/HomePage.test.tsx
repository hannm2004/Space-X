import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { fetchCourses, fetchProgress, type CourseProgress } from '../learningApi'
import { AuthContext, type AuthState } from '../useAuth'
import HomePage from './HomePage'

vi.mock('../learningApi', () => ({ fetchCourses: vi.fn(), fetchProgress: vi.fn() }))
vi.mock('../api', () => ({ fetchHealth: vi.fn().mockResolvedValue('UP') }))

const fetchCoursesMock = vi.mocked(fetchCourses)
const fetchProgressMock = vi.mocked(fetchProgress)

const ANONYMOUS: AuthState = { status: 'anonymous' }
const SIGNED_IN: AuthState = {
  status: 'authenticated',
  user: { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' },
}

function renderHome(auth: AuthState = ANONYMOUS) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ state: auth, signIn: vi.fn(), signOut: vi.fn() }}>
        <HomePage />
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

function tienDo(courseId: number, slug: string, title: string, done: number, total: number): CourseProgress {
  return {
    courseId,
    courseSlug: slug,
    courseTitle: title,
    completedLessons: done,
    totalLessons: total,
    percent: total === 0 ? 0 : Math.floor((done * 100) / total),
    continueLesson: null,
  }
}

beforeEach(() => {
  fetchCoursesMock.mockReset()
  fetchProgressMock.mockReset()
  fetchProgressMock.mockResolvedValue([])
})

describe('HomePage', () => {
  it('có tiêu đề chính và đoạn giới thiệu', () => {
    fetchCoursesMock.mockReturnValue(new Promise(() => {}))

    renderHome()

    expect(screen.getByRole('heading', { level: 1, name: 'Học Tin học 10, 11, 12' })).not.toBeNull()
  })

  it('hiện "đang tải" trong lúc chờ danh sách khóa', () => {
    fetchCoursesMock.mockReturnValue(new Promise(() => {}))

    renderHome()

    expect(screen.getByText('Đang tải danh sách khóa học...')).not.toBeNull()
  })

  it('hiện từng khóa học có tên, mô tả và liên kết tới trang khóa', async () => {
    fetchCoursesMock.mockResolvedValue([
      { id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: 'Khóa lớp 10' },
      { id: 2, slug: 'tin-hoc-11', title: 'Tin học 11', description: null },
    ])

    renderHome()

    const first = await screen.findByRole('link', { name: /Tin học 10/ })
    expect(first.getAttribute('href')).toBe('/khoa-hoc/tin-hoc-10')
    expect(screen.getByText('Khóa lớp 10')).not.toBeNull()
    const second = screen.getByRole('link', { name: /Tin học 11/ })
    expect(second.getAttribute('href')).toBe('/khoa-hoc/tin-hoc-11')
    expect(screen.queryByText('Đang tải danh sách khóa học...')).toBeNull()
  })

  it('slug có ký tự lạ vẫn được mã hóa trong liên kết', async () => {
    fetchCoursesMock.mockResolvedValue([
      { id: 1, slug: 'a b/c', title: 'Khóa lạ', description: null },
    ])

    renderHome()

    const link = await screen.findByRole('link', { name: /Khóa lạ/ })
    expect(link.getAttribute('href')).toBe('/khoa-hoc/a%20b%2Fc')
  })

  it('chưa có khóa nào thì báo rõ, không để trống', async () => {
    fetchCoursesMock.mockResolvedValue([])

    renderHome()

    expect(await screen.findByText('Chưa có khóa học nào.')).not.toBeNull()
  })

  it('lỗi tải thì báo lỗi và bấm Thử lại sẽ tải lại', async () => {
    fetchCoursesMock
      .mockRejectedValueOnce(new ApiError(0, 'Không kết nối được tới máy chủ'))
      .mockResolvedValueOnce([{ id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: null }])

    renderHome()

    expect(await screen.findByText('Không tải được danh sách khóa học.')).not.toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('link', { name: /Tin học 10/ })).not.toBeNull()
    expect(fetchCoursesMock).toHaveBeenCalledTimes(2)
  })
})

describe('HomePage — tiến độ học', () => {
  const KHOA = [
    { id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: null },
    { id: 2, slug: 'tin-hoc-11', title: 'Tin học 11', description: null },
  ]

  it('khách chưa đăng nhập: không gọi API tiến độ và không có thanh tiến độ', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA)

    renderHome(ANONYMOUS)

    await screen.findByRole('link', { name: /Tin học 10/ })
    expect(fetchProgressMock).not.toHaveBeenCalled()
    expect(screen.queryByRole('progressbar')).toBeNull()
  })

  it('đã đăng nhập: mỗi khóa có thanh tiến độ và số bài đã hoàn thành của đúng khóa đó', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA)
    fetchProgressMock.mockResolvedValue([
      tienDo(1, 'tin-hoc-10', 'Tin học 10', 1, 3),
      tienDo(2, 'tin-hoc-11', 'Tin học 11', 0, 2),
    ])

    renderHome(SIGNED_IN)

    const bar10 = await screen.findByRole('progressbar', { name: 'Tiến độ Tin học 10' })
    expect(bar10.getAttribute('aria-valuenow')).toBe('33')
    expect(screen.getByText('1/3 bài hoàn thành')).not.toBeNull()
    const bar11 = screen.getByRole('progressbar', { name: 'Tiến độ Tin học 11' })
    expect(bar11.getAttribute('aria-valuenow')).toBe('0')
    expect(screen.getByText('0/2 bài hoàn thành')).not.toBeNull()
    expect(fetchProgressMock).toHaveBeenCalledTimes(1)
  })

  it('khóa không có trong tiến độ hoặc chưa có bài đã đăng thì không hiện thanh tiến độ', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA)
    fetchProgressMock.mockResolvedValue([tienDo(2, 'tin-hoc-11', 'Tin học 11', 0, 0)])

    renderHome(SIGNED_IN)

    await screen.findByRole('link', { name: /Tin học 10/ })
    await vi.waitFor(() => expect(fetchProgressMock).toHaveBeenCalled())
    expect(screen.queryByRole('progressbar')).toBeNull()
  })

  it('không tải được tiến độ thì danh sách khóa vẫn hiện bình thường, không báo lỗi chặn trang', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA)
    fetchProgressMock.mockRejectedValue(new ApiError(500, 'SQLException: bí mật nội bộ'))

    renderHome(SIGNED_IN)

    expect(await screen.findByRole('link', { name: /Tin học 10/ })).not.toBeNull()
    await vi.waitFor(() => expect(fetchProgressMock).toHaveBeenCalled())
    expect(screen.queryByRole('progressbar')).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(document.body.textContent).not.toContain('SQLException')
  })
})
