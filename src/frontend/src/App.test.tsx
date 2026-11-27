import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { fetchHealth } from './api'
import { fetchCurrentUser, fetchProfile, logout } from './authApi'
import { fetchCatalog, fetchCategories } from './catalogApi'
import { ApiError } from './client'
import { fetchCourses, fetchLesson, fetchLessons, fetchProgress, recordProgress, searchLessons } from './learningApi'

vi.mock('./api', () => ({ fetchHealth: vi.fn() }))
vi.mock('./authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
  fetchProfile: vi.fn(),
  updateDisplayName: vi.fn(),
}))
vi.mock('./catalogApi', () => ({ fetchCatalog: vi.fn(), fetchCategories: vi.fn() }))
vi.mock('./learningApi', () => ({
  fetchCourses: vi.fn(),
  fetchLessons: vi.fn(),
  fetchLesson: vi.fn(),
  searchLessons: vi.fn(),
  fetchProgress: vi.fn(),
  recordProgress: vi.fn(),
}))

beforeEach(() => {
  vi.mocked(fetchCurrentUser).mockReset()
  vi.mocked(fetchCurrentUser).mockResolvedValue(null)
  vi.mocked(logout).mockReset()
  vi.mocked(logout).mockResolvedValue(undefined)
  vi.mocked(fetchHealth).mockResolvedValue('UP')
  vi.mocked(fetchCatalog).mockResolvedValue([])
  vi.mocked(fetchCategories).mockResolvedValue([{ id: 4, slug: 'van-phong', name: 'Tin học văn phòng' }])
  vi.mocked(fetchCourses).mockResolvedValue([
    { id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: null, lessonCount: 1 },
  ])
  vi.mocked(fetchLessons).mockResolvedValue([
    { id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: null },
  ])
  vi.mocked(fetchLesson).mockResolvedValue({
    id: 1,
    slug: 'thong-tin-va-du-lieu',
    title: 'Thông tin và dữ liệu',
    summary: null,
    contentMd: 'Nội dung bài',
    course: { slug: 'tin-hoc-10', title: 'Tin học 10' },
    previous: null,
    next: null,
  })
  vi.mocked(fetchProgress).mockResolvedValue([])
  vi.mocked(recordProgress).mockResolvedValue({ lessonId: 1, status: 'IN_PROGRESS' })
  vi.stubGlobal(
    'matchMedia',
    vi.fn().mockReturnValue({ matches: false }),
  )
})

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App — định tuyến', () => {
  it('đường dẫn / hiện trang chủ', async () => {
    renderAt('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Khóa học Tin học tại SPACE-X' })).not.toBeNull()
    expect(await screen.findByText('Hệ thống hoạt động bình thường')).not.toBeNull()
  })

  it('/khoa-hoc/{slug} hiện trang khóa học với danh sách bài', async () => {
    renderAt('/khoa-hoc/tin-hoc-10')

    expect(await screen.findByRole('heading', { level: 1, name: 'Tin học 10' })).not.toBeNull()
    expect(screen.getByRole('link', { name: /Thông tin và dữ liệu/ })).not.toBeNull()
  })

  it('/bai/{slug} hiện trang đọc bài', async () => {
    renderAt('/bai/thong-tin-va-du-lieu')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Thông tin và dữ liệu' }),
    ).not.toBeNull()
    expect(screen.getByText('Nội dung bài')).not.toBeNull()
  })

  it('từ trang chủ vào khóa, vào bài, rồi quay lại đúng khóa đó', async () => {
    vi.mocked(fetchCatalog).mockResolvedValue([
      {
        id: 1,
        slug: 'tin-hoc-10',
        title: 'Tin học 10',
        description: null,
        lessonCount: 1,
        category: { id: 1, slug: 'thieu-nien', name: 'Khóa cho thiếu niên' },
        level: 'BASIC',
        totalHours: 0,
        tuitionVnd: 0,
      },
    ])
    renderAt('/')

    fireEvent.click(await screen.findByRole('link', { name: /Tin học 10/ }))
    fireEvent.click(await screen.findByRole('link', { name: /Thông tin và dữ liệu/ }))
    expect(await screen.findByText('Nội dung bài')).not.toBeNull()

    // Đường dẫn trên trang bài đưa về đúng khóa; khóa lấy từ API nên không phụ thuộc việc đi từ đâu tới.
    const duongDan = screen.getByRole('navigation', { name: 'Đường dẫn' })
    fireEvent.click(within(duongDan).getByRole('link', { name: 'Tin học 10' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Tin học 10' })).not.toBeNull()
  })

  it('đường dẫn không tồn tại hiện trang "Không tìm thấy trang" kèm liên kết về trang chủ', () => {
    renderAt('/khong-co-trang-nay')

    expect(screen.getByRole('heading', { level: 1, name: 'Không tìm thấy trang' })).not.toBeNull()
    const home = screen.getByRole('link', { name: 'Về trang chủ' })
    expect(home.getAttribute('href')).toBe('/')
  })

  it('mọi trang đều có thanh đầu trang với liên kết SPACE-X về trang chủ và nút đổi giao diện', () => {
    renderAt('/khong-co-trang-nay')

    const brand = within(screen.getByRole('banner')).getByRole('link', { name: /SPACE-X/ })
    expect(brand.getAttribute('href')).toBe('/')
    expect(screen.getByRole('button', { name: 'Chuyển sang giao diện tối' })).not.toBeNull()
  })

  it('thương hiệu ở đầu trang: chữ SPACE-X và dòng phụ "Trung tâm Tin học"', () => {
    renderAt('/khong-co-trang-nay')

    const brand = within(screen.getByRole('banner')).getByRole('link', { name: /SPACE-X/ })
    expect(brand.textContent).toContain('SPACE-X')
    expect(brand.textContent).toMatch(/Trung tâm Tin học/i)
    // Tên cũ "Space-X" (viết hoa chữ đầu) không còn.
    expect(brand.textContent).not.toContain('Space-X')
  })

  it('logo có hai bản, bản nền sáng ẩn khi chế độ tối và ngược lại; cả hai là hình trang trí', () => {
    renderAt('/khong-co-trang-nay')

    const brand = within(screen.getByRole('banner')).getByRole('link', { name: /SPACE-X/ })
    expect(brand.querySelectorAll('img')).toHaveLength(2)
    const light = brand.querySelector('img[src="/logo-mark.svg"]')!
    const dark = brand.querySelector('img[src="/logo-mark-on-dark.svg"]')!
    expect(light).not.toBeNull()
    expect(dark).not.toBeNull()
    // Tên đã có bằng chữ ngay bên cạnh nên ảnh để alt rỗng, trình đọc màn hình không đọc hai lần.
    expect(light.getAttribute('alt')).toBe('')
    expect(dark.getAttribute('alt')).toBe('')
    // Đổi sáng/tối bằng class `dark` trên <html> (ThemeToggle), không cần JavaScript thêm.
    expect(light.classList.contains('dark:hidden')).toBe(true)
    expect(dark.classList.contains('hidden')).toBe(true)
    expect(dark.classList.contains('dark:block')).toBe(true)
    // Có sẵn width/height để trình duyệt giữ chỗ, trang không nhảy khi ảnh tải xong.
    for (const img of [light, dark]) {
      expect(img.getAttribute('width')).toBeTruthy()
      expect(img.getAttribute('height')).toBeTruthy()
    }
  })

  it('chân trang cũng dùng logo mới và tên SPACE-X, không còn biểu tượng ngoặc nhọn cũ', async () => {
    renderAt('/khong-co-trang-nay')

    const footer = await screen.findByRole('contentinfo')
    expect(footer.querySelector('img[src="/logo-mark.svg"]')).not.toBeNull()
    expect(footer.querySelector('img[src="/logo-mark-on-dark.svg"]')).not.toBeNull()
    expect(within(footer).getByText('SPACE-X')).not.toBeNull()
    // Biểu tượng cũ là một <svg> nằm trong khối thương hiệu; logo mới là <img>.
    expect(footer.querySelector('svg path[d="M9 7l-5 5 5 5M15 7l5 5-5 5"]')).toBeNull()
  })

  it('có liên kết "Bỏ qua tới nội dung" là mục đầu tiên để người dùng bàn phím khỏi Tab qua cả thanh đầu trang', () => {
    renderAt('/khong-co-trang-nay')

    const skip = screen.getByRole('link', { name: 'Bỏ qua tới nội dung' })
    expect(skip.getAttribute('href')).toBe('#noi-dung')
    // Đích của liên kết là vùng nội dung chính và nhận được focus khi bấm.
    const main = screen.getByRole('main')
    expect(main.id).toBe('noi-dung')
    expect(main.getAttribute('tabindex')).toBe('-1')
    // Là phần tử đầu tiên trong trang nên Tab một lần là tới.
    expect(document.body.querySelector('a, button')).toBe(skip)
  })

  it('tên thương hiệu SPACE-X không bị trình duyệt dịch tự động', () => {
    renderAt('/khong-co-trang-nay')

    const brand = within(screen.getByRole('banner')).getByRole('link', { name: /SPACE-X/ })
    expect(brand.querySelector('[translate="no"]')).not.toBeNull()
  })

  it('tìm bài từ trang chủ: nhập từ khóa, bấm Tìm, sang trang kết quả và thấy bài khớp', async () => {
    vi.mocked(searchLessons).mockResolvedValue({
      items: [{ id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: null }],
      page: 0,
      size: 20,
      totalItems: 1,
    })
    renderAt('/')

    fireEvent.change(await screen.findByRole('searchbox', { name: 'Từ khóa' }), {
      target: { value: 'thông tin' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Tìm bài học' })).not.toBeNull()
    expect(await screen.findByText('Tìm thấy 1 bài')).not.toBeNull()
    expect(searchLessons).toHaveBeenCalledWith('thông tin', 0)
  })

  it('/tim-kiem hiện trang tìm kiếm và thanh đầu trang có nút kính lúp mở ô tìm', async () => {
    renderAt('/tim-kiem')

    expect(await screen.findByRole('heading', { level: 1, name: 'Tìm bài học' })).not.toBeNull()
    const header = screen.getByRole('banner')
    const search = within(header).getByRole('button', { name: 'Tìm bài học' })
    expect(search.getAttribute('title')).toBe('Tìm bài học')
    // Kính lúp chỉ để trang trí: tên của nút đã nằm ở aria-label.
    expect(search.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true')
    expect(within(header).queryByText('Tìm bài')).toBeNull()
  })

  it('nút kính lúp có ở mọi trang; bấm vào thì ô tìm hiện ngay trong thanh đầu trang', async () => {
    renderAt('/dang-nhap')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
    const header = screen.getByRole('banner')
    fireEvent.click(within(header).getByRole('button', { name: 'Tìm bài học' }))

    expect(within(header).getByRole('combobox', { name: 'Tìm bài học' })).not.toBeNull()
  })

  it('gõ ở thanh đầu trang rồi Enter thì sang trang kết quả với đúng từ khóa', async () => {
    vi.mocked(searchLessons).mockResolvedValue({
      items: [{ id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: null }],
      page: 0,
      size: 20,
      totalItems: 1,
    })
    renderAt('/dang-nhap')

    const header = await screen.findByRole('banner')
    fireEvent.click(within(header).getByRole('button', { name: 'Tìm bài học' }))
    const input = within(header).getByRole('combobox', { name: 'Tìm bài học' })
    fireEvent.change(input, { target: { value: 'thông tin' } })
    fireEvent.submit(input.closest('form') as HTMLFormElement)

    expect(await screen.findByText('Tìm thấy 1 bài')).not.toBeNull()
    expect(searchLessons).toHaveBeenCalledWith('thông tin', 0)
  })

  it('/dang-nhap, /dang-ky hiện đúng trang', async () => {
    renderAt('/dang-nhap')
    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()

    cleanup()
    renderAt('/dang-ky')
    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng ký' })).not.toBeNull()
  })
})

describe('App — thanh điều hướng theo trạng thái đăng nhập', () => {
  const USER = { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' as const }

  it('chưa biết trạng thái (đang hỏi /api/me) thì chưa hiện liên kết tài khoản nào, tránh nháy', () => {
    vi.mocked(fetchCurrentUser).mockReturnValue(new Promise(() => {}))

    renderAt('/khong-co-trang-nay')

    expect(screen.queryByRole('link', { name: 'Đăng nhập' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Đăng xuất' })).toBeNull()
  })

  it('chưa đăng nhập thì hiện "Đăng nhập" và "Đăng ký", không có "Đăng xuất"', async () => {
    renderAt('/khong-co-trang-nay')

    const header = await screen.findByRole('banner')
    expect(await within(header).findByRole('link', { name: 'Đăng nhập' })).not.toBeNull()
    expect(within(header).getByRole('link', { name: 'Đăng ký' })).not.toBeNull()
    expect(within(header).queryByRole('button', { name: 'Đăng xuất' })).toBeNull()
  })

  it('đã đăng nhập thì tên là liên kết sang hồ sơ, có "Đăng xuất", không còn "Đăng nhập"', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)

    renderAt('/khong-co-trang-nay')

    const header = await screen.findByRole('banner')
    expect((await within(header).findByRole('link', { name: 'Nguyễn Văn A' })).getAttribute('href')).toBe('/ho-so')
    expect(within(header).getByRole('button', { name: 'Đăng xuất' })).not.toBeNull()
    expect(within(header).queryByRole('link', { name: 'Đăng nhập' })).toBeNull()
  })

  it('"Đổi mật khẩu" không còn ở thanh đầu trang (nằm trong trang Hồ sơ, chỉ cho tài khoản có mật khẩu)', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)

    renderAt('/khong-co-trang-nay')

    const header = await screen.findByRole('banner')
    await within(header).findByRole('link', { name: 'Nguyễn Văn A' })
    expect(within(header).queryByRole('link', { name: 'Đổi mật khẩu' })).toBeNull()
  })

  it('Admin chỉ thấy một liên kết "Quản trị" trên thanh đầu trang, các mục quản lý nằm ở thanh riêng của khu quản trị', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({ ...USER, role: 'ADMIN' })

    renderAt('/khong-co-trang-nay')

    const header = await screen.findByRole('banner')
    expect((await within(header).findByRole('link', { name: 'Quản trị' })).getAttribute('href')).toBe('/quan-tri')
    expect(within(header).queryByRole('link', { name: 'Quản lý người dùng' })).toBeNull()
    expect(within(header).queryByRole('link', { name: 'Quản lý khóa học' })).toBeNull()
    expect(within(header).queryByRole('link', { name: 'Quản lý bài học' })).toBeNull()
  })

  it('học sinh và khách không thấy liên kết "Quản trị"', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)
    renderAt('/khong-co-trang-nay')
    const header = await screen.findByRole('banner')
    await within(header).findByText('Nguyễn Văn A')
    expect(within(header).queryByRole('link', { name: 'Quản trị' })).toBeNull()

    cleanup()
    vi.mocked(fetchCurrentUser).mockResolvedValue(null)
    renderAt('/khong-co-trang-nay')
    const guestHeader = await screen.findByRole('banner')
    await within(guestHeader).findByRole('link', { name: 'Đăng nhập' })
    expect(within(guestHeader).queryByRole('link', { name: 'Quản trị' })).toBeNull()
  })

  it('thanh đầu trang có điều hướng chính với liên kết "Khóa học" tới danh sách khóa', async () => {
    renderAt('/khong-co-trang-nay')

    const nav = within(await screen.findByRole('banner')).getByRole('navigation', { name: 'Chính' })
    expect(within(nav).getByRole('link', { name: 'Khóa học' }).getAttribute('href')).toBe('/danh-sach-khoa-hoc')
  })

  it('/danh-sach-khoa-hoc hiện trang danh sách và đánh dấu mục "Khóa học" đang mở', async () => {
    renderAt('/danh-sach-khoa-hoc')

    expect(await screen.findByRole('heading', { level: 1, name: 'Danh sách khóa học' })).not.toBeNull()
    const nav = within(screen.getByRole('banner')).getByRole('navigation', { name: 'Chính' })
    expect(within(nav).getByRole('link', { name: 'Khóa học' }).getAttribute('aria-current')).toBe('page')
  })

  it('trạng thái hệ thống nằm ở chân trang của mọi trang, không nằm trong nội dung trang chủ', async () => {
    renderAt('/khong-co-trang-nay')

    const footer = await screen.findByRole('contentinfo')
    expect(await within(footer).findByText('Hệ thống hoạt động bình thường')).not.toBeNull()
    cleanup()

    renderAt('/')
    const main = await screen.findByRole('main')
    await within(screen.getByRole('contentinfo')).findByText('Hệ thống hoạt động bình thường')
    expect(within(main).queryByText('Hệ thống hoạt động bình thường')).toBeNull()
  })

  it('bấm "Đăng xuất" gọi API rồi trở về trạng thái chưa đăng nhập và trang chủ', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)
    renderAt('/doi-mat-khau')

    fireEvent.click(await screen.findByRole('button', { name: 'Đăng xuất' }))

    expect(await screen.findByRole('link', { name: 'Đăng nhập' })).not.toBeNull()
    expect(logout).toHaveBeenCalledTimes(1)
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Khóa học Tin học tại SPACE-X' }),
    ).not.toBeNull()
    expect(screen.queryByRole('heading', { level: 1, name: 'Đăng nhập' })).toBeNull()
  })

  it('đăng xuất lỗi (mất kết nối) thì vẫn giữ trạng thái đăng nhập và báo lỗi', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)
    vi.mocked(logout).mockRejectedValue(new ApiError(0, 'Không kết nối được tới máy chủ'))
    renderAt('/khong-co-trang-nay')

    fireEvent.click(await screen.findByRole('button', { name: 'Đăng xuất' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Không đăng xuất được')
    expect(screen.getByRole('button', { name: 'Đăng xuất' })).not.toBeNull()
  })

  it('/doi-mat-khau khi chưa đăng nhập chuyển sang trang đăng nhập', async () => {
    renderAt('/doi-mat-khau')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
  })

  it('/doi-mat-khau khi đã đăng nhập hiện trang đổi mật khẩu', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)

    renderAt('/doi-mat-khau')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đổi mật khẩu' })).not.toBeNull()
  })

  it('/ho-so khi chưa đăng nhập chuyển sang trang đăng nhập', async () => {
    renderAt('/ho-so')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
  })

  it('/ho-so khi đã đăng nhập hiện trang hồ sơ cá nhân', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)
    vi.mocked(fetchProfile).mockResolvedValue({ ...USER, authProvider: 'LOCAL', createdAt: '2026-11-01T09:30:00' })

    renderAt('/ho-so')

    expect(await screen.findByRole('heading', { level: 1, name: 'Hồ sơ cá nhân' })).not.toBeNull()
  })
})

