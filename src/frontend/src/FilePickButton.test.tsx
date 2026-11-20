import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import FilePickButton from './FilePickButton'

const INPUT_LABEL = 'Tải ảnh lên (PNG, JPEG hoặc WebP, tối đa 1 MB)'

function setup(props: { disabled?: boolean } = {}) {
  const onFile = vi.fn()
  render(
    <FilePickButton
      label="Chọn ảnh từ máy"
      inputLabel={INPUT_LABEL}
      accept="image/png,image/jpeg,image/webp"
      onFile={onFile}
      {...props}
    />,
  )
  return { onFile, input: screen.getByLabelText(INPUT_LABEL) as HTMLInputElement }
}

describe('FilePickButton', () => {
  it('hiện một nút bấm đúng nhãn, dùng bộ kiểu nút chung (không phải ô chọn tệp mặc định của trình duyệt)', () => {
    setup()

    const button = screen.getByRole('button', { name: 'Chọn ảnh từ máy' })
    expect(button.className).toContain('btn')
    expect(button.getAttribute('type')).toBe('button')
  })

  it('bấm nút thì mở hộp chọn tệp', () => {
    const { input } = setup()
    const click = vi.spyOn(input, 'click')

    fireEvent.click(screen.getByRole('button', { name: 'Chọn ảnh từ máy' }))

    expect(click).toHaveBeenCalledTimes(1)
  })

  it('ô nhập tệp chỉ nhận loại ảnh đã khai báo và không nằm trong thứ tự Tab (nút đã thay vai trò)', () => {
    const { input } = setup()

    expect(input.accept).toBe('image/png,image/jpeg,image/webp')
    expect(input.tabIndex).toBe(-1)
  })

  it('chọn tệp thì báo tệp đó cho nơi gọi', () => {
    const { onFile, input } = setup()
    const file = new File(['x'], 'bia.png', { type: 'image/png' })

    fireEvent.change(input, { target: { files: [file] } })

    expect(onFile).toHaveBeenCalledTimes(1)
    expect(onFile).toHaveBeenCalledWith(file)
  })

  it('đóng hộp chọn tệp mà không chọn gì thì không báo gì', () => {
    const { onFile, input } = setup()

    fireEvent.change(input, { target: { files: [] } })

    expect(onFile).not.toHaveBeenCalled()
  })

  it('xóa lựa chọn sau mỗi lần để chọn lại đúng tệp cũ vẫn kích hoạt', () => {
    const { input } = setup()
    const file = new File(['x'], 'bia.png', { type: 'image/png' })

    fireEvent.change(input, { target: { files: [file] } })

    expect(input.value).toBe('')
  })

  it('đang bận thì khóa cả nút lẫn ô nhập', () => {
    const { input } = setup({ disabled: true })

    expect((screen.getByRole('button', { name: 'Chọn ảnh từ máy' }) as HTMLButtonElement).disabled).toBe(true)
    expect(input.disabled).toBe(true)
  })
})
