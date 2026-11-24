// @vitest-environment node
/// <reference types="node" />
import { existsSync, readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// Kiểm tra phần "đầu trang" mà trình duyệt, Google và mạng xã hội đọc: mô tả, thẻ chia sẻ, biểu tượng tab, robots.txt.
const read = (path: string) => readFileSync(new URL(path, import.meta.url), 'utf-8')
const html = read('../index.html')

function meta(attribute: 'name' | 'property', key: string): string | null {
  const tag = new RegExp(`<meta\\s+${attribute}="${key}"\\s+content="([^"]*)"`, 'i').exec(html)
  return tag ? tag[1] : null
}

describe('index.html — mô tả và thẻ chia sẻ', () => {
  it('tiêu đề tab là tên trung tâm: SPACE-X và "Trung tâm Tin học"', () => {
    const title = /<title>([^<]*)<\/title>/i.exec(html)
    expect(title).not.toBeNull()
    expect(title![1]).toMatch(/SPACE-X/)
    expect(title![1]).toMatch(/Trung tâm Tin học/)
  })

  it('có meta description tiếng Việt, độ dài vừa phải để Google không cắt', () => {
    const description = meta('name', 'description')
    expect(description).not.toBeNull()
    expect(description!.length).toBeGreaterThanOrEqual(70)
    expect(description!.length).toBeLessThanOrEqual(160)
    expect(description).toMatch(/Trung tâm Tin học SPACE-X/)
  })

  it('mô tả không còn nói "lớp 6 đến lớp 12" hay "Miễn phí" của đề tài cũ (D38)', () => {
    const texts = [/<title>([^<]*)<\/title>/i.exec(html)?.[1], meta('name', 'description'), meta('property', 'og:title')]
    for (const text of texts) {
      expect(text).toBeTruthy()
      expect(text).not.toMatch(/lớp 6|lớp 12|GDPT|Miễn phí/i)
    }
  })

  it('có thẻ Open Graph để dán liên kết lên Zalo, Facebook vẫn ra tiêu đề và mô tả', () => {
    expect(meta('property', 'og:title')).toMatch(/SPACE-X/)
    expect(meta('property', 'og:title')).toMatch(/Trung tâm Tin học/)
    expect(meta('property', 'og:description')).toBe(meta('name', 'description'))
    expect(meta('property', 'og:type')).toBe('website')
    expect(meta('property', 'og:locale')).toBe('vi_VN')
    expect(meta('property', 'og:site_name')).toBe('SPACE-X')
  })

  it('không trỏ ảnh chia sẻ tới địa chỉ tạm (localhost) hay đường dẫn tương đối không dùng được', () => {
    expect(html).not.toMatch(/localhost/i)
    const image = meta('property', 'og:image')
    if (image !== null) expect(image).toMatch(/^https:\/\//)
  })
})

describe('biểu tượng tab (favicon)', () => {
  const favicon = read('../public/favicon.svg')

  it('không còn biểu tượng tia sét tím mặc định của Vite', () => {
    expect(favicon.toLowerCase()).not.toContain('#863bff')
    expect(favicon.toLowerCase()).not.toContain('#47bfff')
  })

  it('là SVG hợp lệ, dùng màu thương hiệu mới (nền xanh đậm #0b1424, điểm ảnh cam #f59e0b) và có tên SPACE-X', () => {
    expect(favicon).toMatch(/^<svg[^>]+xmlns="http:\/\/www\.w3\.org\/2000\/svg"/)
    expect(favicon.toLowerCase()).toContain('#0b1424')
    expect(favicon.toLowerCase()).toContain('#f59e0b')
    expect(favicon).toMatch(/aria-label="SPACE-X"/)
  })

  it('không còn biểu tượng ngoặc nhọn "</>" của giao diện cũ (màu nhấn xanh #1d5bd1)', () => {
    expect(favicon.toLowerCase()).not.toContain('#1d5bd1')
  })

  it('không còn file icons.svg bản mẫu của Vite mà không nơi nào dùng', () => {
    expect(existsSync(new URL('../public/icons.svg', import.meta.url))).toBe(false)
  })
})

describe('logo thương hiệu SPACE-X', () => {
  const light = read('../public/logo-mark.svg')
  const dark = read('../public/logo-mark-on-dark.svg')

  it('có đủ hai bản: nền sáng và nền tối', () => {
    expect(existsSync(new URL('../public/logo-mark.svg', import.meta.url))).toBe(true)
    expect(existsSync(new URL('../public/logo-mark-on-dark.svg', import.meta.url))).toBe(true)
  })

  it('cả hai là SVG hợp lệ có tên đọc được và giữ điểm ảnh màu cam', () => {
    for (const svg of [light, dark]) {
      expect(svg).toMatch(/^<svg[^>]+xmlns="http:\/\/www\.w3\.org\/2000\/svg"/)
      expect(svg).toMatch(/aria-label="Logo Trung tâm Tin học SPACE-X"/)
      expect(svg.toLowerCase()).toContain('#f59e0b')
    }
  })

  it('bản nền sáng vẽ bằng xanh đậm, bản nền tối vẽ bằng trắng (không lẫn màu để đọc được trên từng nền)', () => {
    expect(light.toLowerCase()).toContain('fill="#0b1424"')
    expect(light.toLowerCase()).not.toContain('#05070d')
    expect(dark.toLowerCase()).toContain('fill="#ffffff"')
    expect(dark.toLowerCase()).not.toContain('#0b1424')
  })
})

describe('robots.txt', () => {
  const robots = read('../public/robots.txt')

  it('cho công cụ tìm kiếm vào trang công khai nhưng không vào API và trang quản trị', () => {
    expect(robots).toMatch(/^User-agent:\s*\*/im)
    expect(robots).toMatch(/^Disallow:\s*\/api\/\s*$/im)
    expect(robots).toMatch(/^Disallow:\s*\/quan-tri\s*$/im)
    expect(robots).not.toMatch(/^Disallow:\s*\/\s*$/im)
  })
})
