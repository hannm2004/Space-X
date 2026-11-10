import { describe, expect, it } from 'vitest'
import { ApiError } from './client'
import { errorMessages } from './formErrors'

const FALLBACK = 'Không đăng nhập được.'

describe('errorMessages', () => {
  it('mất kết nối (status 0) thì báo kiểm tra mạng', () => {
    expect(errorMessages(new ApiError(0, 'Không kết nối được tới máy chủ'), FALLBACK)).toEqual([
      'Không kết nối được tới máy chủ. Kiểm tra mạng rồi thử lại.',
    ])
  })

  it('403 (thiếu hoặc sai CSRF) thì bảo tải lại trang, không hiện nội dung thô của máy chủ', () => {
    expect(errorMessages(new ApiError(403, 'Máy chủ trả mã lỗi 403'), FALLBACK)).toEqual([
      'Phiên làm việc đã hết hạn hoặc không hợp lệ. Hãy tải lại trang rồi thử lại.',
    ])
  })

  it('lỗi 5xx thì báo máy chủ đang gặp lỗi', () => {
    expect(errorMessages(new ApiError(503, 'Máy chủ trả mã lỗi 503'), FALLBACK)).toEqual([
      'Máy chủ đang gặp lỗi. Hãy thử lại sau.',
    ])
  })

  it('có danh sách lỗi theo trường thì hiện từng câu của máy chủ', () => {
    const error = new ApiError(400, 'Dữ liệu không hợp lệ', {
      detail: 'Dữ liệu không hợp lệ',
      fieldErrors: [
        { field: 'email', message: 'Email không đúng định dạng' },
        { field: 'password', message: 'Mật khẩu tối thiểu 8 ký tự' },
      ],
    })

    expect(errorMessages(error, FALLBACK)).toEqual([
      'Email không đúng định dạng',
      'Mật khẩu tối thiểu 8 ký tự',
    ])
  })

  it('lỗi 4xx có "detail" của máy chủ thì dùng đúng câu đó', () => {
    const error = new ApiError(401, 'Email hoặc mật khẩu không chính xác', {
      detail: 'Email hoặc mật khẩu không chính xác',
    })

    expect(errorMessages(error, FALLBACK)).toEqual(['Email hoặc mật khẩu không chính xác'])
  })

  it('lỗi 4xx không có "detail" thì dùng câu dự phòng của trang, không hiện "mã lỗi 4xx"', () => {
    expect(errorMessages(new ApiError(400, 'Máy chủ trả mã lỗi 400'), FALLBACK)).toEqual([FALLBACK])
  })

  it('lỗi không phải ApiError thì dùng câu dự phòng', () => {
    expect(errorMessages(new Error('chi tiet noi bo'), FALLBACK)).toEqual([FALLBACK])
  })
})
