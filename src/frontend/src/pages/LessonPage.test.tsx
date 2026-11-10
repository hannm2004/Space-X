import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { fetchLesson } from '../learningApi'
import LessonPage from './LessonPage'

vi.mock('../learningApi', () => ({ fetchLesson: vi.fn() }))

const fetchLessonMock = vi.mocked(fetchLesson)

const LESSON = {
  id: 1,
  slug: 'thong-tin-va-du-lieu',
  title: 'Thông tin và dữ liệu',
  summary: 'Phân biệt thông tin và dữ liệu',
  contentMd: '# Thông tin và dữ liệu\n\n## Hai khái niệm\n\nNội dung **quan trọng**.',
}

function renderLesson(entry: string | { pathname: string; state: unknown } = '/bai/thong-tin-va-du-lieu') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/bai/:lessonSlug" element={<LessonPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  fetchLessonMock.mockReset()
})

describe('LessonPage — đọc bài', () => {
  it('hiện "đang tải" trong lúc chờ', () => {
    fetchLessonMock.mockReturnValue(new Promise(() => {}))

    renderLesson()

    expect(screen.getByText('Đang tải bài học...')).not.toBeNull()
  })

  it('hiện tên bài, tóm tắt và nội dung Markdown đã dựng', async () => {
    fetchLessonMock.mockResolvedValue(LESSON)

    const { container } = renderLesson()

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Thông tin và dữ liệu' }),
    ).not.toBeNull()
    expect(screen.getByText('Phân biệt thông tin và dữ liệu')).not.toBeNull()
    expect(screen.getByRole('heading', { level: 3, name: 'Hai khái niệm' })).not.toBeNull()
    expect(container.querySelector('strong')?.textContent).toBe('quan trọng')
    expect(fetchLessonMock).toHaveBeenCalledWith('thong-tin-va-du-lieu')
  })

  it('bỏ dòng "# Tên bài" ở đầu nội dung nếu trùng tên bài, để trang không lặp tiêu đề', async () => {
    fetchLessonMock.mockResolvedValue(LESSON)

    renderLesson()

    await screen.findByRole('heading', { level: 1, name: 'Thông tin và dữ liệu' })
    expect(screen.getAllByRole('heading', { name: 'Thông tin và dữ liệu' })).toHaveLength(1)
  })

  it('giữ lại tiêu đề đầu nội dung nếu khác tên bài', async () => {
    fetchLessonMock.mockResolvedValue({ ...LESSON, contentMd: '# Mở đầu khác\n\nNội dung' })

    renderLesson()

    expect(await screen.findByRole('heading', { level: 2, name: 'Mở đầu khác' })).not.toBeNull()
  })

  it('bài không có tóm tắt thì không hiện đoạn tóm tắt rỗng', async () => {
    fetchLessonMock.mockResolvedValue({ ...LESSON, summary: null })

    const { container } = renderLesson()

    await screen.findByRole('heading', { level: 1 })
    expect(container.querySelector('[data-testid="lesson-summary"]')).toBeNull()
  })
})

describe('LessonPage — liên kết quay lại', () => {
  it('đi từ trang khóa sang thì có liên kết quay lại đúng khóa đó', async () => {
    fetchLessonMock.mockResolvedValue(LESSON)

    renderLesson({
      pathname: '/bai/thong-tin-va-du-lieu',
      state: { courseSlug: 'tin-hoc-10', courseTitle: 'Tin học 10' },
    })

    const back = await screen.findByRole('link', { name: /Quay lại Tin học 10/ })
    expect(back.getAttribute('href')).toBe('/khoa-hoc/tin-hoc-10')
  })

  it('mở thẳng bài (không có thông tin khóa) thì quay về trang chủ', async () => {
    fetchLessonMock.mockResolvedValue(LESSON)

    renderLesson()

    const back = await screen.findByRole('link', { name: /Trang chủ/ })
    expect(back.getAttribute('href')).toBe('/')
  })

  it('thông tin khóa sai dạng hoặc có ký tự lạ thì bị bỏ qua', async () => {
    fetchLessonMock.mockResolvedValue(LESSON)

    renderLesson({
      pathname: '/bai/thong-tin-va-du-lieu',
      state: { courseSlug: '../admin', courseTitle: 'Khóa giả' },
    })

    const back = await screen.findByRole('link', { name: /Trang chủ/ })
    expect(back.getAttribute('href')).toBe('/')
    expect(screen.queryByText(/Khóa giả/)).toBeNull()
  })
})

describe('LessonPage — quyền đọc và lỗi', () => {
  it('chưa đăng nhập (401) thì mời đăng nhập, không hiện nội dung bài', async () => {
    fetchLessonMock.mockRejectedValue(new ApiError(401, 'Bạn cần đăng nhập'))

    renderLesson()

    expect(await screen.findByText('Bạn cần đăng nhập để đọc bài học này.')).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Đăng nhập' }).getAttribute('href')).toBe('/dang-nhap')
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })

  it('bài không tồn tại hoặc là bản nháp (404) thì báo không tìm thấy', async () => {
    fetchLessonMock.mockRejectedValue(new ApiError(404, 'Không tìm thấy bài học'))

    renderLesson('/bai/khong-co')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Không tìm thấy bài học' }),
    ).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Về trang chủ' }).getAttribute('href')).toBe('/')
  })

  it('lỗi khác thì báo lỗi chung và bấm Thử lại sẽ tải lại', async () => {
    fetchLessonMock
      .mockRejectedValueOnce(new ApiError(0, 'Không kết nối được tới máy chủ'))
      .mockResolvedValueOnce(LESSON)

    renderLesson()

    expect(await screen.findByText('Không tải được bài học.')).not.toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Thông tin và dữ liệu' }),
    ).not.toBeNull()
    expect(fetchLessonMock).toHaveBeenCalledTimes(2)
  })
})
