import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchCatalog, fetchCategories, type CatalogCourse } from '../catalogApi'
import { fetchProgress } from '../learningApi'
import { AuthContext } from '../useAuth'
import CourseListPage from './CourseListPage'

vi.mock('../catalogApi', () => ({ fetchCatalog: vi.fn(), fetchCategories: vi.fn() }))
vi.mock('../learningApi', () => ({ fetchProgress: vi.fn() }))

const fetchCatalogMock = vi.mocked(fetchCatalog)
const fetchCategoriesMock = vi.mocked(fetchCategories)

const NHOM = [
  { id: 1, slug: 'thieu-nien', name: 'Khóa cho thiếu niên' },
  { id: 4, slug: 'van-phong', name: 'Tin học văn phòng' },
]

function khoa(id: number, slug: string, title: string): CatalogCourse {
  return {
    id,
    slug,
    title,
    description: null,
    lessonCount: 0,
    category: NHOM[0],
    level: 'BASIC',
    totalHours: 0,
    tuitionVnd: 0,
  }
}

function Address() {
  return <p data-testid="address">{useLocation().search}</p>
}

function renderPage(entry = '/danh-sach-khoa-hoc') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <AuthContext.Provider
        value={{ state: { status: 'anonymous' }, signIn: vi.fn(), signOut: vi.fn(), updateUser: vi.fn() }}
      >
        <CourseListPage />
        <Address />
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

const select = (label: string) => screen.getByLabelText(label) as HTMLSelectElement
const choose = (label: string, value: string) => fireEvent.change(select(label), { target: { value } })
const address = () => screen.getByTestId('address').textContent

beforeEach(() => {
  fetchCatalogMock.mockReset()
  fetchCategoriesMock.mockReset()
  fetchCatalogMock.mockResolvedValue([khoa(1, 'a', 'Khóa A'), khoa(2, 'b', 'Khóa B')])
  fetchCategoriesMock.mockResolvedValue(NHOM)
  vi.mocked(fetchProgress).mockResolvedValue([])
})

describe('CourseListPage', () => {
  it('có tiêu đề, số khóa và thẻ của từng khóa; không lọc thì gọi với bộ lọc rỗng', async () => {
    renderPage()

    expect(screen.getByRole('heading', { level: 1, name: 'Danh sách khóa học' })).not.toBeNull()
    expect(await screen.findByText('2 khóa học')).not.toBeNull()
    expect(screen.getByRole('heading', { level: 2, name: 'Khóa A' })).not.toBeNull()
    expect(screen.getByRole('link', { name: /Khóa B/ }).getAttribute('href')).toBe('/khoa-hoc/b')
    expect(fetchCatalogMock).toHaveBeenCalledWith({})
    expect(screen.queryByRole('link', { name: 'Xóa bộ lọc' })).toBeNull()
  })

  it('đọc đủ năm bộ lọc từ địa chỉ, gửi lên máy chủ và hiện đúng trong ô chọn', async () => {
    renderPage(
      '/danh-sach-khoa-hoc?nhom=van-phong&trinh-do=ADVANCED&thoi-luong=31-60&hoc-phi=1000000-3000000&sap-xep=price_asc',
    )

    await screen.findByText('2 khóa học')
    expect(fetchCatalogMock).toHaveBeenCalledWith({
      category: 'van-phong',
      level: 'ADVANCED',
      duration: '31-60',
      price: '1000000-3000000',
      sort: 'price_asc',
    })
    expect(select('Chương trình').value).toBe('van-phong')
    expect(select('Trình độ').value).toBe('ADVANCED')
    expect(select('Thời lượng').value).toBe('31-60')
    expect(select('Học phí').value).toBe('1000000-3000000')
    expect(select('Sắp xếp').value).toBe('price_asc')
  })

  it('tham số lạ trong địa chỉ bị bỏ qua thay vì gửi lên máy chủ', async () => {
    renderPage('/danh-sach-khoa-hoc?nhom=A%20B&trinh-do=MASTER&thoi-luong=1-2&hoc-phi=abc&sap-xep=title')

    await screen.findByText('2 khóa học')
    expect(fetchCatalogMock).toHaveBeenCalledWith({})
  })

  it('đổi ô chọn thì ghi vào địa chỉ, tải lại và giữ các bộ lọc khác', async () => {
    renderPage('/danh-sach-khoa-hoc?trinh-do=BASIC')
    await screen.findByText('2 khóa học')

    choose('Học phí', '3000001-')

    await waitFor(() =>
      expect(fetchCatalogMock).toHaveBeenLastCalledWith({ level: 'BASIC', price: '3000001-' }),
    )
    expect(address()).toBe('?trinh-do=BASIC&hoc-phi=3000001-')
  })

  it('chọn lại "Tất cả" thì bỏ tham số đó khỏi địa chỉ', async () => {
    renderPage('/danh-sach-khoa-hoc?trinh-do=BASIC')
    await screen.findByText('2 khóa học')

    choose('Trình độ', '')

    await waitFor(() => expect(fetchCatalogMock).toHaveBeenLastCalledWith({}))
    expect(address()).toBe('')
  })

  it('có "Xóa bộ lọc" khi đang lọc, bấm thì về danh sách đầy đủ', async () => {
    renderPage('/danh-sach-khoa-hoc?nhom=van-phong&sap-xep=price_desc')
    await screen.findByText('2 khóa học')

    fireEvent.click(screen.getByRole('link', { name: 'Xóa bộ lọc' }))

    await waitFor(() => expect(fetchCatalogMock).toHaveBeenLastCalledWith({}))
    expect(address()).toBe('')
  })

  it('không có khóa khớp thì nói rõ', async () => {
    fetchCatalogMock.mockResolvedValue([])

    renderPage('/danh-sach-khoa-hoc?trinh-do=EXPERT')

    expect(await screen.findByText('Không có khóa học phù hợp.')).not.toBeNull()
  })

  it('lỗi tải thì có nút Thử lại và bấm thì tải lại', async () => {
    fetchCatalogMock.mockRejectedValueOnce(new Error('mạng'))

    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('2 khóa học')).not.toBeNull()
    expect(fetchCatalogMock).toHaveBeenCalledTimes(2)
  })

  it('không tải được danh sách nhóm thì vẫn xem và lọc được khóa', async () => {
    fetchCategoriesMock.mockRejectedValue(new Error('mạng'))

    renderPage()

    expect(await screen.findByText('2 khóa học')).not.toBeNull()
    expect(Array.from(select('Chương trình').options).map((o) => o.textContent)).toEqual(['Tất cả chương trình'])
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
