import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { fetchHealth } from './api'
import { fetchCourses, fetchLesson, fetchLessons } from './learningApi'

vi.mock('./api', () => ({ fetchHealth: vi.fn() }))
vi.mock('./learningApi', () => ({
  fetchCourses: vi.fn(),
  fetchLessons: vi.fn(),
  fetchLesson: vi.fn(),
}))

beforeEach(() => {
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
})
