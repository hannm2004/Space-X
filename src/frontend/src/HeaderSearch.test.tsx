import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './client'
import HeaderSearch from './HeaderSearch'
import { searchLessons, type LessonSearchPage } from './learningApi'

vi.mock('./learningApi', () => ({ searchLessons: vi.fn() }))

const searchMock = vi.mocked(searchLessons)

const DEBOUNCE_MS = 300

function page(titles: string[], totalItems = titles.length): LessonSearchPage {
  return {
    items: titles.map((title, index) => ({
      id: index + 1,
      slug: title.toLowerCase().replaceAll(' ', '-'),
      title,
      summary: null,
    })),
    page: 0,
    size: 5,
    totalItems,
  }
}

function Where() {
  const location = useLocation()
  return <p data-testid="where">{location.pathname + location.search}</p>
}

function renderSearch() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <HeaderSearch />
      <Where />
    </MemoryRouter>,
  )
}

function where(): string {
  return screen.getByTestId('where').textContent ?? ''
}

function openBox(): HTMLInputElement {
  fireEvent.click(screen.getByRole('button', { name: 'Tìm bài học' }))
  return screen.getByRole('combobox', { name: 'Tìm bài học' }) as HTMLInputElement
}

function typeInto(input: HTMLInputElement, value: string) {
  fireEvent.change(input, { target: { value } })
}

// Chờ qua thời gian debounce rồi để các Promise đang chờ chạy xong.
async function waitDebounce() {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(DEBOUNCE_MS)
  })
}

beforeEach(() => {
  vi.useFakeTimers()
  searchMock.mockReset()
  searchMock.mockResolvedValue(page([]))
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
})

