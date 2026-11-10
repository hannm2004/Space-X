import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import MarkdownContent from './MarkdownContent'

describe('MarkdownContent — định dạng', () => {
  it('hạ cấp tiêu đề một bậc để trang chỉ có một h1 (tiêu đề bài)', () => {
    render(<MarkdownContent source={'# Tiêu đề lớn\n\n## Mục con\n\n### Mục nhỏ'} />)

    expect(screen.getByRole('heading', { level: 2, name: 'Tiêu đề lớn' })).not.toBeNull()
    expect(screen.getByRole('heading', { level: 3, name: 'Mục con' })).not.toBeNull()
    expect(screen.getByRole('heading', { level: 4, name: 'Mục nhỏ' })).not.toBeNull()
    expect(screen.queryByRole('heading', { level: 1 })).toBeNull()
  })

  it('tiêu đề cấp sâu nhất không vượt quá h6', () => {
    render(<MarkdownContent source={'###### Rất sâu'} />)

    expect(screen.getByRole('heading', { level: 6, name: 'Rất sâu' })).not.toBeNull()
  })

  it('hiển thị đoạn văn, chữ đậm, mã trong dòng, danh sách và trích dẫn', () => {
    const { container } = render(
      <MarkdownContent
        source={
          'Đây là **chữ đậm** và `mã`.\n\n- mục một\n- mục hai\n\n1. bước một\n2. bước hai\n\n> ghi chú'
        }
      />,
    )

    expect(container.querySelector('strong')?.textContent).toBe('chữ đậm')
    expect(container.querySelector('code')?.textContent).toBe('mã')
    expect(screen.getAllByRole('listitem')).toHaveLength(4)
    expect(container.querySelector('ul')).not.toBeNull()
    expect(container.querySelector('ol')).not.toBeNull()
    expect(container.querySelector('blockquote')?.textContent).toContain('ghi chú')
  })

  it('hiển thị bảng (GFM) với tiêu đề cột và ô dữ liệu', () => {
    render(
      <MarkdownContent
        source={'| Đơn vị | Quy ước |\n|---|---|\n| 1 byte | 8 bit |\n| 1 KB | 1024 byte |'}
      />,
    )

    expect(screen.getByRole('table')).not.toBeNull()
    expect(screen.getByRole('columnheader', { name: 'Đơn vị' })).not.toBeNull()
    expect(screen.getByRole('cell', { name: '8 bit' })).not.toBeNull()
    expect(screen.getAllByRole('row')).toHaveLength(3)
  })

  it('nguồn rỗng thì không hiện gì và không lỗi', () => {
    const { container } = render(<MarkdownContent source="" />)

    expect(container.textContent).toBe('')
  })
})

describe('MarkdownContent — an toàn (XSS)', () => {
  it('thẻ <script> nhúng trong nội dung không trở thành phần tử script', () => {
    const { container } = render(
      <MarkdownContent source={'Xin chào\n\n<script>window.hacked = true</script>\n\nHết'} />,
    )

    expect(container.querySelector('script')).toBeNull()
    expect((window as unknown as { hacked?: boolean }).hacked).toBeUndefined()
  })

  it('HTML nhúng (img onerror, iframe, style) không tạo phần tử tương ứng', () => {
    const { container } = render(
      <MarkdownContent
        source={
          '<img src="x" onerror="window.hacked = 1">\n\n<iframe src="https://evil.example"></iframe>\n\n<style>body{display:none}</style>'
        }
      />,
    )

    expect(container.querySelector('img')).toBeNull()
    expect(container.querySelector('iframe')).toBeNull()
    expect(container.querySelector('style')).toBeNull()
  })

  it('liên kết javascript: không còn giữ địa chỉ javascript:', () => {
    const { container } = render(
      <MarkdownContent source={'[bấm vào](javascript:window.hacked=1) và [data](data:text/html,<b>x</b>)'} />,
    )

    expect(container.querySelector('a[href^="javascript:" i]')).toBeNull()
    expect(container.querySelector('a[href^="data:" i]')).toBeNull()
  })

  it('liên kết ra ngoài mở tab mới và không gửi referrer', () => {
    render(<MarkdownContent source={'[Wikipedia](https://vi.wikipedia.org)'} />)

    const link = screen.getByRole('link', { name: 'Wikipedia' })
    expect(link.getAttribute('href')).toBe('https://vi.wikipedia.org')
    expect(link.getAttribute('target')).toBe('_blank')
    expect(link.getAttribute('rel')).toContain('noopener')
    expect(link.getAttribute('rel')).toContain('noreferrer')
  })
})

describe('MarkdownContent — ảnh bài học', () => {
  it('hiển thị ảnh của chính hệ thống (/api/images/{id}) với alt và tải lười', () => {
    render(<MarkdownContent source={'![Sơ đồ mạng](/api/images/12)'} />)

    const image = screen.getByRole('img', { name: 'Sơ đồ mạng' })
    expect(image.getAttribute('src')).toBe('/api/images/12')
    expect(image.getAttribute('loading')).toBe('lazy')
  })

  it('không tải ảnh từ trang ngoài (tránh theo dõi người học), chỉ hiện chữ thay thế', () => {
    const { container } = render(
      <MarkdownContent source={'![Ảnh ngoài](https://evil.example/t.png)'} />,
    )

    expect(container.querySelector('img')).toBeNull()
    expect(container.textContent).toContain('Ảnh ngoài')
  })

  it('không nhận đường dẫn ảnh lạ: protocol-relative, id không phải số, đường dẫn lách', () => {
    const { container } = render(
      <MarkdownContent
        source={
          '![a](//evil.example/x.png)\n\n![b](/api/images/abc)\n\n![c](/api/images/1/../../me)\n\n![d](/api/lessons/1)'
        }
      />,
    )

    expect(container.querySelector('img')).toBeNull()
  })
})
