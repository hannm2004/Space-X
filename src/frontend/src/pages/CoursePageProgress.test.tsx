import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { fetchCourses, fetchLessons, fetchProgress, restartCourse, type CourseProgress } from '../learningApi'
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
const SIGNED_IN: AuthState = {
  status: 'authenticated',
  user: { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' },
}

const HAI_BAI = [
  { id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: null },
  { id: 2, slug: 'mang-may-tinh', title: 'Mạng máy tính', summary: null },
]

function renderCourse(auth: AuthState) {
  return render(
    <MemoryRouter initialEntries={['/khoa-hoc/tin-hoc-10']}>
      <AuthContext.Provider value={{ state: auth, signIn: vi.fn(), signOut: vi.fn() }}>
        <Routes>
          <Route path="/khoa-hoc/:courseSlug" element={<CoursePage />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

function tienDo(over: Partial<CourseProgress> = {}): CourseProgress {
  return {
    courseId: 1,
    courseSlug: 'tin-hoc-10',
    courseTitle: 'Tin học 10',
    completedLessons: 1,
    totalLessons: 2,
    percent: 50,
    continueLesson: { id: 2, slug: 'mang-may-tinh', title: 'Mạng máy tính' },
    ...over,
  }
}

beforeEach(() => {
  fetchCoursesMock.mockReset()
  fetchLessonsMock.mockReset()
  fetchProgressMock.mockReset()
  restartCourseMock.mockReset()
  fetchCoursesMock.mockResolvedValue([{ id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: null }])
  fetchLessonsMock.mockResolvedValue(HAI_BAI)
  fetchProgressMock.mockResolvedValue([tienDo()])
  restartCourseMock.mockResolvedValue(undefined)
})

describe('CoursePage — tiến độ học', () => {
  it('khách chưa đăng nhập: không gọi API tiến độ, không có thanh tiến độ, "Học tiếp" hay "Học lại"', async () => {
    renderCourse(ANONYMOUS)

    await screen.findByRole('heading', { level: 1, name: 'Tin học 10' })
    expect(fetchProgressMock).not.toHaveBeenCalled()
    expect(screen.queryByRole('progressbar')).toBeNull()
    expect(screen.queryByRole('link', { name: /Học tiếp/ })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Học lại' })).toBeNull()
  })

  it('đã đăng nhập: hiện phần trăm và số bài của đúng khóa này, không lẫn tiến độ khóa khác', async () => {
    fetchProgressMock.mockResolvedValue([
      tienDo({ courseId: 2, courseSlug: 'tin-hoc-11', courseTitle: 'Tin học 11', completedLessons: 2, percent: 100 }),
      tienDo(),
    ])

    renderCourse(SIGNED_IN)

    const bar = await screen.findByRole('progressbar', { name: 'Tiến độ Tin học 10' })
    expect(bar.getAttribute('aria-valuenow')).toBe('50')
    expect(screen.getByText('1/2 bài hoàn thành')).not.toBeNull()
    expect(screen.getAllByRole('progressbar')).toHaveLength(1)
  })

  it('"Học tiếp" là liên kết tới đúng bài mà máy chủ chỉ định', async () => {
    renderCourse(SIGNED_IN)

    const tiep = await screen.findByRole('link', { name: /Học tiếp/ })
    expect(tiep.getAttribute('href')).toBe('/bai/mang-may-tinh')
    expect(tiep.textContent).toContain('Mạng máy tính')
  })

  it('học xong hết: báo hoàn thành khóa, không còn "Học tiếp" nhưng còn "Học lại"', async () => {
    fetchProgressMock.mockResolvedValue([tienDo({ completedLessons: 2, percent: 100, continueLesson: null })])

    renderCourse(SIGNED_IN)

    expect(await screen.findByText('Bạn đã hoàn thành khóa học này.')).not.toBeNull()
    expect(screen.queryByRole('link', { name: /Học tiếp/ })).toBeNull()
    expect(screen.getByRole('button', { name: 'Học lại' })).not.toBeNull()
  })

  it('chưa hoàn thành bài nào: vẫn có "Học tiếp" (bài đầu) nhưng chưa có "Học lại"', async () => {
    fetchProgressMock.mockResolvedValue([
      tienDo({
        completedLessons: 0,
        percent: 0,
        continueLesson: { id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu' },
      }),
    ])

    renderCourse(SIGNED_IN)

    const tiep = await screen.findByRole('link', { name: /Học tiếp/ })
    expect(tiep.getAttribute('href')).toBe('/bai/thong-tin-va-du-lieu')
    expect(screen.queryByRole('button', { name: 'Học lại' })).toBeNull()
  })

  it('khóa chưa có bài đã đăng thì không hiện khối tiến độ', async () => {
    fetchProgressMock.mockResolvedValue([
      tienDo({ totalLessons: 0, completedLessons: 0, percent: 0, continueLesson: null }),
    ])

    renderCourse(SIGNED_IN)

    await screen.findByRole('heading', { level: 1, name: 'Tin học 10' })
    await vi.waitFor(() => expect(fetchProgressMock).toHaveBeenCalled())
    expect(screen.queryByRole('progressbar')).toBeNull()
  })

  it('không tải được tiến độ thì danh sách bài vẫn hiện, có một dòng báo ngắn (không in lỗi của máy chủ)', async () => {
    fetchProgressMock.mockRejectedValue(new ApiError(500, 'SQLException: bí mật nội bộ'))

    renderCourse(SIGNED_IN)

    expect(await screen.findByText('Không tải được tiến độ học.')).not.toBeNull()
    expect(screen.getByRole('link', { name: /Thông tin và dữ liệu/ })).not.toBeNull()
    expect(document.body.textContent).not.toContain('SQLException')
  })
})

describe('CoursePage — học lại khóa', () => {
  it('bấm "Học lại" chỉ hỏi xác nhận, chưa xóa gì; bấm "Hủy" thì đóng và không gọi API', async () => {
    renderCourse(SIGNED_IN)

    fireEvent.click(await screen.findByRole('button', { name: 'Học lại' }))

    expect(screen.getByText(/xóa toàn bộ tiến độ/i)).not.toBeNull()
    expect(restartCourseMock).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Hủy' }))

    expect(screen.queryByText(/xóa toàn bộ tiến độ/i)).toBeNull()
    expect(screen.getByRole('button', { name: 'Học lại' })).not.toBeNull()
    expect(restartCourseMock).not.toHaveBeenCalled()
  })

  it('xác nhận: xóa tiến độ của đúng khóa rồi tải lại tiến độ và hiện về 0%', async () => {
    fetchProgressMock.mockResolvedValueOnce([tienDo()]).mockResolvedValueOnce([
      tienDo({
        completedLessons: 0,
        percent: 0,
        continueLesson: { id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu' },
      }),
    ])
    renderCourse(SIGNED_IN)

    fireEvent.click(await screen.findByRole('button', { name: 'Học lại' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận học lại' }))

    await vi.waitFor(() =>
      expect(screen.getByRole('progressbar', { name: 'Tiến độ Tin học 10' }).getAttribute('aria-valuenow')).toBe('0'),
    )
    expect(restartCourseMock).toHaveBeenCalledTimes(1)
    expect(restartCourseMock).toHaveBeenCalledWith(1)
    expect(fetchProgressMock).toHaveBeenCalledTimes(2)
    expect(screen.queryByRole('button', { name: 'Học lại' })).toBeNull()
    expect(screen.queryByText(/xóa toàn bộ tiến độ/i)).toBeNull()
  })

  it('xóa lỗi thì báo câu cố định (không in lỗi của máy chủ) và giữ nguyên tiến độ đang hiện', async () => {
    restartCourseMock.mockRejectedValue(new ApiError(500, 'SQLException: bí mật nội bộ'))
    renderCourse(SIGNED_IN)

    fireEvent.click(await screen.findByRole('button', { name: 'Học lại' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận học lại' }))

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Không học lại được, hãy thử lại.')
    expect(alert.textContent).not.toContain('SQLException')
    expect(screen.getByRole('progressbar', { name: 'Tiến độ Tin học 10' }).getAttribute('aria-valuenow')).toBe('50')
    expect(fetchProgressMock).toHaveBeenCalledTimes(1)
  })

  it('đang xóa thì không bấm xác nhận thêm lần nữa được', async () => {
    restartCourseMock.mockReturnValue(new Promise(() => {}))
    renderCourse(SIGNED_IN)

    fireEvent.click(await screen.findByRole('button', { name: 'Học lại' }))
    const xacNhan = screen.getByRole('button', { name: 'Xác nhận học lại' }) as HTMLButtonElement
    fireEvent.click(xacNhan)
    fireEvent.click(xacNhan)

    expect(xacNhan.disabled).toBe(true)
    expect(restartCourseMock).toHaveBeenCalledTimes(1)
  })
})
