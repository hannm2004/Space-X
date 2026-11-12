import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './client'
import { recordProgress, type LessonProgress } from './learningApi'
import LessonProgressControl from './LessonProgressControl'

vi.mock('./learningApi', () => ({ recordProgress: vi.fn() }))

const recordMock = vi.mocked(recordProgress)

const LESSON_ID = 11

function ghi(status: LessonProgress['status']): LessonProgress {
  return { lessonId: LESSON_ID, status }
}

beforeEach(() => {
  recordMock.mockReset()
})

describe('LessonProgressControl — mở bài', () => {
  it('vừa mở bài thì ghi IN_PROGRESS đúng một lần cho đúng bài', async () => {
    recordMock.mockResolvedValue(ghi('IN_PROGRESS'))

    render(<LessonProgressControl lessonId={LESSON_ID} />)

    await screen.findByRole('button', { name: 'Hoàn thành bài' })
    expect(recordMock).toHaveBeenCalledTimes(1)
    expect(recordMock).toHaveBeenCalledWith(LESSON_ID, 'IN_PROGRESS')
  })

  it('trong lúc chờ máy chủ, nút bị vô hiệu vì chưa biết bài đã hoàn thành hay chưa', () => {
    recordMock.mockReturnValue(new Promise(() => {}))

    render(<LessonProgressControl lessonId={LESSON_ID} />)

    const button = screen.getByRole('button', { name: 'Hoàn thành bài' }) as HTMLButtonElement
    expect(button.disabled).toBe(true)
  })

  it('bài chưa hoàn thành: nút "Hoàn thành bài" bấm được', async () => {
    recordMock.mockResolvedValue(ghi('IN_PROGRESS'))

    render(<LessonProgressControl lessonId={LESSON_ID} />)

    const button = (await screen.findByRole('button', { name: 'Hoàn thành bài' })) as HTMLButtonElement
    expect(button.disabled).toBe(false)
    expect(screen.queryByText('Bạn đã hoàn thành bài này.')).toBeNull()
  })

  // Máy chủ không hạ bài đã COMPLETED: gửi IN_PROGRESS vẫn nhận lại COMPLETED, giao diện phải theo kết quả đó.
  it('mở lại bài đã hoàn thành: báo đã hoàn thành và không còn nút', async () => {
    recordMock.mockResolvedValue(ghi('COMPLETED'))

    render(<LessonProgressControl lessonId={LESSON_ID} />)

    expect(await screen.findByText('Bạn đã hoàn thành bài này.')).not.toBeNull()
    expect(screen.queryByRole('button', { name: 'Hoàn thành bài' })).toBeNull()
  })
})

describe('LessonProgressControl — bấm "Hoàn thành bài"', () => {
  it('gửi COMPLETED, trong lúc lưu nút bị vô hiệu (bấm đúp chỉ gửi một lần), xong thì báo đã hoàn thành', async () => {
    let xong: (value: LessonProgress) => void = () => {}
    recordMock.mockResolvedValueOnce(ghi('IN_PROGRESS'))
    recordMock.mockReturnValueOnce(
      new Promise<LessonProgress>((resolve) => {
        xong = resolve
      }),
    )
    render(<LessonProgressControl lessonId={LESSON_ID} />)
    const button = (await screen.findByRole('button', { name: 'Hoàn thành bài' })) as HTMLButtonElement

    fireEvent.click(button)
    fireEvent.click(button)

    expect(button.disabled).toBe(true)
    expect(recordMock).toHaveBeenCalledTimes(2)
    expect(recordMock).toHaveBeenLastCalledWith(LESSON_ID, 'COMPLETED')

    xong(ghi('COMPLETED'))

    expect(await screen.findByText('Bạn đã hoàn thành bài này.')).not.toBeNull()
    expect(screen.queryByRole('button', { name: 'Hoàn thành bài' })).toBeNull()
  })

  it('lưu lỗi thì báo bằng câu cố định (không in nội dung lỗi của máy chủ), nút bấm lại được và thử lại thành công', async () => {
    recordMock.mockResolvedValueOnce(ghi('IN_PROGRESS'))
    recordMock.mockRejectedValueOnce(new Error('SQLException: bí mật nội bộ'))
    recordMock.mockResolvedValueOnce(ghi('COMPLETED'))
    render(<LessonProgressControl lessonId={LESSON_ID} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Hoàn thành bài' }))

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toBe('Không ghi nhận được, hãy thử lại.')
    expect(alert.textContent).not.toContain('SQLException')
    const button = screen.getByRole('button', { name: 'Hoàn thành bài' }) as HTMLButtonElement
    expect(button.disabled).toBe(false)

    fireEvent.click(button)

    expect(await screen.findByText('Bạn đã hoàn thành bài này.')).not.toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
  })
})

describe('LessonProgressControl — lỗi khi mở bài', () => {
  it('lỗi chung: báo câu cố định kèm nút "Thử lại"; thử lại thành công thì hiện nút hoàn thành', async () => {
    recordMock.mockRejectedValueOnce(new Error('SQLException: bí mật nội bộ'))
    recordMock.mockResolvedValueOnce(ghi('IN_PROGRESS'))

    render(<LessonProgressControl lessonId={LESSON_ID} />)

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Không lưu được tiến độ.')
    expect(alert.textContent).not.toContain('SQLException')
    expect(screen.queryByRole('button', { name: 'Hoàn thành bài' })).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('button', { name: 'Hoàn thành bài' })).not.toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(recordMock).toHaveBeenCalledTimes(2)
  })

  it('401 (hết phiên đăng nhập): mời đăng nhập để lưu tiến độ', async () => {
    recordMock.mockRejectedValue(new ApiError(401, 'Bạn cần đăng nhập'))

    render(<LessonProgressControl lessonId={LESSON_ID} />)

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('Bạn cần đăng nhập để lưu tiến độ.')
  })
})
