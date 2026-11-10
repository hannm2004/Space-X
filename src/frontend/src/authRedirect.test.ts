import { describe, expect, it } from 'vitest'
import { readRedirectPath } from './authRedirect'

describe('readRedirectPath', () => {
  it('nhận đường dẫn nội bộ hợp lệ trong state.from', () => {
    expect(readRedirectPath({ from: '/bai/thong-tin-va-du-lieu' })).toBe('/bai/thong-tin-va-du-lieu')
  })

  it('không có state hoặc state sai dạng thì trả về null', () => {
    expect(readRedirectPath(undefined)).toBeNull()
    expect(readRedirectPath(null)).toBeNull()
    expect(readRedirectPath('/bai/abc')).toBeNull()
    expect(readRedirectPath({})).toBeNull()
    expect(readRedirectPath({ from: 123 })).toBeNull()
  })

  it('từ chối địa chỉ sang trang khác (open redirect)', () => {
    expect(readRedirectPath({ from: 'https://evil.example/bai' })).toBeNull()
    expect(readRedirectPath({ from: '//evil.example' })).toBeNull()
    expect(readRedirectPath({ from: '/\\evil.example' })).toBeNull()
    expect(readRedirectPath({ from: 'javascript:alert(1)' })).toBeNull()
    expect(readRedirectPath({ from: 'bai/khong-bat-dau-bang-gach' })).toBeNull()
  })

  it('từ chối đường dẫn có ký tự điều khiển hoặc khoảng trắng', () => {
    expect(readRedirectPath({ from: '/bai/a\nb' })).toBeNull()
    expect(readRedirectPath({ from: '/bai/a b' })).toBeNull()
  })

  it('từ chối đường dẫn quá dài', () => {
    expect(readRedirectPath({ from: '/' + 'a'.repeat(300) })).toBeNull()
  })

  it('từ chối chính trang đăng nhập và đăng ký để không quay vòng', () => {
    expect(readRedirectPath({ from: '/dang-nhap' })).toBeNull()
    expect(readRedirectPath({ from: '/dang-ky' })).toBeNull()
  })
})
