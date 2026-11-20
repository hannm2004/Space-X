// @vitest-environment node
/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// Vitest không trả nội dung CSS qua "?raw" (ra chuỗi rỗng), nên đọc thẳng tệp.
const indexCss = readFileSync(new URL('./index.css', import.meta.url), 'utf-8')

// Test "danh mục" cho nút bấm: quét toàn bộ mã nguồn để nút ở mọi trang đi theo cùng một bộ kiểu (index.css, buttons.ts),
// thay vì mỗi trang tự ghép màu/viền. Thêm nút mới sai chuẩn thì test này báo ngay, đỡ phải rà bằng mắt.
const sources = import.meta.glob('./**/*.{ts,tsx}', { query: '?raw', import: 'default', eager: true }) as Record<
  string,
  string
>
const appFiles = Object.entries(sources).filter(([path]) => !/\.test\.tsx?$/.test(path))

type Tag = { file: string; line: number; name: string; text: string }

// Lấy phần mở đầu của thẻ (từ "<" tới ">" ngoài dấu ngoặc nhọn), để không bị dấu ">" của hàm mũi tên "=>" đánh lừa.
function openingTags(names: string[]): Tag[] {
  const tags: Tag[] = []
  const start = new RegExp(`<(${names.join('|')})\\b`, 'g')
  for (const [file, source] of appFiles) {
    for (const match of source.matchAll(start)) {
      let depth = 0
      let end = match.index
      for (; end < source.length; end++) {
        const ch = source[end]
        if (ch === '{') depth++
        else if (ch === '}') depth--
        else if (ch === '>' && depth === 0) break
      }
      tags.push({
        file,
        line: source.slice(0, match.index).split('\n').length,
        name: match[1],
        text: source.slice(match.index, end + 1),
      })
    }
  }
  return tags
}

const where = (tag: Tag) => `${tag.file}:${tag.line}`
// "link" là nút dạng chữ có gạch chân (ví dụ "Thử lại" trong dòng chữ), cùng kiểu với liên kết trong bài.
const BUTTON_STYLE = /\bbtn\b|\bbtn-icon\b|\blink\b|\bPRIMARY\b|\bSECONDARY\b|\bDANGER\b/
// Những thứ nút không được tự ghép: màu/viền (đã có trong btn-*), kiểu hiển thị (đè lên inline-flex của .btn), cỡ đệm.
const RAW_UTILITIES = /(?:^|[\s'"`:])(?:(?:hover:|focus:)?(?:bg|border)-[a-z]|inline-block|block|inline-flex|flex|rounded|px-\d|py-\d)/

describe('nút bấm — dùng chung một bộ kiểu', () => {
  it('quét được mã nguồn (tránh test rỗng cho qua)', () => {
    expect(openingTags(['button']).length).toBeGreaterThan(20)
  })

  it('mọi <button> có type rõ ràng, để không vô tình gửi form', () => {
    const missing = openingTags(['button']).filter((tag) => !/\btype=/.test(tag.text))
    expect(missing.map(where)).toEqual([])
  })

  it('mọi <button> dùng bộ kiểu nút chung (btn, btn-icon hoặc PRIMARY/SECONDARY/DANGER)', () => {
    const plain = openingTags(['button']).filter((tag) => !BUTTON_STYLE.test(tag.text))
    expect(plain.map(where)).toEqual([])
  })

  it('liên kết trông như nút (có btn) không tự ghép màu, viền hay kiểu hiển thị', () => {
    const offenders = openingTags(['button', 'Link', 'a'])
      .filter((tag) => /\bbtn\b/.test(tag.text))
      .filter((tag) => RAW_UTILITIES.test(tag.text.replace(/\bbtn(?:-[a-z]+)?\b/g, '')))
    expect(offenders.map(where)).toEqual([])
  })

  it('SECONDARY, DANGER, PRIMARY chỉ khai báo một nơi (buttons.ts)', () => {
    const declared = appFiles
      .filter(([, source]) => /\bconst\s+(?:PRIMARY|SECONDARY|DANGER|BUTTON)\s*=/.test(source))
      .map(([file]) => file)
    expect(declared).toEqual(['./buttons.ts'])
  })

  // Liên kết đứng riêng (về danh sách, quay lại, phân trang, thêm mới...) là thao tác chứ không phải chữ trong câu, nên
  // phải trông như nút. Liên kết chen trong câu (đăng ký/đăng nhập, chính sách) vẫn là chữ gạch chân.
  it('liên kết điều hướng đứng riêng ở trang Admin, khóa học, bài học và hồ sơ là nút, không phải chữ gạch chân', () => {
    const standalone = /^\.\/pages\/(?:Admin\w+|CoursePage|LessonPage|ProfilePage)\.tsx$/
    const plain = openingTags(['Link', 'a'])
      .filter((tag) => standalone.test(tag.file))
      .filter((tag) => /className=(?:\{LINK\}|["']link\b)/.test(tag.text))
    expect(plain.map(where)).toEqual([])
  })

  it('ô chọn tệp chỉ xuất hiện trong FilePickButton, nơi khác dùng nút bấm đó', () => {
    const usesFileInput = appFiles
      .filter(([, source]) => /type=["']file["']/.test(source))
      .map(([file]) => file)
    expect(usesFileInput).toEqual(['./FilePickButton.tsx'])
  })

  it('không chép lại chuỗi class của nút phụ/nút xóa ở nơi khác, hãy import từ buttons.ts', () => {
    const copied = appFiles
      .filter(([file]) => file !== './buttons.ts')
      .filter(([, source]) => /btn btn-(?:secondary|danger) btn-sm/.test(source))
      .map(([file]) => file)
    expect(copied).toEqual([])
  })
})

describe('nút bấm — kiểu trong index.css', () => {
  it('có viền hiện khi duyệt bằng bàn phím (:focus-visible)', () => {
    expect(indexCss).toContain(':focus-visible')
  })

  it('nút bấm không bị trễ khi chạm hai lần liên tiếp trên điện thoại (touch-action: manipulation)', () => {
    expect(indexCss).toMatch(/\.btn\s*\{[^}]*touch-action:\s*manipulation/)
    expect(indexCss).toMatch(/\.btn-icon\s*\{[^}]*touch-action:\s*manipulation/)
  })

  it('có nút trung tính cho đăng nhập Google, không phải ghép class thô', () => {
    expect(indexCss).toContain('.btn-neutral')
  })

  it('nút biểu tượng và nút nhỏ đủ cao để chạm trên điện thoại', () => {
    expect(indexCss).toMatch(/\.btn-icon\s*\{[^}]*min-height/)
    expect(indexCss).toMatch(/@media \(pointer: coarse\)/)
  })
})
