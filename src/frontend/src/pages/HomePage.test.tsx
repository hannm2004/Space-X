import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchCatalog, type CatalogCourse } from '../catalogApi'
import { ApiError } from '../client'
import { fetchProgress, type CourseProgress } from '../learningApi'
import { AuthContext, type AuthState } from '../useAuth'
import HomePage from './HomePage'

vi.mock('../catalogApi', () => ({ fetchCatalog: vi.fn(), fetchCategories: vi.fn() }))
vi.mock('../learningApi', () => ({ fetchProgress: vi.fn() }))
vi.mock('../api', () => ({ fetchHealth: vi.fn().mockResolvedValue('UP') }))

const fetchCatalogMock = vi.mocked(fetchCatalog)
const fetchProgressMock = vi.mocked(fetchProgress)

const ANONYMOUS: AuthState = { status: 'anonymous' }
const SIGNED_IN: AuthState = {
  status: 'authenticated',
  user: { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' },
}

const THIEU_NIEN = { id: 1, slug: 'thieu-nien', name: 'Khóa cho thiếu niên' }
const VAN_PHONG = { id: 4, slug: 'van-phong', name: 'Tin học văn phòng' }

function khoa(id: number, slug: string, title: string, them: Partial<CatalogCourse> = {}): CatalogCourse {
  return {
    id,
    slug,
    title,
    description: null,
    lessonCount: 3,
    category: THIEU_NIEN,
    level: 'BASIC',
    totalHours: 0,
    tuitionVnd: 0,
    ...them,
  }
}

const DANH_SACH = [
  khoa(1, 'tin-hoc-10', 'Tin học 10', { description: 'Khóa lớp 10', lessonCount: 5 }),
  khoa(2, 'tin-hoc-11', 'Tin học 11', { lessonCount: 0 }),
  khoa(3, 'excel', 'Excel nâng cao', {
    category: VAN_PHONG,
    level: 'ADVANCED',
    totalHours: 40,
    tuitionVnd: 1500000,
    lessonCount: 7,
  }),
]

function renderHome(auth: AuthState = ANONYMOUS) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ state: auth, signIn: vi.fn(), signOut: vi.fn(), updateUser: vi.fn() }}>
        <HomePage />
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

const theKhoa = async (ten: string) => (await screen.findByRole('link', { name: new RegExp(ten) })) as HTMLElement

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
  fetchCatalogMock.mockReset()
  fetchCatalogMock.mockResolvedValue(DANH_SACH)
  fetchProgressMock.mockReset()
  fetchProgressMock.mockResolvedValue([])
})

