import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  deleteLesson,
  fetchAdminCourses,
  fetchAdminLessons,
  setLessonStatus,
  type AdminCourse,
  type AdminLesson,
  type AdminLessonPage,
  type AdminLessonSummary,
} from '../adminApi'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser } from '../authApi'
import { ApiError } from '../client'
import AdminLessonsPage from './AdminLessonsPage'

vi.mock('../adminApi', () => ({
  fetchAdminLessons: vi.fn(),
  fetchAdminCourses: vi.fn(),
  setLessonStatus: vi.fn(),
  deleteLesson: vi.fn(),
}))
vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchAdminLessonsMock = vi.mocked(fetchAdminLessons)
const fetchAdminCoursesMock = vi.mocked(fetchAdminCourses)
const setLessonStatusMock = vi.mocked(setLessonStatus)
const deleteLessonMock = vi.mocked(deleteLesson)
const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)

const ADMIN_ME = { displayName: 'Quản trị viên', email: 'admin@example.com', role: 'ADMIN' as const }
const LEARNER_ME = { displayName: 'Học sinh', email: 'hs@example.com', role: 'LEARNER' as const }

const KHOA: AdminCourse[] = [
  { id: 5, slug: 'tin-hoc-10', title: 'Tin học 10', description: null, sortOrder: 1, status: 'PUBLISHED', tuitionVnd: 0, categoryId: 3, level: 'BASIC', totalHours: 0 },
  { id: 6, slug: 'tin-hoc-11', title: 'Tin học 11', description: null, sortOrder: 2, status: 'PUBLISHED', tuitionVnd: 0, categoryId: 3, level: 'BASIC', totalHours: 0 },
]
const BAI_NHAP: AdminLessonSummary = {
  id: 3,
  courseId: 5,
  slug: 'may-tinh-la-gi',
  title: 'Máy tính là gì',
  summary: null,
  status: 'DRAFT',
  sortOrder: 1,
}
const BAI_DANG: AdminLessonSummary = {
  id: 4,
  courseId: 6,
  slug: 'phan-mem',
  title: 'Phần mềm',
  summary: 'Tóm tắt',
  status: 'PUBLISHED',
  sortOrder: 2,
}

function lessonPage(overrides: Partial<AdminLessonPage> = {}): AdminLessonPage {
  return { items: [BAI_NHAP, BAI_DANG], page: 0, size: 20, totalItems: 2, ...overrides }
}

function full(lesson: AdminLessonSummary, status = lesson.status): AdminLesson {
  return { ...lesson, status, contentMd: '# Nội dung' }
}

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

function renderPage(entry = '/quan-tri/bai-hoc') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <AuthProvider>
        <Routes>
          <Route path="/quan-tri/bai-hoc" element={<AdminLessonsPage />} />
          <Route path="/dang-nhap" element={<LoginStub />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function rowOf(title: string) {
  const cell = await screen.findByText(title)
  return within(cell.closest('li') as HTMLElement)
}

beforeEach(() => {
  fetchAdminLessonsMock.mockReset()
  fetchAdminCoursesMock.mockReset()
  setLessonStatusMock.mockReset()
  deleteLessonMock.mockReset()
  fetchCurrentUserMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(ADMIN_ME)
  fetchAdminCoursesMock.mockResolvedValue(KHOA)
  fetchAdminLessonsMock.mockResolvedValue(lessonPage())
})

describe('AdminLessonsPage — quyền truy cập', () => {
  it('chưa đăng nhập thì chuyển sang trang đăng nhập, nhớ để quay lại và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(null)

    renderPage('/quan-tri/bai-hoc?khoa=5')

    expect(await screen.findByText('Trang đăng nhập, quay lại /quan-tri/bai-hoc?khoa=5')).not.toBeNull()
    expect(fetchAdminLessonsMock).not.toHaveBeenCalled()
  })

  it('học sinh đăng nhập thì báo không có quyền và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(LEARNER_ME)

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(fetchAdminLessonsMock).not.toHaveBeenCalled()
  })

  it('máy chủ trả 403 thì hiện câu không có quyền thay vì nút Thử lại', async () => {
    fetchAdminLessonsMock.mockRejectedValue(new ApiError(403, 'Bạn không có quyền thực hiện thao tác này'))

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })
})

