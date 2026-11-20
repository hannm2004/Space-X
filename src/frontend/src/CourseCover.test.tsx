import { fireEvent, render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import CourseCover from './CourseCover'

function cover(container: HTMLElement) {
  return container.querySelector('[data-testid="course-cover"]') as HTMLElement
}

describe('CourseCover', () => {
  it('có ảnh bìa thì hiện ảnh đó (trang trí, không đọc thành chữ vì tên khóa nằm ngay bên cạnh)', () => {
    const { container } = render(<CourseCover slug="tin-hoc-10" coverImageUrl="/api/courses/tin-hoc-10/cover?v=1" />)

    const img = container.querySelector('img') as HTMLImageElement
    expect(img.getAttribute('src')).toBe('/api/courses/tin-hoc-10/cover?v=1')
    expect(img.getAttribute('alt')).toBe('')
    expect(cover(container).dataset.kind).toBe('image')
  })

  it('mặc định tải lười và có sẵn width/height để trình duyệt giữ chỗ, không làm trang nhảy', () => {
    const { container } = render(<CourseCover slug="tin-hoc-10" coverImageUrl="/api/courses/tin-hoc-10/cover?v=1" />)

    const img = container.querySelector('img') as HTMLImageElement
    expect(img.getAttribute('loading')).toBe('lazy')
    expect(img.getAttribute('width')).toBe('320')
    expect(img.getAttribute('height')).toBe('180')
  })

  it('priority (ảnh ở đầu trang) thì tải ngay và ưu tiên cao thay vì tải lười', () => {
    const { container } = render(
      <CourseCover slug="tin-hoc-10" coverImageUrl="/api/courses/tin-hoc-10/cover?v=1" priority />,
    )

    const img = container.querySelector('img') as HTMLImageElement
    expect(img.getAttribute('loading')).toBe('eager')
    expect(img.getAttribute('fetchpriority')).toBe('high')
  })

  it('chưa có ảnh bìa thì hiện hình mặc định theo lớp, để khóa nào cũng có ảnh bìa', () => {
    const { container } = render(<CourseCover slug="tin-hoc-10" coverImageUrl={null} />)

    expect(container.querySelector('img')).toBeNull()
    expect(cover(container).dataset.kind).toBe('generated')
    expect(cover(container).textContent).toContain('10')
    expect(cover(container).textContent).toContain('TIN HỌC')
  })

  it('thiếu coverImageUrl (máy chủ cũ) cũng dùng hình mặc định', () => {
    const { container } = render(<CourseCover slug="tin-hoc-6" />)

    expect(cover(container).dataset.kind).toBe('generated')
    expect(cover(container).textContent).toContain('6')
  })

  it('slug không có số lớp thì lấy chữ cái đầu viết hoa thay cho số', () => {
    const { container } = render(<CourseCover slug="lap-trinh-python" />)

    expect(cover(container).textContent).toContain('L')
  })

  it('hình mặc định của cùng một khóa luôn giống nhau, của hai lớp thì khác nhau', () => {
    const a = render(<CourseCover slug="tin-hoc-10" />)
    const b = render(<CourseCover slug="tin-hoc-10" />)
    const c = render(<CourseCover slug="tin-hoc-11" />)

    expect(cover(a.container).innerHTML).toBe(cover(b.container).innerHTML)
    expect(cover(a.container).innerHTML).not.toBe(cover(c.container).innerHTML)
  })

  it('ảnh bìa bị lỗi tải thì chuyển sang hình mặc định thay vì để ảnh hỏng', () => {
    const { container } = render(<CourseCover slug="tin-hoc-10" coverImageUrl="/api/courses/tin-hoc-10/cover?v=1" />)

    fireEvent.error(container.querySelector('img') as HTMLImageElement)

    expect(container.querySelector('img')).toBeNull()
    expect(cover(container).dataset.kind).toBe('generated')
  })

  it('đổi sang đường dẫn ảnh mới thì thử hiện ảnh lại, dù ảnh cũ đã lỗi', () => {
    const { container, rerender } = render(
      <CourseCover slug="tin-hoc-10" coverImageUrl="/api/courses/tin-hoc-10/cover?v=1" />,
    )
    fireEvent.error(container.querySelector('img') as HTMLImageElement)

    rerender(<CourseCover slug="tin-hoc-10" coverImageUrl="/api/courses/tin-hoc-10/cover?v=2" />)

    expect(container.querySelector('img')?.getAttribute('src')).toBe('/api/courses/tin-hoc-10/cover?v=2')
  })

  it('nhận className để nơi dùng đặt cỡ và khung', () => {
    const { container } = render(<CourseCover slug="tin-hoc-10" className="w-28 border" />)

    expect(cover(container).className).toContain('w-28')
  })
})