describe('HeaderSearch — mở và đóng', () => {
  it('lúc đầu chỉ có nút kính lúp, chưa có ô nhập', () => {
    renderSearch()

    const button = screen.getByRole('button', { name: 'Tìm bài học' })
    expect(button.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true')
    expect(screen.queryByRole('combobox')).toBeNull()
    expect(screen.queryByRole('search')).toBeNull()
  })

  it('bấm kính lúp: hiện ô nhập đúng vai trò combobox, đang được focus, giới hạn 200 ký tự', () => {
    renderSearch()

    const input = openBox()

    expect(screen.getByRole('search')).not.toBeNull()
    expect(document.activeElement).toBe(input)
    expect(input.maxLength).toBe(200)
    expect(input.getAttribute('aria-autocomplete')).toBe('list')
    expect(input.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByRole('button', { name: 'Tìm bài học' })).toBeNull()
  })

  it('Esc: đóng ô tìm, xóa chữ đã gõ, trả focus về nút kính lúp', async () => {
    renderSearch()
    const input = openBox()
    typeInto(input, 'thông tin')
    await waitDebounce()

    fireEvent.keyDown(input, { key: 'Escape' })

    const button = screen.getByRole('button', { name: 'Tìm bài học' })
    expect(screen.queryByRole('combobox')).toBeNull()
    expect(document.activeElement).toBe(button)
    // Mở lại thì ô trống, không còn gợi ý cũ.
    const reopened = openBox()
    expect(reopened.value).toBe('')
    expect(screen.queryByRole('listbox')).toBeNull()
  })

  it('bấm ra ngoài ô tìm thì đóng lại', () => {
    renderSearch()
    openBox()

    fireEvent.mouseDown(document.body)

    expect(screen.queryByRole('combobox')).toBeNull()
    expect(screen.getByRole('button', { name: 'Tìm bài học' })).not.toBeNull()
  })

  it('Tab sang phần tử khác ngoài ô tìm thì đóng; focus rời đi mà không có đích cụ thể thì không đóng', () => {
    renderSearch()
    const input = openBox()

    fireEvent.blur(input, { relatedTarget: null })
    expect(screen.getByRole('combobox', { name: 'Tìm bài học' })).not.toBeNull()

    fireEvent.blur(input, { relatedTarget: document.body })
    expect(screen.queryByRole('combobox')).toBeNull()
  })

  it('bấm bên trong ô tìm thì không đóng', () => {
    renderSearch()
    const input = openBox()

    fireEvent.mouseDown(input)

    expect(screen.getByRole('combobox', { name: 'Tìm bài học' })).not.toBeNull()
  })
})

describe('HeaderSearch — gợi ý khi gõ', () => {
  it('không gọi API ngay mỗi lần gõ: gõ liên tục chỉ gọi một lần, sau khi dừng, với từ khóa đã cắt khoảng trắng', async () => {
    renderSearch()
    const input = openBox()

    typeInto(input, 'thôn')
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100)
    })
    typeInto(input, 'thông')
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100)
    })
    typeInto(input, '  thông tin  ')
    expect(searchMock).not.toHaveBeenCalled()

    await waitDebounce()

    expect(searchMock).toHaveBeenCalledTimes(1)
    expect(searchMock).toHaveBeenCalledWith('thông tin', 0, 5)
  })

  it('từ khóa ngắn hơn 3 ký tự (sau khi cắt khoảng trắng) thì không gọi API và không hiện gợi ý', async () => {
    renderSearch()
    const input = openBox()

    typeInto(input, '  ab  ')
    await waitDebounce()

    expect(searchMock).not.toHaveBeenCalled()
    expect(screen.queryByRole('listbox')).toBeNull()
  })

  it('có kết quả: hiện danh sách gợi ý theo thứ tự API trả về, ô nhập báo đã mở danh sách', async () => {
    searchMock.mockResolvedValue(page(['Thông tin và dữ liệu', 'Mạng máy tính']))
    renderSearch()
    const input = openBox()

    typeInto(input, 'thông tin')
    await waitDebounce()

    const options = within(screen.getByRole('listbox')).getAllByRole('option')
    expect(options.map((option) => option.textContent)).toEqual(['Thông tin và dữ liệu', 'Mạng máy tính'])
    expect(input.getAttribute('aria-expanded')).toBe('true')
    expect(input.getAttribute('aria-controls')).toBe(screen.getByRole('listbox').id)
  })

  it('còn nhiều bài hơn số gợi ý: có dòng cuối "Xem tất cả N kết quả"', async () => {
    searchMock.mockResolvedValue(page(['Bài một', 'Bài hai'], 12))
    renderSearch()
    typeInto(openBox(), 'bài')
    await waitDebounce()

    const options = within(screen.getByRole('listbox')).getAllByRole('option')

    expect(options.map((option) => option.textContent)).toEqual([
      'Bài một',
      'Bài hai',
      'Xem tất cả 12 kết quả',
    ])
  })

  it('đủ hết rồi thì không có dòng "Xem tất cả"', async () => {
    searchMock.mockResolvedValue(page(['Bài một', 'Bài hai'], 2))
    renderSearch()
    typeInto(openBox(), 'bài')
    await waitDebounce()

    expect(screen.queryByRole('option', { name: /Xem tất cả/ })).toBeNull()
  })

  it('đang chờ máy chủ: báo "Đang tìm..."', async () => {
    searchMock.mockReturnValue(new Promise(() => {}))
    renderSearch()
    typeInto(openBox(), 'mạng')
    await waitDebounce()

    expect(screen.getByRole('status').textContent).toBe('Đang tìm...')
    expect(screen.queryByRole('listbox')).toBeNull()
  })

  it('không có bài phù hợp: báo rõ, không hiện danh sách rỗng', async () => {
    searchMock.mockResolvedValue(page([]))
    renderSearch()
    typeInto(openBox(), 'zzzzz')
    await waitDebounce()

    expect(screen.getByRole('status').textContent).toBe('Không có bài phù hợp.')
    expect(screen.queryByRole('listbox')).toBeNull()
  })

  it('chưa đăng nhập (401): mời đăng nhập bằng câu cố định', async () => {
    searchMock.mockRejectedValue(new ApiError(401, 'Bạn cần đăng nhập'))
    renderSearch()
    typeInto(openBox(), 'mạng')
    await waitDebounce()

    expect(screen.getByRole('status').textContent).toBe('Bạn cần đăng nhập để tìm bài học.')
  })

  it('lỗi khác: báo không tải được gợi ý, không in nội dung lỗi của máy chủ', async () => {
    searchMock.mockRejectedValue(new Error('SQLException: bí mật nội bộ'))
    renderSearch()
    typeInto(openBox(), 'mạng')
    await waitDebounce()

    const status = screen.getByRole('status')
    expect(status.textContent).toBe('Không tải được gợi ý. Nhấn Enter để tìm.')
    expect(status.textContent).not.toContain('SQLException')
  })

  it('kết quả của lần gõ cũ đến muộn thì bị bỏ, chỉ hiện kết quả của từ khóa mới nhất', async () => {
    let resolveOld: (value: LessonSearchPage) => void = () => {}
    searchMock.mockImplementationOnce(
      () =>
        new Promise<LessonSearchPage>((resolve) => {
          resolveOld = resolve
        }),
    )
    searchMock.mockResolvedValueOnce(page(['Bài mới']))
    renderSearch()
    const input = openBox()

    typeInto(input, 'mạng')
    await waitDebounce()
    typeInto(input, 'máy tính')
    await waitDebounce()
    await act(async () => {
      resolveOld(page(['Bài cũ']))
    })

    const options = within(screen.getByRole('listbox')).getAllByRole('option')
    expect(options.map((option) => option.textContent)).toEqual(['Bài mới'])
  })

  it('xóa bớt chữ xuống dưới 3 ký tự thì gợi ý biến mất và kết quả cũ đến muộn không hiện lại', async () => {
    let resolveOld: (value: LessonSearchPage) => void = () => {}
    searchMock.mockImplementationOnce(
      () =>
        new Promise<LessonSearchPage>((resolve) => {
          resolveOld = resolve
        }),
    )
    renderSearch()
    const input = openBox()

    typeInto(input, 'mạng')
    await waitDebounce()
    typeInto(input, 'mạ')
    await act(async () => {
      resolveOld(page(['Bài cũ']))
    })

    expect(screen.queryByRole('listbox')).toBeNull()
    expect(screen.queryByRole('status')).toBeNull()
  })
})

