import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { searchLessons, type LessonSearchPage } from '../learningApi'
import SearchPage from './SearchPage'

vi.mock('../learningApi', () => ({ searchLessons: vi.fn() }))

const searchLessonsMock = vi.mocked(searchLessons)

function resultPage(overrides: Partial<LessonSearchPage> = {}): LessonSearchPage {
  return {
    items: [
      { id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: 'Phân biệt thông tin và dữ liệu' },
      { id: 2, slug: 'mang-may-tinh', title: 'Mạng máy tính', summary: null },
    ],
    page: 0,
    size: 20,
    totalItems: 2,
    ...overrides,
  }
}

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

function renderSearch(entry = '/tim-kiem?q=mang') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/tim-kiem" element={<SearchPage />} />
        <Route path="/dang-nhap" element={<LoginStub />} />
        <Route path="/bai/:slug" element={<p>Trang bài</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  searchLessonsMock.mockReset()
})

describe('SearchPage — tìm bài học', () => {
  it('chưa có từ khóa thì hiện ô tìm và lời nhắc, không gọi API', () => {
    renderSearch('/tim-kiem')

    expect(screen.getByRole('heading', { level: 1, name: 'Tìm bài học' })).not.toBeNull()
    expect(screen.getByRole('searchbox', { name: 'Từ khóa' })).not.toBeNull()
    expect(screen.getByText('Nhập từ khóa để tìm bài học.')).not.toBeNull()
    expect(searchLessonsMock).not.toHaveBeenCalled()
  })

  it('hiện "đang tìm" trong lúc chờ rồi gọi API với từ khóa và trang 0', () => {
    searchLessonsMock.mockReturnValue(new Promise(() => {}))

    renderSearch()

    expect(screen.getByText('Đang tìm...')).not.toBeNull()
    expect(searchLessonsMock).toHaveBeenCalledWith('mang', 0)
  })

  it('hiện tổng số bài, tên bài (liên kết sang trang bài) và tóm tắt nếu có', async () => {
    searchLessonsMock.mockResolvedValue(resultPage())

    renderSearch()

    expect(await screen.findByText('Tìm thấy 2 bài')).not.toBeNull()
    const link = screen.getByRole('link', { name: /Thông tin và dữ liệu/ })
    expect(link.getAttribute('href')).toBe('/bai/thong-tin-va-du-lieu')
    expect(screen.getByText('Phân biệt thông tin và dữ liệu')).not.toBeNull()
    expect(screen.getByRole('link', { name: /Mạng máy tính/ }).getAttribute('href')).toBe('/bai/mang-may-tinh')
  })

  it('từ khóa được cắt khoảng trắng và đọc từ địa chỉ, ô tìm hiện lại đúng từ khóa', async () => {
    searchLessonsMock.mockResolvedValue(resultPage())

    renderSearch('/tim-kiem?q=%20%20mang%20')

    await screen.findByText('Tìm thấy 2 bài')
    expect(searchLessonsMock).toHaveBeenCalledWith('mang', 0)
    expect((screen.getByRole('searchbox', { name: 'Từ khóa' }) as HTMLInputElement).value).toBe('mang')
  })

  it('không có bài nào thì báo không tìm thấy kèm gợi ý, không phải lỗi', async () => {
    searchLessonsMock.mockResolvedValue(resultPage({ items: [], totalItems: 0 }))

    renderSearch()

    const message = await screen.findByText(/Không tìm thấy bài phù hợp/)
    expect(message.textContent).toContain('mang')
    expect(screen.getByText(/ít nhất 3 ký tự/)).not.toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('chưa đăng nhập (401) thì mời đăng nhập và nhớ để quay lại đúng trang tìm', async () => {
    searchLessonsMock.mockRejectedValue(new ApiError(401, 'Bạn cần đăng nhập'))

    renderSearch()

    expect(await screen.findByText('Bạn cần đăng nhập để tìm bài học.')).not.toBeNull()
    fireEvent.click(screen.getByRole('link', { name: 'Đăng nhập' }))
    expect(await screen.findByText('Trang đăng nhập, quay lại /tim-kiem?q=mang')).not.toBeNull()
  })

  it('lỗi khác thì hiện thông báo và nút Thử lại gọi lại API', async () => {
    searchLessonsMock.mockRejectedValueOnce(new ApiError(500, 'Lỗi')).mockResolvedValueOnce(resultPage())

    renderSearch()

    expect((await screen.findByRole('alert')).textContent).toContain('Không tìm kiếm được')
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Tìm thấy 2 bài')).not.toBeNull()
    expect(searchLessonsMock).toHaveBeenCalledTimes(2)
  })

  it('nhập từ khóa mới rồi bấm Tìm thì tìm lại từ trang 0', async () => {
    searchLessonsMock.mockResolvedValue(resultPage())
    renderSearch('/tim-kiem?q=mang&page=2')
    await screen.findByText('Tìm thấy 2 bài')

    fireEvent.change(screen.getByRole('searchbox', { name: 'Từ khóa' }), { target: { value: '  thuật toán ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))

    await screen.findByText('Tìm thấy 2 bài')
    expect(searchLessonsMock).toHaveBeenLastCalledWith('thuật toán', 0)
  })

  it('bấm Tìm khi ô trống hoặc chỉ có khoảng trắng thì không gọi thêm API', async () => {
    searchLessonsMock.mockResolvedValue(resultPage())
    renderSearch()
    await screen.findByText('Tìm thấy 2 bài')
    searchLessonsMock.mockClear()

    fireEvent.change(screen.getByRole('searchbox', { name: 'Từ khóa' }), { target: { value: '   ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))

    expect(searchLessonsMock).not.toHaveBeenCalled()
  })

  it('ô tìm giới hạn 200 ký tự, đúng giới hạn của API', () => {
    renderSearch('/tim-kiem')

    expect(screen.getByRole('searchbox', { name: 'Từ khóa' }).getAttribute('maxlength')).toBe('200')
  })

  it('tham số trang sai (chữ, số âm) thì coi là trang 0', async () => {
    searchLessonsMock.mockResolvedValue(resultPage())

    renderSearch('/tim-kiem?q=mang&page=abc')
    await screen.findByText('Tìm thấy 2 bài')

    expect(searchLessonsMock).toHaveBeenCalledWith('mang', 0)
  })
})

describe('SearchPage — phân trang', () => {
  it('trang đầu: có "Trang sau" trỏ tới page=1, không có liên kết "Trang trước"', async () => {
    searchLessonsMock.mockResolvedValue(resultPage({ totalItems: 45, page: 0, size: 20 }))

    renderSearch()

    expect(await screen.findByText('Trang 1 / 3')).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Trang sau' }).getAttribute('href')).toBe('/tim-kiem?q=mang&page=1')
    expect(screen.queryByRole('link', { name: 'Trang trước' })).toBeNull()
  })

  it('trang giữa có cả hai liên kết; từ khóa trong liên kết được mã hóa', async () => {
    searchLessonsMock.mockResolvedValue(resultPage({ totalItems: 45, page: 1, size: 20 }))

    renderSearch('/tim-kiem?q=th%C3%B4ng+tin&page=1')

    expect(await screen.findByText('Trang 2 / 3')).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Trang trước' }).getAttribute('href')).toBe(
      '/tim-kiem?q=th%C3%B4ng+tin&page=0',
    )
    expect(screen.getByRole('link', { name: 'Trang sau' }).getAttribute('href')).toBe(
      '/tim-kiem?q=th%C3%B4ng+tin&page=2',
    )
  })

  it('trang cuối không có liên kết "Trang sau"', async () => {
    searchLessonsMock.mockResolvedValue(resultPage({ totalItems: 45, page: 2, size: 20 }))

    renderSearch('/tim-kiem?q=mang&page=2')

    expect(await screen.findByText('Trang 3 / 3')).not.toBeNull()
    expect(screen.queryByRole('link', { name: 'Trang sau' })).toBeNull()
    expect(screen.getByRole('link', { name: 'Trang trước' })).not.toBeNull()
  })

  it('số trang quá lớn (trang rỗng dù có kết quả) thì báo và có liên kết về trang đầu', async () => {
    searchLessonsMock.mockResolvedValue(resultPage({ items: [], totalItems: 45, page: 9, size: 20 }))

    renderSearch('/tim-kiem?q=mang&page=9')

    expect(await screen.findByText(/Không có kết quả ở trang này/)).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Về trang đầu' }).getAttribute('href')).toBe(
      '/tim-kiem?q=mang&page=0',
    )
    expect(screen.queryByText(/Không tìm thấy bài phù hợp/)).toBeNull()
  })

  it('chỉ một trang thì không hiện thanh phân trang', async () => {
    searchLessonsMock.mockResolvedValue(resultPage({ totalItems: 2 }))

    renderSearch()

    await screen.findByText('Tìm thấy 2 bài')
    expect(screen.queryByText(/Trang \d+ \/ \d+/)).toBeNull()
  })

  it('bấm "Trang sau" thì gọi API với trang kế tiếp', async () => {
    searchLessonsMock.mockResolvedValue(resultPage({ totalItems: 45, page: 0, size: 20 }))
    renderSearch()
    fireEvent.click(await screen.findByRole('link', { name: 'Trang sau' }))

    await screen.findByText('Tìm thấy 45 bài')
    expect(searchLessonsMock).toHaveBeenLastCalledWith('mang', 1)
  })
})
