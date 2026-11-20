import { describe, expect, it } from 'vitest'
import { IMAGE_TYPES, MAX_IMAGE_BYTES, imageProblem } from './imageUpload'

function file(type: string, size = 10) {
  return new File([new Uint8Array(size)], 'x', { type })
}

describe('imageProblem', () => {
  it('nhận PNG, JPEG và WebP cỡ nhỏ', () => {
    expect(imageProblem(file('image/png'))).toBeNull()
    expect(imageProblem(file('image/jpeg'))).toBeNull()
    expect(imageProblem(file('image/webp'))).toBeNull()
  })

  it('từ chối loại khác, kể cả GIF và SVG (máy chủ cũng không nhận)', () => {
    for (const type of ['image/gif', 'image/svg+xml', 'text/plain', '']) {
      expect(imageProblem(file(type))).toBe('Chỉ nhận ảnh PNG, JPEG hoặc WebP.')
    }
  })

  it('đúng 1 MB còn nhận, hơn 1 MB bị từ chối', () => {
    expect(imageProblem(file('image/png', MAX_IMAGE_BYTES))).toBeNull()
    expect(imageProblem(file('image/png', MAX_IMAGE_BYTES + 1))).toBe('Ảnh lớn hơn 1 MB. Hãy chọn ảnh nhỏ hơn.')
  })

  it('giới hạn khớp máy chủ (1 MiB) và đúng ba loại ảnh', () => {
    expect(MAX_IMAGE_BYTES).toBe(1_048_576)
    expect(IMAGE_TYPES).toEqual(['image/png', 'image/jpeg', 'image/webp'])
  })
})
