// @vitest-environment node
/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// Test cho hệ thống thiết kế: đọc thẳng index.css, tính độ tương phản của các cặp màu quan trọng ở cả hai chế độ (WCAG AA)
// và chặn việc quay lại các kiểu cũ (chữ có chân, gạch chân đen dày). Đổi màu mà làm chữ khó đọc là test báo đỏ ngay.
const css = readFileSync(new URL('./index.css', import.meta.url), 'utf-8')
const sources = import.meta.glob('./**/*.{ts,tsx}', { query: '?raw', import: 'default', eager: true }) as Record<string, string>
const appFiles = Object.entries(sources).filter(([path]) => !/\.test\.tsx?$/.test(path))

function tokens(block: RegExpMatchArray | null): Record<string, string> {
  const result: Record<string, string> = {}
  for (const match of (block?.[1] ?? '').matchAll(/--color-([a-z-]+):\s*(#[0-9a-fA-F]{6})/g)) result[match[1]] = match[2]
  return result
}

const light = tokens(css.match(/@theme\s*\{([^}]*)\}/))
const dark = tokens(css.match(/^\.dark\s*\{([^}]*)\}/m))

function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
  const channel = (c: number) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

// [chữ, nền]: chữ thường cần tối thiểu 4,5:1.
const TEXT_PAIRS: [string, string][] = [
  ['ink', 'paper'],
  ['ink', 'surface'],
  ['muted', 'paper'],
  ['muted', 'surface'],
  ['muted', 'sunken'],
  ['accent', 'paper'],
  ['accent', 'surface'],
  ['accent', 'accent-soft'],
  ['on-accent', 'accent'],
  ['on-accent', 'accent-strong'],
  ['danger', 'surface'],
  ['danger', 'danger-soft'],
  ['success', 'surface'],
  ['success', 'success-soft'],
  ['warn', 'surface'],
]

function worst(palette: Record<string, string>, pairs: [string, string][]) {
  return pairs
    .map(([fg, bg]) => ({ pair: `${fg}/${bg}`, ratio: contrast(palette[fg], palette[bg]) }))
    .sort((x, y) => x.ratio - y.ratio)[0]
}

describe('hệ thống thiết kế — màu', () => {
  it('đọc được đủ các màu của cả hai chế độ từ index.css', () => {
    expect(Object.keys(light).length).toBeGreaterThanOrEqual(14)
    expect(Object.keys(dark).sort()).toEqual(Object.keys(light).sort())
  })

  it('chữ và nền đạt tương phản tối thiểu 4,5:1 ở chế độ sáng (WCAG AA)', () => {
    const lowest = worst(light, TEXT_PAIRS)
    expect(lowest.ratio, `cặp thấp nhất: ${lowest.pair}`).toBeGreaterThanOrEqual(4.5)
  })

  it('chữ và nền đạt tương phản tối thiểu 4,5:1 ở chế độ tối (WCAG AA)', () => {
    const lowest = worst(dark, TEXT_PAIRS)
    expect(lowest.ratio, `cặp thấp nhất: ${lowest.pair}`).toBeGreaterThanOrEqual(4.5)
  })

  it('viền ô nhập và nút phụ đạt tối thiểu 3:1 với nền thẻ ở cả hai chế độ (WCAG 1.4.11)', () => {
    expect(contrast(light['line-strong'], light.surface)).toBeGreaterThanOrEqual(3)
    expect(contrast(dark['line-strong'], dark.surface)).toBeGreaterThanOrEqual(3)
  })
})

describe('hệ thống thiết kế — bàn phím, cảm ứng và thanh trình duyệt', () => {
  it('cuộn tới phần tử đang focus chừa chỗ cho thanh đầu trang dính, không để thanh che mất', () => {
    expect(css).toMatch(/html\s*\{[^}]*scroll-padding-top:\s*[\d.]+rem/)
  })

  it('đặt màu nháy khi chạm trên điện thoại có chủ ý', () => {
    expect(css).toContain('-webkit-tap-highlight-color')
  })

  it('index.html khai báo theme-color khớp màu nền ở cả hai chế độ', () => {
    const html = readFileSync(new URL('../index.html', import.meta.url), 'utf-8')
    expect(html).toContain(`media="(prefers-color-scheme: light)"`)
    expect(html).toContain(`media="(prefers-color-scheme: dark)"`)
    expect(html).toMatch(new RegExp(`name="theme-color"[^>]*content="${light.paper}"`, 'i'))
    expect(html).toMatch(new RegExp(`name="theme-color"[^>]*content="${dark.paper}"`, 'i'))
  })
})

describe('hệ thống thiết kế — kiểu chữ và khung', () => {
  it('không còn chữ có chân (serif): mọi tiêu đề dùng chung font không chân', () => {
    expect(css).not.toContain('--font-serif')
    const using = appFiles.filter(([, source]) => source.includes('font-serif')).map(([file]) => file)
    expect(using).toEqual([])
  })

  it('không còn gạch chân đen dày dưới tiêu đề mục và vạch trái làm khung cảnh báo', () => {
    const old = appFiles
      .filter(([, source]) => /border-[bt]-2 border-ink|border-l-4 border-(?:danger|success|warn)/.test(source))
      .map(([file]) => file)
    expect(old).toEqual([])
  })

  it('thẻ và khung dùng góc bo: bán kính nhỏ nhất của nút và ô nhập là 8px', () => {
    expect(css).toMatch(/--radius-md:\s*8px/)
    expect(css).toMatch(/--radius-lg:\s*12px/)
  })
})
