import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { fetchCourses, fetchLessons, fetchProgress, restartCourse } from '../learningApi'
import { AuthContext, type AuthState } from '../useAuth'
import CoursePage from './CoursePage'

vi.mock('../learningApi', () => ({
  fetchCourses: vi.fn(),
  fetchLessons: vi.fn(),
  fetchProgress: vi.fn(),
  restartCourse: vi.fn(),
}))

const fetchCoursesMock = vi.mocked(fetchCourses)
const fetchLessonsMock = vi.mocked(fetchLessons)
const fetchProgressMock = vi.mocked(fetchProgress)
const restartCourseMock = vi.mocked(restartCourse)

const ANONYMOUS: AuthState = { status: 'anonymous' }

const COURSES = [
  { id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: 'Khóa học lớp 10' },
  { id: 2, slug: 'tin-hoc-11', title: 'Tin học 11', description: null },
]

function renderCourse(path = '/khoa-hoc/tin-hoc-10', auth: AuthState = ANONYMOUS) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider value={{ state: auth, signIn: vi.fn(), signOut: vi.fn() }}>
        <Routes>
          <Route path="/khoa-hoc/:courseSlug" element={<CoursePage />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  fetchCoursesMock.mockReset()
  fetchLessonsMock.mockReset()
  fetchProgressMock.mockReset()
  restartCourseMock.mockReset()
  fetchCoursesMock.mockResolvedValue(COURSES)
  fetchProgressMock.mockResolvedValue([])
  restartCourseMock.mockResolvedValue(undefined)
})

describe('CoursePage', () => {
  it('hiện "đang tải" trong lúc chờ', () => {
    fetchLessonsMock.mockReturnValue(new Promise(() => {}))

    renderCourse()

    expect(screen.getByText('Đang tải khóa học...')).not.toBeNull()
  })

  it('hiện tên khóa, mô tả và danh sách bài có tóm tắt, mỗi bài là một liên kết', async () => {
    fetchLessonsMock.mockResolvedValue([
      { id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: 'Phân biệt thông tin và dữ liệu' },
      { id: 2, slug: 'mang-may-tinh', title: 'Mạng máy tính', summary: null },
    ])

    renderCourse()

    expect(await screen.findByRole('heading', { level: 1, name: 'Tin học 10' })).not.toBeNull()
    expect(screen.getByText('Khóa học lớp 10')).not.toBeNull()
    const first = screen.getByRole('link', { name: /Thông tin và dữ liệu/ })
    expect(first.getAttribute('href')).toBe('/bai/thong-tin-va-du-lieu')
    expect(screen.getByText('Phân biệt thông tin và dữ liệu')).not.toBeNull()
    const second = screen.getByRole('link', { name: /Mạng máy tính/ })
    expect(second.getAttribute('href')).toBe('/bai/mang-may-tinh')
    expect(fetchLessonsMock).toHaveBeenCalledWith('tin-hoc-10')
  })

  it('có liên kết quay về danh sách khóa', async () => {
    fetchLessonsMock.mockResolvedValue([])

    renderCourse()

    const back = await screen.findByRole('link', { name: /Tất cả khóa học/ })
    expect(back.getAttribute('href')).toBe('/')
  })

  it('khóa chưa có bài đã đăng thì báo rõ', async () => {
    fetchLessonsMock.mockResolvedValue([])

    renderCourse()

    expect(await screen.findByText('Khóa học này chưa có bài học nào.')).not.toBeNull()
  })

  it('khóa không tồn tại (404) thì báo không tìm thấy kèm liên kết về trang chủ', async () => {
    fetchLessonsMock.mockRejectedValue(new ApiError(404, 'Không tìm thấy khóa học'))

    renderCourse('/khoa-hoc/khong-co')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Không tìm thấy khóa học' }),
    ).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Về trang chủ' }).getAttribute('href')).toBe('/')
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })

  it('lỗi khác thì báo lỗi chung và bấm Thử lại sẽ tải lại', async () => {
    fetchLessonsMock
      .mockRejectedValueOnce(new ApiError(502, 'Máy chủ trả mã lỗi 502'))
      .mockResolvedValueOnce([])

    renderCourse()

    expect(await screen.findByText('Không tải được khóa học.')).not.toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByText('Khóa học này chưa có bài học nào.')).not.toBeNull()
  })

  it('lỗi khi tải danh sách khóa cũng báo lỗi chung, không hiện trang trống', async () => {
    fetchCoursesMock.mockRejectedValue(new ApiError(0, 'Không kết nối được tới máy chủ'))
    fetchLessonsMock.mockResolvedValue([])

    renderCourse()

    expect(await screen.findByText('Không tải được khóa học.')).not.toBeNull()
  })
})
