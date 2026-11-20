// @vitest-environment node
/// <reference types="node" />
import { existsSync, readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// Kiểm tra phần "đầu trang" mà trình duyệt, Google và mạng xã hội đọc: mô tả, thẻ chia sẻ, biểu tượng tab, robots.txt.
const read = (path: string) => readFileSync(new URL(path, import.meta.url), 'utf-8')
const html = read('../index.html')
const css = read('./index.css')

function meta(attribute: 'name' | 'property', key: string): string | null {
  const tag = new RegExp(`<meta\\s+${attribute}="${key}"\\s+content="([^"]*)"`, 'i').exec(html)
  return tag ? tag[1] : null
}

describe('index.html — mô tả và thẻ chia sẻ', () => {
  it('có meta description tiếng Việt, độ dài vừa phải để Google không cắt', () => {
    const description = meta('name', 'description')
    expect(description).not.toBeNull()
    expect(description!.length).toBeGreaterThanOrEqual(70)
    expect(description!.length).toBeLessThanOrEqual(160)
    expect(description).toMatch(/Tin học/)
  })

  it('có thẻ Open Graph để dán liên kết lên Zalo, Facebook vẫn ra tiêu đề và mô tả', () => {
    expect(meta('property', 'og:title')).toMatch(/Space-X/)
    expect(meta('property', 'og:description')).toBe(meta('name', 'description'))
    expect(meta('property', 'og:type')).toBe('website')
    expect(meta('property', 'og:locale')).toBe('vi_VN')
    expect(meta('property', 'og:site_name')).toBe('Space-X')
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

  it('dùng đúng màu nhấn của giao diện và là SVG hợp lệ', () => {
    const accent = /--color-accent:\s*(#[0-9a-fA-F]{6})/.exec(css)![1].toLowerCase()
    expect(favicon).toMatch(/^<svg[^>]+xmlns="http:\/\/www\.w3\.org\/2000\/svg"/)
    expect(favicon.toLowerCase()).toContain(accent)
  })

  it('không còn file icons.svg bản mẫu của Vite mà không nơi nào dùng', () => {
    expect(existsSync(new URL('../public/icons.svg', import.meta.url))).toBe(false)
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