describe('HomePage', () => {
  it('tiêu đề chính giới thiệu trung tâm SPACE-X', () => {
    fetchCatalogMock.mockReturnValue(new Promise(() => {}))

    renderHome()

    expect(screen.getByRole('heading', { level: 1, name: 'Khóa học Tin học tại SPACE-X' })).not.toBeNull()
    expect(screen.getByText('Đang tải danh sách khóa học...')).not.toBeNull()
  })

  it('không còn nội dung của đề tài cũ: chọn lớp, lớp 6 đến lớp 12, GDPT, miễn phí', async () => {
    const { container } = renderHome()
    await theKhoa('Tin học 10')

    expect(screen.queryByRole('navigation', { name: 'Chọn lớp' })).toBeNull()
    expect(container.textContent).not.toMatch(/lớp 6|lớp 12|GDPT|miễn phí|THCS|THPT/i)
  })

  it('hiện từng khóa có tên, mô tả, liên kết và ảnh bìa', async () => {
    fetchCatalogMock.mockResolvedValue([
      khoa(1, 'a b/c', 'Khóa lạ', { description: 'Mô tả lạ', coverImageUrl: '/api/courses/x/cover?v=1' }),
    ])

    renderHome()

    const the = await theKhoa('Khóa lạ')
    expect(the.getAttribute('href')).toBe('/khoa-hoc/a%20b%2Fc')
    expect(within(the).getByText('Mô tả lạ')).not.toBeNull()
    expect(the.querySelector('img')?.getAttribute('src')).toBe('/api/courses/x/cover?v=1')
  })

  it('thẻ khóa ghi nhóm, trình độ, số giờ và học phí; chưa có học phí thì ghi đang cập nhật', async () => {
    renderHome()

    const excel = await theKhoa('Excel nâng cao')
    expect(within(excel).getByText('Tin học văn phòng')).not.toBeNull()
    expect(within(excel).getByText('Nâng cao')).not.toBeNull()
    expect(within(excel).getByText('40 giờ')).not.toBeNull()
    expect(within(excel).getByText('1.500.000 đ')).not.toBeNull()
    const tin10 = await theKhoa('Tin học 10')
    expect(within(tin10).getByText('Cơ bản')).not.toBeNull()
    expect(within(tin10).getByText('Học phí đang cập nhật')).not.toBeNull()
    expect(within(tin10).queryByText('0 giờ')).toBeNull()
  })

  it('thẻ khóa ghi số bài đã đăng; khóa chưa có bài ghi "Sắp có bài"', async () => {
    renderHome()

    expect(within(await theKhoa('Tin học 10')).getByText('5 bài')).not.toBeNull()
    const chua = await theKhoa('Tin học 11')
    expect(within(chua).getByText('Sắp có bài')).not.toBeNull()
    expect(within(chua).queryByText('0 bài')).toBeNull()
  })

  it('hàng số liệu tính từ dữ liệu thật và đọc đúng thứ tự thuật ngữ rồi định nghĩa', async () => {
    renderHome()

    for (const so of ['3 khóa học', '2 nhóm chương trình', '12 bài học']) {
      const dd = await screen.findByText(so)
      expect(dd.tagName).toBe('DD')
      expect(dd.previousElementSibling?.tagName).toBe('DT')
    }
  })

  it('chọn nhóm chương trình thì chỉ hiện khóa của nhóm đó, chọn "Tất cả" thì hiện lại', async () => {
    renderHome()
    await theKhoa('Tin học 10')
    const nhom = within(screen.getByRole('group', { name: 'Chọn nhóm chương trình' }))
    expect(nhom.getByRole('button', { name: 'Tất cả' }).getAttribute('aria-pressed')).toBe('true')

    fireEvent.click(nhom.getByRole('button', { name: 'Tin học văn phòng' }))

    expect(nhom.getByRole('button', { name: 'Tin học văn phòng' }).getAttribute('aria-pressed')).toBe('true')
    expect(screen.queryByRole('link', { name: /Tin học 10/ })).toBeNull()
    expect(screen.getByRole('link', { name: /Excel nâng cao/ })).not.toBeNull()

    fireEvent.click(nhom.getByRole('button', { name: 'Tất cả' }))
    expect(screen.getByRole('link', { name: /Tin học 10/ })).not.toBeNull()
  })

  it('liên kết tới danh sách đầy đủ mang theo nhóm đang chọn', async () => {
    renderHome()
    await theKhoa('Tin học 10')
    const lienKet = () => screen.getByRole('link', { name: 'Lọc và so sánh khóa học' }).getAttribute('href')
    expect(lienKet()).toBe('/danh-sach-khoa-hoc')

    fireEvent.click(screen.getByRole('button', { name: 'Tin học văn phòng' }))

    expect(lienKet()).toBe('/danh-sach-khoa-hoc?nhom=van-phong')
  })

  it('chưa có khóa nào thì báo rõ, không để trống', async () => {
    fetchCatalogMock.mockResolvedValue([])

    renderHome()

    expect(await screen.findByText('Chưa có khóa học nào.')).not.toBeNull()
    expect(screen.queryByRole('group', { name: 'Chọn nhóm chương trình' })).toBeNull()
  })

  it('lỗi tải thì báo lỗi và bấm Thử lại sẽ tải lại', async () => {
    fetchCatalogMock.mockRejectedValueOnce(new ApiError(0, 'Không kết nối được tới máy chủ'))

    renderHome()

    expect(await screen.findByText('Không tải được danh sách khóa học.')).not.toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await theKhoa('Tin học 10')).not.toBeNull()
    expect(fetchCatalogMock).toHaveBeenCalledTimes(2)
  })

  it('có phần "Học liệu trực tuyến" nêu những thứ đang có thật: bài viết, tiến độ, tìm kiếm', async () => {
    renderHome()

    const vung = await screen.findByRole('region', { name: 'Học liệu trực tuyến' })
    expect(within(vung).getByText(/Bài viết ngắn gọn/)).not.toBeNull()
    expect(within(vung).getByText(/Lưu tiến độ/)).not.toBeNull()
    expect(within(vung).getByText(/Tìm kiếm/)).not.toBeNull()
  })

  it('câu hỏi thường gặp nói đúng hiện trạng: trung tâm hư cấu, chưa mở đăng ký, không thu tiền thật', async () => {
    const { container } = renderHome()

    const vung = await screen.findByRole('region', { name: 'Câu hỏi thường gặp' })
    expect(container.querySelector('#hoi-dap')).not.toBeNull()
    expect(vung.querySelectorAll('details')).toHaveLength(6)
    expect(within(vung).getByText('Trung tâm SPACE-X có thật không?')).not.toBeNull()
    expect(within(vung).getByText('Đăng ký lớp và đóng học phí thế nào?')).not.toBeNull()
    expect(vung.textContent).toContain('đang được xây dựng')
    expect(vung.textContent).toContain('không thu tiền thật')
    expect(within(vung).getByRole('link', { name: 'Chính sách bảo mật' }).getAttribute('href')).toBe(
      '/chinh-sach-bao-mat',
    )
  })

  it('khách thấy lời mời tạo tài khoản, người đã đăng nhập thì không', async () => {
    const { unmount } = renderHome(ANONYMOUS)
    expect((await screen.findByRole('link', { name: 'Tạo tài khoản' })).getAttribute('href')).toBe('/dang-ky')
    unmount()

    renderHome(SIGNED_IN)
    await theKhoa('Tin học 10')
    expect(screen.queryByRole('link', { name: 'Tạo tài khoản' })).toBeNull()
  })
})

