import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { fetchLesson, recordProgress } from '../learningApi'
import LessonPage from './LessonPage'

vi.mock('../learningApi', () => ({ fetchLesson: vi.fn(), recordProgress: vi.fn() }))

const fetchLessonMock = vi.mocked(fetchLesson)
const recordProgressMock = vi.mocked(recordProgress)

const LESSON = {
  id: 1,
  slug: 'thong-tin-va-du-lieu',
  title: 'Thông tin và dữ liệu',
  summary: 'Phân biệt thông tin và dữ liệu',
  contentMd: '# Thông tin và dữ liệu\n\n## Hai khái niệm\n\nNội dung **quan trọng**.',
  course: { slug: 'tin-hoc-10', title: 'Tin học 10' },
  previous: null as { slug: string; title: string } | null,
  next: null as { slug: string; title: string } | null,
}

function renderLesson(entry = '/bai/thong-tin-va-du-lieu') {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/bai/:lessonSlug" element={<LessonPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

const scrollToMock = vi.fn()

beforeEach(() => {
  scrollToMock.mockReset()
  // jsdom không cuộn thật; thay bằng hàm giả để kiểm tra trang bài có đưa người học về đầu trang hay không.
  window.scrollTo = scrollToMock as unknown as typeof window.scrollTo
  fetchLessonMock.mockReset()
  recordProgressMock.mockReset()
  recordProgressMock.mockResolvedValue({ lessonId: 1, status: 'IN_PROGRESS' })
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

describe('LessonPage — đường dẫn, thời gian đọc', () => {
  it('có đường dẫn Trang chủ › khóa › bài, lấy khóa từ API nên mở thẳng bài vẫn đúng', async () => {
    fetchLessonMock.mockResolvedValue(LESSON)

    renderLesson()

    const nav = await screen.findByRole('navigation', { name: 'Đường dẫn' })
    expect(within(nav).getByRole('link', { name: 'Trang chủ' }).getAttribute('href')).toBe('/')
    expect(within(nav).getByRole('link', { name: 'Tin học 10' }).getAttribute('href')).toBe('/khoa-hoc/tin-hoc-10')
    // Bài đang đọc là chữ thường, không phải liên kết, và được đánh dấu là trang hiện tại.
    const current = within(nav).getByText('Thông tin và dữ liệu')
    expect(current.getAttribute('aria-current')).toBe('page')
    expect(current.closest('a')).toBeNull()
  })

  it('tên khóa trong đường dẫn đi qua encodeURIComponent, không dựng địa chỉ từ ký tự lạ', async () => {
    fetchLessonMock.mockResolvedValue({ ...LESSON, course: { slug: 'a b/../admin', title: 'Khóa lạ' } })

    renderLesson()

    const link = await screen.findByRole('link', { name: 'Khóa lạ' })
    expect(link.getAttribute('href')).toBe('/khoa-hoc/a%20b%2F..%2Fadmin')
  })

  it('hiện thời gian đọc ước tính theo độ dài bài', async () => {
    const dai = Array.from({ length: 450 }, (_, i) => `từ${i}`).join(' ')
    fetchLessonMock.mockResolvedValue({ ...LESSON, contentMd: dai })

    renderLesson()

    expect(await screen.findByText('Khoảng 3 phút đọc')).not.toBeNull()
  })
})

describe('LessonPage — bài trước, bài sau', () => {
  const TRUOC = { slug: 'bai-truoc', title: 'Bài trước đó' }
  const SAU = { slug: 'bai-sau', title: 'Bài kế tiếp' }

  it('bài giữa khóa có cả hai liên kết, kèm tên bài và đúng địa chỉ', async () => {
    fetchLessonMock.mockResolvedValue({ ...LESSON, previous: TRUOC, next: SAU })

    renderLesson()

    const nav = await screen.findByRole('navigation', { name: 'Chuyển bài' })
    expect(within(nav).getByRole('link', { name: /Bài trước.*Bài trước đó/ }).getAttribute('href')).toBe('/bai/bai-truoc')
    expect(within(nav).getByRole('link', { name: /Bài sau.*Bài kế tiếp/ }).getAttribute('href')).toBe('/bai/bai-sau')
  })

  it('bài đầu khóa không có liên kết Bài trước', async () => {
    fetchLessonMock.mockResolvedValue({ ...LESSON, previous: null, next: SAU })

    renderLesson()

    const nav = await screen.findByRole('navigation', { name: 'Chuyển bài' })
    expect(within(nav).queryByRole('link', { name: /Bài trước/ })).toBeNull()
    expect(within(nav).getByRole('link', { name: /Bài sau/ })).not.toBeNull()
  })

  it('bài cuối khóa không có Bài sau, thay vào đó có liên kết về trang khóa để không bị cụt đường', async () => {
    fetchLessonMock.mockResolvedValue({ ...LESSON, previous: TRUOC, next: null })

    renderLesson()

    const nav = await screen.findByRole('navigation', { name: 'Chuyển bài' })
    expect(within(nav).queryByRole('link', { name: /Bài sau/ })).toBeNull()
    expect(within(nav).getByRole('link', { name: /Về khóa Tin học 10/ }).getAttribute('href')).toBe('/khoa-hoc/tin-hoc-10')
  })

  it('bấm Bài sau thì tải bài đó và đưa người học về đầu trang', async () => {
    fetchLessonMock.mockImplementation(async (slug: string) =>
      slug === 'bai-sau'
        ? { ...LESSON, id: 2, slug: 'bai-sau', title: 'Bài kế tiếp', previous: { slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu' }, next: null }
        : { ...LESSON, next: SAU },
    )

    renderLesson()
    fireEvent.click(await screen.findByRole('link', { name: /Bài sau/ }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Bài kế tiếp' })).not.toBeNull()
    expect(fetchLessonMock).toHaveBeenCalledWith('bai-sau')
    await waitFor(() => expect(scrollToMock).toHaveBeenLastCalledWith(0, 0))
    // Tiêu đề bài mới nhận focus để người dùng bàn phím và trình đọc màn hình biết trang đã đổi.
    await waitFor(() => expect(document.activeElement?.textContent).toBe('Bài kế tiếp'))
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

describe('LessonPage — ảnh bìa', () => {
  it('bài có ảnh bìa thì hiện ảnh bìa dưới tiêu đề', async () => {
    fetchLessonMock.mockResolvedValue({ ...LESSON, coverImageUrl: '/api/images/5' })

    const { container } = renderLesson()

    await screen.findByRole('heading', { level: 1 })
    expect(container.querySelector('[data-testid="lesson-cover"]')?.getAttribute('src')).toBe('/api/images/5')
  })

  it('bài không có ảnh bìa thì không có khung ảnh bìa', async () => {
    fetchLessonMock.mockResolvedValue(LESSON)

    const { container } = renderLesson()

    await screen.findByRole('heading', { level: 1 })
    expect(container.querySelector('[data-testid="lesson-cover"]')).toBeNull()
  })
})