describe('AdminLessonsPage — danh sách và bộ lọc', () => {
  it('mỗi bài có tên, khóa, trạng thái bằng chữ và thứ tự; không lọc thì gọi với bộ lọc rỗng', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { level: 1, name: 'Quản lý bài học' })).not.toBeNull()
    // Danh sách được tải trong useEffect, chạy sau khi tiêu đề đã hiện một nhịp: phải chờ chứ không kiểm ngay.
    await waitFor(() => expect(fetchAdminLessonsMock).toHaveBeenCalledWith({ courseId: null, status: null, page: 0 }))
    const nhap = await rowOf('Máy tính là gì')
    expect(nhap.getByText('Tin học 10')).not.toBeNull()
    expect(nhap.getByText('Nháp')).not.toBeNull()
    expect(nhap.getByText('Thứ tự 1')).not.toBeNull()
    const dang = await rowOf('Phần mềm')
    expect(dang.getByText('Tin học 11')).not.toBeNull()
    expect(dang.getByText('Đã đăng')).not.toBeNull()
    expect(screen.getByText('Có 2 bài học')).not.toBeNull()
  })

  it('đọc bộ lọc và trang từ địa chỉ', async () => {
    renderPage('/quan-tri/bai-hoc?khoa=5&trang-thai=PUBLISHED&trang=1')

    await screen.findByRole('heading', { level: 1, name: 'Quản lý bài học' })
    await waitFor(() =>
      expect(fetchAdminLessonsMock).toHaveBeenCalledWith({ courseId: 5, status: 'PUBLISHED', page: 1 }),
    )
  })

  it('tham số lạ trong địa chỉ bị bỏ qua thay vì gửi lên máy chủ', async () => {
    renderPage('/quan-tri/bai-hoc?khoa=abc&trang-thai=ARCHIVED&trang=-3')

    await screen.findByRole('heading', { level: 1, name: 'Quản lý bài học' })
    await waitFor(() => expect(fetchAdminLessonsMock).toHaveBeenCalledWith({ courseId: null, status: null, page: 0 }))
  })

  it('đổi bộ lọc khóa thì tải lại theo khóa đó, về trang đầu', async () => {
    renderPage('/quan-tri/bai-hoc?trang=2')
    await screen.findByText('Máy tính là gì')

    fireEvent.change(screen.getByLabelText('Khóa học'), { target: { value: '6' } })

    await waitFor(() =>
      expect(fetchAdminLessonsMock).toHaveBeenLastCalledWith({ courseId: 6, status: null, page: 0 }),
    )
  })

  it('đổi bộ lọc trạng thái thì tải lại theo trạng thái đó', async () => {
    renderPage()
    await screen.findByText('Máy tính là gì')

    fireEvent.change(screen.getByLabelText('Trạng thái'), { target: { value: 'DRAFT' } })

    await waitFor(() =>
      expect(fetchAdminLessonsMock).toHaveBeenLastCalledWith({ courseId: null, status: 'DRAFT', page: 0 }),
    )
  })

  it('không có bài nào thì báo rõ', async () => {
    fetchAdminLessonsMock.mockResolvedValue(lessonPage({ items: [], totalItems: 0 }))

    renderPage()

    expect(await screen.findByText('Không có bài học phù hợp.')).not.toBeNull()
  })

  it('có liên kết thêm bài và sửa từng bài', async () => {
    renderPage()

    const nhap = await rowOf('Máy tính là gì')
    expect(screen.getByRole('link', { name: 'Thêm bài học' }).getAttribute('href')).toBe('/quan-tri/bai-hoc/moi')
    expect(nhap.getByRole('link', { name: 'Sửa Máy tính là gì' }).getAttribute('href')).toBe(
      '/quan-tri/bai-hoc/3/sua',
    )
  })

  it('mỗi bài có liên kết Ảnh dẫn thẳng tới mục ảnh trong trang sửa', async () => {
    renderPage()

    const nhap = await rowOf('Máy tính là gì')
    expect(nhap.getByRole('link', { name: 'Ảnh của bài Máy tính là gì' }).getAttribute('href')).toBe(
      '/quan-tri/bai-hoc/3/sua#anh-cua-bai',
    )
  })

  it('có nhiều trang thì có liên kết trang trước/sau giữ nguyên bộ lọc', async () => {
    fetchAdminLessonsMock.mockResolvedValue(lessonPage({ page: 1, size: 20, totalItems: 45 }))

    renderPage('/quan-tri/bai-hoc?khoa=5&trang=1')

    await screen.findByText('Trang 2 / 3')
    expect(screen.getByRole('link', { name: 'Trang trước' }).getAttribute('href')).toBe(
      '/quan-tri/bai-hoc?khoa=5&trang=0',
    )
    expect(screen.getByRole('link', { name: 'Trang sau' }).getAttribute('href')).toBe(
      '/quan-tri/bai-hoc?khoa=5&trang=2',
    )
  })

  it('lỗi tải thì có nút Thử lại', async () => {
    fetchAdminLessonsMock.mockRejectedValueOnce(new Error('mạng'))

    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Máy tính là gì')).not.toBeNull()
  })
})