describe('App — quản trị nội dung (CP6.4)', () => {
  const USER = { displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' as const }

  it('Admin ở khu quản trị thấy thanh "Quản trị" với đủ bốn mục, mục đang mở được đánh dấu', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({ ...USER, role: 'ADMIN' })

    renderAt('/quan-tri/khoa-hoc')

    const nav = await screen.findByRole('navigation', { name: 'Quản trị' })
    const hrefs = (name: string) => within(nav).getByRole('link', { name }).getAttribute('href')
    expect(hrefs('Tổng quan')).toBe('/quan-tri')
    expect(hrefs('Người dùng')).toBe('/quan-tri/nguoi-dung')
    expect(hrefs('Khóa học')).toBe('/quan-tri/khoa-hoc')
    expect(hrefs('Bài học')).toBe('/quan-tri/bai-hoc')
    expect(within(nav).getByRole('link', { name: 'Khóa học' }).getAttribute('aria-current')).toBe('page')
    expect(within(nav).getByRole('link', { name: 'Tổng quan' }).getAttribute('aria-current')).toBeNull()
  })

  it('ở /quan-tri chỉ mục "Tổng quan" được đánh dấu đang mở (không đánh dấu nhầm các mục con)', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({ ...USER, role: 'ADMIN' })

    renderAt('/quan-tri')

    const nav = await screen.findByRole('navigation', { name: 'Quản trị' })
    expect(within(nav).getByRole('link', { name: 'Tổng quan' }).getAttribute('aria-current')).toBe('page')
    expect(within(nav).getByRole('link', { name: 'Bài học' }).getAttribute('aria-current')).toBeNull()
  })

  it('học sinh và khách không thấy thanh "Quản trị" ở khu quản trị', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)
    renderAt('/quan-tri/khoa-hoc')
    await screen.findByText('Bạn không có quyền truy cập trang này.')
    expect(screen.queryByRole('navigation', { name: 'Quản trị' })).toBeNull()

    cleanup()
    vi.mocked(fetchCurrentUser).mockResolvedValue(null)
    renderAt('/quan-tri/khoa-hoc')
    await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })
    expect(screen.queryByRole('navigation', { name: 'Quản trị' })).toBeNull()
  })

  // Khách vào trang quản trị bị chuyển sang đăng nhập: chứng tỏ route có tồn tại (không rơi vào trang 404).
  it('khách vào /quan-tri/khoa-hoc thì được chuyển sang trang đăng nhập', async () => {
    renderAt('/quan-tri/khoa-hoc')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
  })

  it('khách vào /quan-tri/bai-hoc thì được chuyển sang trang đăng nhập', async () => {
    renderAt('/quan-tri/bai-hoc')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
  })

  it('khách vào /quan-tri/bai-hoc/moi thì được chuyển sang trang đăng nhập', async () => {
    renderAt('/quan-tri/bai-hoc/moi')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
  })

  it('khách vào /quan-tri/bai-hoc/3/sua thì được chuyển sang trang đăng nhập', async () => {
    renderAt('/quan-tri/bai-hoc/3/sua')

    expect(await screen.findByRole('heading', { level: 1, name: 'Đăng nhập' })).not.toBeNull()
  })
})

