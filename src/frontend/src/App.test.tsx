import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { fetchHealth } from './api'
import { fetchCurrentUser, logout } from './authApi'
import { ApiError } from './client'
import { fetchCourses, fetchLesson, fetchLessons, fetchProgress, recordProgress, searchLessons } from './learningApi'

vi.mock('./api', () => ({ fetchHealth: vi.fn() }))
vi.mock('./authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))
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
  vi.mocked(fetchCourses).mockResolvedValue([
    { id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: null },
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

    expect(screen.getByRole('heading', { level: 1, name: 'Học Tin học 10, 11, 12' })).not.toBeNull()
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
    renderAt('/')

    fireEvent.click(await screen.findByRole('link', { name: /Tin học 10/ }))
    fireEvent.click(await screen.findByRole('link', { name: /Thông tin và dữ liệu/ }))
    expect(await screen.findByText('Nội dung bài')).not.toBeNull()

    fireEvent.click(screen.getByRole('link', { name: /Quay lại Tin học 10/ }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Tin học 10' })).not.toBeNull()
  })

  it('đường dẫn không tồn tại hiện trang "Không tìm thấy trang" kèm liên kết về trang chủ', () => {
    renderAt('/khong-co-trang-nay')

    expect(screen.getByRole('heading', { level: 1, name: 'Không tìm thấy trang' })).not.toBeNull()
    const home = screen.getByRole('link', { name: 'Về trang chủ' })
    expect(home.getAttribute('href')).toBe('/')
  })

  it('mọi trang đều có thanh đầu trang với liên kết Space-X về trang chủ và nút đổi giao diện', () => {
    renderAt('/khong-co-trang-nay')

    const brand = screen.getByRole('link', { name: 'Space-X' })
    expect(brand.getAttribute('href')).toBe('/')
    expect(screen.getByRole('button', { name: 'Chuyển sang giao diện tối' })).not.toBeNull()
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

  it('đã đăng nhập thì hiện tên, "Đổi mật khẩu" và "Đăng xuất", không còn "Đăng nhập"', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)

    renderAt('/khong-co-trang-nay')

    const header = await screen.findByRole('banner')
    expect(await within(header).findByText('Nguyễn Văn A')).not.toBeNull()
    expect(within(header).getByRole('link', { name: 'Đổi mật khẩu' }).getAttribute('href')).toBe(
      '/doi-mat-khau',
    )
    expect(within(header).getByRole('button', { name: 'Đăng xuất' })).not.toBeNull()
    expect(within(header).queryByRole('link', { name: 'Đăng nhập' })).toBeNull()
  })

  it('Admin thấy liên kết "Quản lý người dùng" trên thanh đầu trang', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({ ...USER, role: 'ADMIN' })

    renderAt('/khong-co-trang-nay')

    const header = await screen.findByRole('banner')
    expect(
      (await within(header).findByRole('link', { name: 'Quản lý người dùng' })).getAttribute('href'),
    ).toBe('/quan-tri/nguoi-dung')
  })

  it('học sinh và khách không thấy liên kết "Quản lý người dùng"', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)
    renderAt('/khong-co-trang-nay')
    const header = await screen.findByRole('banner')
    await within(header).findByText('Nguyễn Văn A')
    expect(within(header).queryByRole('link', { name: 'Quản lý người dùng' })).toBeNull()

    cleanup()
    vi.mocked(fetchCurrentUser).mockResolvedValue(null)
    renderAt('/khong-co-trang-nay')
    const guestHeader = await screen.findByRole('banner')
    await within(guestHeader).findByRole('link', { name: 'Đăng nhập' })
    expect(within(guestHeader).queryByRole('link', { name: 'Quản lý người dùng' })).toBeNull()
  })

  it('bấm "Đăng xuất" gọi API rồi trở về trạng thái chưa đăng nhập và trang chủ', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue(USER)
    renderAt('/doi-mat-khau')

    fireEvent.click(await screen.findByRole('button', { name: 'Đăng xuất' }))

    expect(await screen.findByRole('link', { name: 'Đăng nhập' })).not.toBeNull()
    expect(logout).toHaveBeenCalledTimes(1)
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Học Tin học 10, 11, 12' }),
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
})