describe('HeaderSearch — chọn bằng bàn phím và chuột', () => {
  async function openWithSuggestions() {
    searchMock.mockResolvedValue(page(['Bài một', 'Bài hai'], 9))
    renderSearch()
    const input = openBox()
    typeInto(input, 'bài')
    await waitDebounce()
    return input
  }

  function activeOptionText(input: HTMLElement): string | null {
    const id = input.getAttribute('aria-activedescendant')
    return id === null ? null : (document.getElementById(id)?.textContent ?? null)
  }

  it('mũi tên xuống/lên đổi dòng đang chọn và xoay vòng; lúc đầu chưa chọn dòng nào', async () => {
    const input = await openWithSuggestions()
    expect(activeOptionText(input)).toBeNull()

    fireEvent.keyDown(input, { key: 'ArrowDown' })
    expect(activeOptionText(input)).toBe('Bài một')
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    expect(activeOptionText(input)).toBe('Bài hai')
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    expect(activeOptionText(input)).toBe('Xem tất cả 9 kết quả')
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    expect(activeOptionText(input)).toBe('Bài một')
    fireEvent.keyDown(input, { key: 'ArrowUp' })
    expect(activeOptionText(input)).toBe('Xem tất cả 9 kết quả')
    expect(screen.getByRole('option', { selected: true }).textContent).toBe('Xem tất cả 9 kết quả')
  })

  it('đang chọn một gợi ý rồi nhấn Enter: vào thẳng bài đó và đóng ô tìm', async () => {
    const input = await openWithSuggestions()

    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.keyDown(input, { key: 'Enter' })

    expect(where()).toBe('/bai/b%C3%A0i-hai')
    expect(screen.queryByRole('combobox')).toBeNull()
  })

  it('đang chọn dòng "Xem tất cả" rồi nhấn Enter: sang trang kết quả', async () => {
    const input = await openWithSuggestions()

    fireEvent.keyDown(input, { key: 'ArrowUp' })
    fireEvent.keyDown(input, { key: 'Enter' })

    expect(where()).toBe(`/tim-kiem?${new URLSearchParams({ q: 'bài' })}`)
  })

  it('bấm chuột vào một gợi ý: vào bài đó và đóng ô tìm', async () => {
    await openWithSuggestions()

    fireEvent.click(screen.getByRole('option', { name: 'Bài một' }))

    expect(where()).toBe('/bai/b%C3%A0i-m%E1%BB%99t')
    expect(screen.queryByRole('combobox')).toBeNull()
  })

  it('gõ thêm chữ thì bỏ dòng đang chọn (tránh Enter nhầm vào bài của từ khóa cũ)', async () => {
    const input = await openWithSuggestions()
    fireEvent.keyDown(input, { key: 'ArrowDown' })

    typeInto(input, 'bài học')

    expect(activeOptionText(input)).toBeNull()
  })
})

describe('HeaderSearch — Enter sang trang kết quả', () => {
  it('Enter khi chưa chọn gợi ý: sang /tim-kiem?q= với từ khóa đã cắt khoảng trắng và mã hóa, rồi đóng ô tìm', () => {
    renderSearch()
    const input = openBox()
    typeInto(input, '  thông tin & dữ liệu  ')

    fireEvent.submit(input.closest('form') as HTMLFormElement)

    expect(where()).toBe(`/tim-kiem?${new URLSearchParams({ q: 'thông tin & dữ liệu' })}`)
    expect(screen.queryByRole('combobox')).toBeNull()
  })

  it('Enter khi ô trống hoặc chỉ có khoảng trắng: không đi đâu, ô vẫn mở', () => {
    renderSearch()
    const input = openBox()
    typeInto(input, '   ')

    fireEvent.submit(input.closest('form') as HTMLFormElement)

    expect(where()).toBe('/')
    expect(screen.getByRole('combobox', { name: 'Tìm bài học' })).not.toBeNull()
  })

  it('Enter ngay khi vừa gõ: hủy gợi ý đang chờ, không gọi API gợi ý nữa', async () => {
    renderSearch()
    const input = openBox()
    typeInto(input, 'thông tin')

    fireEvent.submit(input.closest('form') as HTMLFormElement)
    await waitDebounce()

    expect(searchMock).not.toHaveBeenCalled()
  })

  it('từ khóa 1–2 ký tự vẫn Enter được (trang kết quả tự báo không có bài)', () => {
    renderSearch()
    const input = openBox()
    typeInto(input, 'ai')

    fireEvent.submit(input.closest('form') as HTMLFormElement)

    expect(where()).toBe('/tim-kiem?q=ai')
  })
})
