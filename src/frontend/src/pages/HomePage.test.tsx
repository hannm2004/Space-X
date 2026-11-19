import { fireEvent, render, screen, within } from '@testing-library/react'
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
      <AuthContext.Provider value={{ state: auth, signIn: vi.fn(), signOut: vi.fn(), updateUser: vi.fn() }}>
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

    expect(screen.getByRole('heading', { level: 1, name: 'Học Tin học từ lớp 6 đến lớp 12' })).not.toBeNull()
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

const KHOA_NHIEU_LOP = [
  { id: 1, slug: 'tin-hoc-6', title: 'Tin học 6', description: 'Mô tả lớp 6' },
  { id: 2, slug: 'tin-hoc-9', title: 'Tin học 9', description: null },
  { id: 3, slug: 'tin-hoc-10', title: 'Tin học 10', description: 'Mô tả lớp 10' },
  { id: 4, slug: 'tin-hoc-12', title: 'Tin học 12', description: null },
]

describe('HomePage — bố cục như trang khóa học thực tế', () => {
  it('chia khóa học thành hai nhóm THCS và THPT theo số lớp trong slug', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA_NHIEU_LOP)

    renderHome()

    const thcs = await screen.findByRole('region', { name: /Trung học cơ sở/ })
    const thpt = screen.getByRole('region', { name: /Trung học phổ thông/ })
    expect(within(thcs).getAllByRole('link').map((a) => a.getAttribute('href'))).toEqual([
      '/khoa-hoc/tin-hoc-6',
      '/khoa-hoc/tin-hoc-9',
    ])
    expect(within(thpt).getAllByRole('link').map((a) => a.getAttribute('href'))).toEqual([
      '/khoa-hoc/tin-hoc-10',
      '/khoa-hoc/tin-hoc-12',
    ])
  })

  it('khóa có slug không theo dạng tin-hoc-<lớp> vẫn hiện, ở nhóm "Khác"', async () => {
    fetchCoursesMock.mockResolvedValue([
      ...KHOA_NHIEU_LOP,
      { id: 9, slug: 'on-thi-hsg', title: 'Ôn thi HSG', description: null },
    ])

    renderHome()

    const khac = await screen.findByRole('region', { name: /Khác/ })
    expect(within(khac).getByRole('link', { name: /Ôn thi HSG/ }).getAttribute('href')).toBe('/khoa-hoc/on-thi-hsg')
  })

  it('mỗi thẻ khóa có số lớp lớn và nhãn cấp học', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA_NHIEU_LOP)

    renderHome()

    const the = (await screen.findByRole('link', { name: /Tin học 10/ })) as HTMLElement
    expect(within(the).getByText('THPT')).not.toBeNull()
    expect(within(the).getByText('Lớp 10')).not.toBeNull()
  })

  it('phần mở đầu có nút chọn lớp dẫn thẳng tới từng khóa', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA_NHIEU_LOP)

    renderHome()

    const nav = await screen.findByRole('navigation', { name: 'Chọn lớp' })
    expect(within(nav).getByRole('link', { name: 'Lớp 6' }).getAttribute('href')).toBe('/khoa-hoc/tin-hoc-6')
    expect(within(nav).getByRole('link', { name: 'Lớp 12' }).getAttribute('href')).toBe('/khoa-hoc/tin-hoc-12')
  })

  it('số khóa học trong phần giới thiệu tính từ dữ liệu thật, không ghi cứng', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA_NHIEU_LOP)

    renderHome()

    expect(await screen.findByText('4 khóa học')).not.toBeNull()
  })

  it('có phần "Mỗi bài học có gì" nêu những thứ có thật: kiểm tra nhanh, tiến độ, tìm kiếm', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA_NHIEU_LOP)

    renderHome()

    const vung = await screen.findByRole('region', { name: 'Mỗi bài học có gì' })
    expect(within(vung).getByText(/Kiểm tra nhanh/)).not.toBeNull()
    expect(within(vung).getByText(/tiến độ/i)).not.toBeNull()
    expect(within(vung).getByText(/Tìm kiếm/)).not.toBeNull()
  })

  it('có phần câu hỏi thường gặp với đủ sáu câu, mỗi câu mở ra được câu trả lời', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA_NHIEU_LOP)

    const { container } = renderHome()

    const vung = await screen.findByRole('region', { name: 'Câu hỏi thường gặp' })
    expect(container.querySelector('#hoi-dap')).not.toBeNull()
    expect(within(vung).getByText('Học trên Space-X có mất phí không?')).not.toBeNull()
    expect(within(vung).getByText('Có cần tạo tài khoản không?')).not.toBeNull()
    expect(within(vung).getByText('Quên mật khẩu thì làm thế nào?')).not.toBeNull()
    expect(vung.querySelectorAll('details')).toHaveLength(6)
    expect(within(vung).getByRole('link', { name: 'Chính sách bảo mật' }).getAttribute('href')).toBe(
      '/chinh-sach-bao-mat',
    )
  })

  it('khách thấy lời mời đăng ký miễn phí, người đã đăng nhập thì không', async () => {
    fetchCoursesMock.mockResolvedValue(KHOA_NHIEU_LOP)
    const { unmount } = renderHome(ANONYMOUS)
    expect((await screen.findByRole('link', { name: 'Đăng ký miễn phí' })).getAttribute('href')).toBe('/dang-ky')
    unmount()

    renderHome(SIGNED_IN)
    await screen.findByRole('region', { name: /Trung học cơ sở/ })
    expect(screen.queryByRole('link', { name: 'Đăng ký miễn phí' })).toBeNull()
  })
})
