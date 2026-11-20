import { describe, expect, it } from 'vitest'
import { readingMinutes } from './readingTime'

const words = (count: number) => Array.from({ length: count }, (_, i) => `từ${i}`).join(' ')

describe('readingMinutes', () => {
  it('bài rỗng hoặc rất ngắn vẫn tính tối thiểu 1 phút, không bao giờ là 0', () => {
    expect(readingMinutes('')).toBe(1)
    expect(readingMinutes('Chỉ vài từ.')).toBe(1)
  })

  it('tính 200 từ một phút và làm tròn lên', () => {
    expect(readingMinutes(words(200))).toBe(1)
    expect(readingMinutes(words(201))).toBe(2)
    expect(readingMinutes(words(1000))).toBe(5)
  })

  it('không đếm địa chỉ ảnh hay liên kết, chỉ đếm chữ người học đọc', () => {
    const url = 'https://example.com/' + 'x'.repeat(300)

    // Chữ thật: 196 + "Sơ", "đồ", "xem", "thêm" = đúng 200 từ → 1 phút. Nếu đếm cả hai địa chỉ thì thành 202 → 2 phút.
    expect(readingMinutes(`${words(196)} ![Sơ đồ](${url}) [xem thêm](${url})`)).toBe(1)
  })

  it('ký hiệu Markdown đứng riêng (#, -, >) không bị đếm thành từ', () => {
    // Chữ thật: "Tiêu", "đề" + 99 + 99 = đúng 200 từ → 1 phút. Nếu đếm cả ba ký hiệu thì thành 203 → 2 phút.
    expect(readingMinutes(`# Tiêu đề\n\n- ${words(99)}\n\n> ${words(99)}\n`)).toBe(1)
  })
})