describe('HomePage — tiến độ học', () => {
  it('khách chưa đăng nhập: không gọi API tiến độ và không có thanh tiến độ', async () => {
    renderHome(ANONYMOUS)

    await theKhoa('Tin học 10')
    expect(fetchProgressMock).not.toHaveBeenCalled()
    expect(screen.queryByRole('progressbar')).toBeNull()
  })

  it('đã đăng nhập: khóa có tiến độ hiện thanh tiến độ và số bài đã hoàn thành của đúng khóa đó', async () => {
    fetchProgressMock.mockResolvedValue([
      tienDo(1, 'tin-hoc-10', 'Tin học 10', 1, 3),
      tienDo(2, 'tin-hoc-11', 'Tin học 11', 0, 0),
    ])

    renderHome(SIGNED_IN)

    const thanh = await screen.findByRole('progressbar', { name: 'Tiến độ Tin học 10' })
    expect(thanh.getAttribute('aria-valuenow')).toBe('33')
    expect(screen.getByText('1/3 bài hoàn thành')).not.toBeNull()
    // Khóa chưa có bài đã đăng thì không có thanh tiến độ.
    expect(screen.queryByRole('progressbar', { name: 'Tiến độ Tin học 11' })).toBeNull()
    expect(fetchProgressMock).toHaveBeenCalledTimes(1)
  })

  it('không tải được tiến độ thì danh sách khóa vẫn hiện bình thường, không báo lỗi chặn trang', async () => {
    fetchProgressMock.mockRejectedValue(new ApiError(500, 'SQLException: bí mật nội bộ'))

    renderHome(SIGNED_IN)

    expect(await theKhoa('Tin học 10')).not.toBeNull()
    await vi.waitFor(() => expect(fetchProgressMock).toHaveBeenCalled())
    expect(screen.queryByRole('progressbar')).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(document.body.textContent).not.toContain('SQLException')
  })
})
