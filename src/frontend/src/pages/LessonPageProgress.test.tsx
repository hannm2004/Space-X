import { fireEvent, render, screen, within } from '@testing-library/react'
import { Link, MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../client'
import { fetchLesson, recordProgress } from '../learningApi'
import LessonPage from './LessonPage'

vi.mock('../learningApi', () => ({ fetchLesson: vi.fn(), recordProgress: vi.fn() }))

const fetchLessonMock = vi.mocked(fetchLesson)
const recordProgressMock = vi.mocked(recordProgress)

function bai(id: number, slug: string, title: string) {
  return { id, slug, title, summary: null, contentMd: 'Nội dung ' + title }
}

function renderLesson(path = '/bai/may-tinh') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Link to="/bai/mang">Sang bài Mạng</Link>
      <Routes>
        <Route path="/bai/:lessonSlug" element={<LessonPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  fetchLessonMock.mockReset()
  recordProgressMock.mockReset()
  recordProgressMock.mockImplementation((lessonId) => Promise.resolve({ lessonId, status: 'IN_PROGRESS' }))
})

describe('LessonPage — tiến độ học', () => {
  it('bài tải xong thì ghi đã mở đúng bài (theo id của bài) và có nút "Hoàn thành bài" trong phần bài viết', async () => {
    fetchLessonMock.mockResolvedValue(bai(11, 'may-tinh', 'Máy tính'))

    renderLesson()

    const article = await screen.findByRole('article')
    expect(await within(article).findByRole('button', { name: 'Hoàn thành bài' })).not.toBeNull()
    expect(recordProgressMock).toHaveBeenCalledTimes(1)
    expect(recordProgressMock).toHaveBeenCalledWith(11, 'IN_PROGRESS')
  })

  it('đang tải bài, bài lỗi 401, 404 hoặc lỗi khác thì không gọi API tiến độ và không có nút', async () => {
    fetchLessonMock.mockReturnValue(new Promise(() => {}))
    const loading = renderLesson()
    expect(recordProgressMock).not.toHaveBeenCalled()
    loading.unmount()

    for (const loi of [new ApiError(401, 'x'), new ApiError(404, 'x'), new ApiError(502, 'x')]) {
      fetchLessonMock.mockReset()
      fetchLessonMock.mockRejectedValue(loi)
      const view = renderLesson()
      await vi.waitFor(() => expect(screen.queryByText('Đang tải bài học...')).toBeNull())
      expect(screen.queryByRole('button', { name: 'Hoàn thành bài' })).toBeNull()
      view.unmount()
    }
    expect(recordProgressMock).not.toHaveBeenCalled()
  })

  it('sang bài khác thì nút gắn lại từ đầu và ghi đã mở bài mới, không giữ trạng thái "đã hoàn thành" của bài trước', async () => {
    fetchLessonMock.mockImplementation((slug) =>
      Promise.resolve(slug === 'may-tinh' ? bai(11, 'may-tinh', 'Máy tính') : bai(12, 'mang', 'Mạng')),
    )
    recordProgressMock.mockImplementation((lessonId, status) =>
      Promise.resolve({ lessonId, status: lessonId === 11 ? 'COMPLETED' : status }),
    )
    renderLesson()
    expect(await screen.findByText('Bạn đã hoàn thành bài này.')).not.toBeNull()

    fireEvent.click(screen.getByRole('link', { name: 'Sang bài Mạng' }))

    expect(await screen.findByRole('button', { name: 'Hoàn thành bài' })).not.toBeNull()
    expect(screen.queryByText('Bạn đã hoàn thành bài này.')).toBeNull()
    expect(recordProgressMock).toHaveBeenCalledWith(12, 'IN_PROGRESS')
  })
})
