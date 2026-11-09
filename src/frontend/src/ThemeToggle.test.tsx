import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ThemeToggle from './ThemeToggle'

const LABEL_TO_LIGHT = 'Chuyển sang giao diện sáng'
const LABEL_TO_DARK = 'Chuyển sang giao diện tối'

// jsdom không có matchMedia nên phải giả lập: `prefersDark` là cài đặt giao diện tối của hệ điều hành.
function stubSystemTheme(prefersDark: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    writable: true,
    value: vi.fn().mockReturnValue({ matches: prefersDark }),
  })
}

function isDarkApplied() {
  return document.documentElement.classList.contains('dark')
}

beforeEach(() => {
  localStorage.clear()
  document.documentElement.classList.remove('dark')
})

afterEach(() => {
  vi.restoreAllMocks()
  Reflect.deleteProperty(window, 'matchMedia')
})

describe('ThemeToggle — giao diện ban đầu', () => {
  it('chưa lưu lựa chọn, hệ điều hành dùng giao diện tối thì đang ở tối', () => {
    stubSystemTheme(true)

    render(<ThemeToggle />)

    expect(screen.getByRole('button', { name: LABEL_TO_LIGHT })).not.toBeNull()
  })

  it('chưa lưu lựa chọn, hệ điều hành dùng giao diện sáng thì đang ở sáng', () => {
    stubSystemTheme(false)

    render(<ThemeToggle />)

    expect(screen.getByRole('button', { name: LABEL_TO_DARK })).not.toBeNull()
  })

  it('lựa chọn đã lưu được ưu tiên hơn hệ điều hành', () => {
    stubSystemTheme(true)
    localStorage.setItem('theme', 'light')

    render(<ThemeToggle />)

    expect(screen.getByRole('button', { name: LABEL_TO_DARK })).not.toBeNull()
  })
})

describe('ThemeToggle — bấm nút', () => {
  it('từ sáng sang tối: bật class dark, nhớ lựa chọn và đổi nhãn nút', () => {
    stubSystemTheme(false)
    render(<ThemeToggle />)

    fireEvent.click(screen.getByRole('button', { name: LABEL_TO_DARK }))

    expect(isDarkApplied()).toBe(true)
    expect(localStorage.getItem('theme')).toBe('dark')
    expect(screen.getByRole('button', { name: LABEL_TO_LIGHT })).not.toBeNull()
  })

  it('bấm lần hai quay lại sáng: bỏ class dark và nhớ lựa chọn mới', () => {
    stubSystemTheme(false)
    render(<ThemeToggle />)

    fireEvent.click(screen.getByRole('button', { name: LABEL_TO_DARK }))
    fireEvent.click(screen.getByRole('button', { name: LABEL_TO_LIGHT }))

    expect(isDarkApplied()).toBe(false)
    expect(localStorage.getItem('theme')).toBe('light')
  })

  it('trình duyệt chặn localStorage thì vẫn đổi được giao diện', () => {
    stubSystemTheme(false)
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('bị chặn', 'SecurityError')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('bị chặn', 'SecurityError')
    })

    render(<ThemeToggle />)
    fireEvent.click(screen.getByRole('button', { name: LABEL_TO_DARK }))

    expect(isDarkApplied()).toBe(true)
    expect(screen.getByRole('button', { name: LABEL_TO_LIGHT })).not.toBeNull()
  })
})
