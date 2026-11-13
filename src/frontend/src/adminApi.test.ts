import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchAdminUsers, resetUserPassword, setUserEnabled } from './adminApi'
import { ApiError, apiRequest } from './client'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./client')>()
  return { ...actual, apiRequest: vi.fn() }
})

const apiRequestMock = vi.mocked(apiRequest)

const USER_JSON = {
  id: 7,
  email: 'hs@example.com',
  displayName: 'Học sinh',
  role: 'LEARNER',
  authProvider: 'LOCAL',
  enabled: true,
  createdAt: '2026-11-01T08:00:00',
}

beforeEach(() => {
  apiRequestMock.mockReset()
})

describe('fetchAdminUsers', () => {
  it('gọi GET với trang và cỡ trang, bỏ tham số q khi từ khóa trống, và trả về trang người dùng', async () => {
    apiRequestMock.mockResolvedValue({ items: [USER_JSON], page: 0, size: 20, totalItems: 1 })

    const result = await fetchAdminUsers('', 0)

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/admin/users?page=0&size=20')
    expect(result).toEqual({
      items: [
        {
          id: 7,
          email: 'hs@example.com',
          displayName: 'Học sinh',
          role: 'LEARNER',
          authProvider: 'LOCAL',
          enabled: true,
          createdAt: '2026-11-01T08:00:00',
        },
      ],
      page: 0,
      size: 20,
      totalItems: 1,
    })
  })

  it('từ khóa đi qua URLSearchParams nên ký tự & = # % không chen thêm tham số', async () => {
    apiRequestMock.mockResolvedValue({ items: [], page: 2, size: 20, totalItems: 0 })

    await fetchAdminUsers('a&b=c#d%', 2)

    expect(apiRequestMock).toHaveBeenCalledWith(
      'GET',
      `/api/admin/users?${new URLSearchParams({ q: 'a&b=c#d%', page: '2', size: '20' })}`,
    )
  })

  it('từ khóa chỉ có khoảng trắng coi như không tìm kiếm', async () => {
    apiRequestMock.mockResolvedValue({ items: [], page: 0, size: 20, totalItems: 0 })

    await fetchAdminUsers('   ', 0)

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/admin/users?page=0&size=20')
  })

  it('nhận createdAt là null', async () => {
    apiRequestMock.mockResolvedValue({
      items: [{ ...USER_JSON, createdAt: null }],
      page: 0,
      size: 20,
      totalItems: 1,
    })

    const result = await fetchAdminUsers('', 0)

    expect(result.items[0].createdAt).toBeNull()
  })

  it('ném lỗi khi phản hồi sai dạng: không phải đối tượng, thiếu items, role/authProvider lạ, enabled hay id sai kiểu', async () => {
    const wrongShapes: unknown[] = [
      'abc',
      { page: 0, size: 20, totalItems: 0 },
      { items: [{ ...USER_JSON, role: 'ROOT' }], page: 0, size: 20, totalItems: 1 },
      { items: [{ ...USER_JSON, authProvider: 'FACEBOOK' }], page: 0, size: 20, totalItems: 1 },
      { items: [{ ...USER_JSON, enabled: 'true' }], page: 0, size: 20, totalItems: 1 },
      { items: [{ ...USER_JSON, id: '7' }], page: 0, size: 20, totalItems: 1 },
    ]
    for (const payload of wrongShapes) {
      apiRequestMock.mockResolvedValueOnce(payload)
      await expect(fetchAdminUsers('', 0)).rejects.toThrow('không đúng dạng')
    }
  })

  it('giữ nguyên ApiError 403 khi không phải Admin', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(403, 'Bạn không có quyền thực hiện thao tác này'))

    await expect(fetchAdminUsers('', 0)).rejects.toMatchObject({ status: 403 })
  })
})

describe('setUserEnabled', () => {
  it('gọi PATCH đúng đường dẫn với { enabled } và trả về người dùng sau khi đổi', async () => {
    apiRequestMock.mockResolvedValue({ ...USER_JSON, enabled: false })

    const user = await setUserEnabled(7, false)

    expect(apiRequestMock).toHaveBeenCalledWith('PATCH', '/api/admin/users/7/status', { enabled: false })
    expect(user.enabled).toBe(false)
    expect(user.email).toBe('hs@example.com')
  })

  it('giữ nguyên ApiError 409 để trang hiện lý do của máy chủ', async () => {
    apiRequestMock.mockRejectedValue(
      new ApiError(409, 'Không thể tự khóa tài khoản của chính mình', {
        detail: 'Không thể tự khóa tài khoản của chính mình',
      }),
    )

    await expect(setUserEnabled(1, false)).rejects.toMatchObject({
      status: 409,
      detail: 'Không thể tự khóa tài khoản của chính mình',
    })
  })

  it('ném lỗi khi phản hồi sai dạng', async () => {
    apiRequestMock.mockResolvedValue({ id: 7 })

    await expect(setUserEnabled(7, true)).rejects.toThrow('không đúng dạng')
  })
})

describe('resetUserPassword', () => {
  it('gọi POST đúng đường dẫn, không gửi thân, và trả về mật khẩu tạm', async () => {
    apiRequestMock.mockResolvedValue({ id: 7, email: 'hs@example.com', temporaryPassword: 'tam-thoi-gia-lap' })

    const result = await resetUserPassword(7)

    expect(apiRequestMock).toHaveBeenCalledWith('POST', '/api/admin/users/7/reset-password')
    expect(result).toEqual({ id: 7, email: 'hs@example.com', temporaryPassword: 'tam-thoi-gia-lap' })
  })

  it('ném lỗi khi thiếu mật khẩu tạm trong phản hồi', async () => {
    apiRequestMock.mockResolvedValue({ id: 7, email: 'hs@example.com' })

    await expect(resetUserPassword(7)).rejects.toThrow('không đúng dạng')
  })

  it('giữ nguyên ApiError 409 (tài khoản Google)', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(409, 'Tài khoản Google không có mật khẩu để đặt lại'))

    await expect(resetUserPassword(7)).rejects.toMatchObject({ status: 409 })
  })
})
