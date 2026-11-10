import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { fetchCourses } from '../learningApi'
import HomePage from './HomePage'

vi.mock('../learningApi', () => ({ fetchCourses: vi.fn() }))
vi.mock('../api', () => ({ fetchHealth: vi.fn().mockResolvedValue('UP') }))

const fetchCoursesMock = vi.mocked(fetchCourses)

function renderHome() {
  return render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  fetchCoursesMock.mockReset()
})

describe('HomePage', () => {
  it('có tiêu đề chính và đoạn giới thiệu', () => {
    fetchCoursesMock.mockReturnValue(new Promise(() => {}))

    renderHome()

    expect(screen.getByRole('heading', { level: 1, name: 'Học Tin học 10, 11, 12' })).not.toBeNull()
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
