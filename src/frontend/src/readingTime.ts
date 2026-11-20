// Tốc độ đọc ước tính của học sinh với văn bản tiếng Việt, tính theo từ (âm tiết) mỗi phút.
const WORDS_PER_MINUTE = 200

// Số phút đọc ước tính của một bài Markdown, tối thiểu 1. Địa chỉ trong ảnh và liên kết không phải thứ người học đọc
// nên bị bỏ trước khi đếm; ký hiệu Markdown đứng riêng (#, -, >) cũng không tính là từ.
export function readingMinutes(markdown: string): number {
  const text = markdown.replace(/\]\([^)]*\)/g, ']')
  const words = text.split(/\s+/).filter((token) => /[\p{L}\p{N}]/u.test(token)).length
  return Math.max(1, Math.ceil(words / WORDS_PER_MINUTE))
}