describe('AdminLessonsPage — đăng và ẩn bài', () => {
  it('bài nháp có nút Đăng bài; bấm thì gọi API và dòng đổi sang Đã đăng với nút Ẩn bài', async () => {
    setLessonStatusMock.mockResolvedValue(full(BAI_NHAP, 'PUBLISHED'))
    renderPage()

    const nhap = await rowOf('Máy tính là gì')
    fireEvent.click(nhap.getByRole('button', { name: 'Đăng bài Máy tính là gì' }))

    await waitFor(() => expect(setLessonStatusMock).toHaveBeenCalledWith(3, 'PUBLISHED'))
    expect(await nhap.findByText('Đã đăng')).not.toBeNull()
    expect(nhap.getByRole('button', { name: 'Ẩn bài Máy tính là gì' })).not.toBeNull()
    expect(nhap.queryByRole('button', { name: 'Đăng bài Máy tính là gì' })).toBeNull()
  })

  it('bài đã đăng có nút Ẩn bài; bấm thì gọi API với DRAFT', async () => {
    setLessonStatusMock.mockResolvedValue(full(BAI_DANG, 'DRAFT'))
    renderPage()

    const dang = await rowOf('Phần mềm')
    fireEvent.click(dang.getByRole('button', { name: 'Ẩn bài Phần mềm' }))

    await waitFor(() => expect(setLessonStatusMock).toHaveBeenCalledWith(4, 'DRAFT'))
    expect(await dang.findByText('Nháp')).not.toBeNull()
  })

  it('đổi trạng thái lỗi thì hiện câu lỗi và giữ nguyên trạng thái cũ', async () => {
    setLessonStatusMock.mockRejectedValue(new ApiError(404, 'Không tìm thấy bài học', { detail: 'Không tìm thấy bài học' }))
    renderPage()

    const nhap = await rowOf('Máy tính là gì')
    fireEvent.click(nhap.getByRole('button', { name: 'Đăng bài Máy tính là gì' }))

    expect((await nhap.findByRole('alert')).textContent).toContain('Không tìm thấy bài học')
    expect(nhap.getByText('Nháp')).not.toBeNull()
  })
})

describe('AdminLessonsPage — xóa bài', () => {
  it('phải xác nhận mới xóa: bấm Xóa chỉ hỏi, Xác nhận xóa mới gọi API rồi tải lại và báo đã xóa', async () => {
    deleteLessonMock.mockResolvedValue(undefined)
    renderPage()

    const nhap = await rowOf('Máy tính là gì')
    fireEvent.click(nhap.getByRole('button', { name: 'Xóa Máy tính là gì' }))
    expect(nhap.getByText(/Xóa bài học "Máy tính là gì"\?/)).not.toBeNull()
    expect(deleteLessonMock).not.toHaveBeenCalled()

    fireEvent.click(nhap.getByRole('button', { name: 'Xác nhận xóa' }))

    await waitFor(() => expect(deleteLessonMock).toHaveBeenCalledWith(3))
    expect((await screen.findByRole('status')).textContent).toContain('Đã xóa bài học')
    await waitFor(() => expect(fetchAdminLessonsMock).toHaveBeenCalledTimes(2))
  })

  it('Hủy ở bước xác nhận thì không xóa', async () => {
    renderPage()

    const nhap = await rowOf('Máy tính là gì')
    fireEvent.click(nhap.getByRole('button', { name: 'Xóa Máy tính là gì' }))
    fireEvent.click(nhap.getByRole('button', { name: 'Hủy' }))

    expect(deleteLessonMock).not.toHaveBeenCalled()
    expect(nhap.queryByRole('button', { name: 'Xác nhận xóa' })).toBeNull()
  })

  it('xóa lỗi thì hiện câu lỗi và bài vẫn còn', async () => {
    deleteLessonMock.mockRejectedValue(new ApiError(500, 'Lỗi'))
    renderPage()

    const nhap = await rowOf('Máy tính là gì')
    fireEvent.click(nhap.getByRole('button', { name: 'Xóa Máy tính là gì' }))
    fireEvent.click(nhap.getByRole('button', { name: 'Xác nhận xóa' }))

    expect((await nhap.findByRole('alert')).textContent).toContain('Máy chủ đang gặp lỗi')
    expect(fetchAdminLessonsMock).toHaveBeenCalledTimes(1)
  })
})