describe('App — chân trang và trang chính sách', () => {
  it('chân trang có liên kết Chính sách bảo mật, Điều khoản sử dụng, Bản quyền và Câu hỏi thường gặp', async () => {
    renderAt('/khong-co-trang-nay')

    const footer = await screen.findByRole('contentinfo')
    const href = (name: string) => within(footer).getByRole('link', { name }).getAttribute('href')
    expect(href('Chính sách bảo mật')).toBe('/chinh-sach-bao-mat')
    expect(href('Điều khoản sử dụng')).toBe('/dieu-khoan-su-dung')
    expect(href('Bản quyền')).toBe('/ban-quyen')
    expect(href('Câu hỏi thường gặp')).toBe('/#hoi-dap')
  })

  it('chân trang có liên kết Giới thiệu và /gioi-thieu hiện đúng trang', async () => {
    renderAt('/khong-co-trang-nay')

    const footer = await screen.findByRole('contentinfo')
    expect(within(footer).getByRole('link', { name: 'Giới thiệu' }).getAttribute('href')).toBe('/gioi-thieu')
    cleanup()
    renderAt('/gioi-thieu')
    expect(await screen.findByRole('heading', { level: 1, name: 'Giới thiệu Space-X' })).not.toBeNull()
  })

  it('chân trang có liên kết góp ý, báo lỗi mở sang trang ngoài an toàn', async () => {
    renderAt('/khong-co-trang-nay')

    const link = within(await screen.findByRole('contentinfo')).getByRole('link', { name: /Góp ý, báo lỗi/ })
    expect(link.getAttribute('href')).toMatch(/^https:\/\/github\.com\//)
    expect(link.getAttribute('target')).toBe('_blank')
    expect(link.getAttribute('rel')).toContain('noopener')
  })

  it('chân trang liệt kê nhóm chương trình, ghi rõ đồ án không thu tiền thật, không còn cột theo lớp', async () => {
    renderAt('/khong-co-trang-nay')

    const footer = await screen.findByRole('contentinfo')
    const nhom = await within(footer).findByRole('link', { name: 'Tin học văn phòng' })
    expect(nhom.getAttribute('href')).toBe('/danh-sach-khoa-hoc?nhom=van-phong')
    expect(within(footer).getByRole('link', { name: 'Tất cả khóa học' }).getAttribute('href')).toBe(
      '/danh-sach-khoa-hoc',
    )
    expect(footer.textContent).toContain('Đồ án sinh viên, môi trường thử nghiệm, không thu tiền thật')
    expect(footer.textContent).not.toMatch(/Trung học|Lớp \d|GDPT|miễn phí/i)
  })

  it('không tải được nhóm chương trình thì chân trang vẫn còn liên kết tới danh sách khóa', async () => {
    vi.mocked(fetchCategories).mockRejectedValue(new Error('mạng'))

    renderAt('/khong-co-trang-nay')

    const footer = await screen.findByRole('contentinfo')
    expect(within(footer).getByRole('link', { name: 'Tất cả khóa học' })).not.toBeNull()
    expect(within(footer).queryByRole('alert')).toBeNull()
  })

  it('thanh điều hướng chính có liên kết Hỏi đáp tới phần câu hỏi thường gặp', async () => {
    renderAt('/khong-co-trang-nay')

    const nav = within(await screen.findByRole('banner')).getByRole('navigation', { name: 'Chính' })
    expect(within(nav).getByRole('link', { name: 'Hỏi đáp' }).getAttribute('href')).toBe('/#hoi-dap')
  })

  it('/chinh-sach-bao-mat, /dieu-khoan-su-dung và /ban-quyen hiện đúng trang', async () => {
    renderAt('/chinh-sach-bao-mat')
    expect(await screen.findByRole('heading', { level: 1, name: 'Chính sách bảo mật' })).not.toBeNull()
    cleanup()
    renderAt('/dieu-khoan-su-dung')
    expect(await screen.findByRole('heading', { level: 1, name: 'Điều khoản sử dụng' })).not.toBeNull()
    cleanup()
    renderAt('/ban-quyen')
    expect(await screen.findByRole('heading', { level: 1, name: 'Bản quyền' })).not.toBeNull()
  })
})
