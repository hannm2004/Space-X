import { describe, expect, it, vi } from 'vitest'
import { applyTheme, resolveInitialTheme, toggleTheme } from './theme'

describe('resolveInitialTheme', () => {
  it('ưu tiên lựa chọn đã lưu, bất kể hệ điều hành', () => {
    expect(resolveInitialTheme('dark', false)).toBe('dark')
    expect(resolveInitialTheme('light', true)).toBe('light')
  })

  it('chưa lưu gì thì theo hệ điều hành', () => {
    expect(resolveInitialTheme(null, true)).toBe('dark')
    expect(resolveInitialTheme(null, false)).toBe('light')
  })

  it('giá trị đã lưu không hợp lệ thì bỏ qua và theo hệ điều hành', () => {
    expect(resolveInitialTheme('xanh', true)).toBe('dark')
    expect(resolveInitialTheme('', false)).toBe('light')
  })
})

describe('toggleTheme', () => {
  it('đổi qua lại giữa sáng và tối', () => {
    expect(toggleTheme('light')).toBe('dark')
    expect(toggleTheme('dark')).toBe('light')
  })
})

describe('applyTheme', () => {
  it('bật class dark khi giao diện tối', () => {
    const element = { classList: { toggle: vi.fn() } }
    applyTheme('dark', element)
    expect(element.classList.toggle).toHaveBeenCalledWith('dark', true)
  })

  it('bỏ class dark khi giao diện sáng', () => {
    const element = { classList: { toggle: vi.fn() } }
    applyTheme('light', element)
    expect(element.classList.toggle).toHaveBeenCalledWith('dark', false)
  })
})
