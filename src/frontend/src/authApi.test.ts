import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './client'
import { changePassword, fetchCurrentUser, login, logout, register } from './authApi'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./client')>()
  return { ...actual, apiRequest: vi.fn() }
})

const apiRequestMock = vi.mocked(apiRequest)

beforeEach(() => {
  apiRequestMock.mockReset()
})

describe('fetchCurrentUser', () => {
  it('gọi GET /api/me và trả về tên, email, vai trò', async () => {
    apiRequestMock.mockResolvedValue({
      displayName: 'Nguyễn Văn A',
      email: 'a@example.com',
      role: 'LEARNER',
      authProvider: 'LOCAL',
      createdAt: '2026-11-01T09:30:00',
    })

    const user = await fetchCurrentUser()

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/me')
    expect(user).toEqual({ displayName: 'Nguyễn Văn A', email: 'a@example.com', role: 'LEARNER' })
  })

  it('chưa đăng nhập (401) thì trả về null, không ném lỗi', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(401, 'Chưa đăng nhập'))

    await expect(fetchCurrentUser()).resolves.toBeNull()
  })

  it('lỗi khác 401 (mất kết nối, 500) thì ném lại, không giả làm "chưa đăng nhập"', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(0, 'Không kết nối được tới máy chủ'))

    await expect(fetchCurrentUser()).rejects.toMatchObject({ status: 0 })
  })

  it('ném lỗi khi vai trò không phải LEARNER hay ADMIN', async () => {
    apiRequestMock.mockResolvedValue({ displayName: 'A', email: 'a@example.com', role: 'ROOT' })

    await expect(fetchCurrentUser()).rejects.toThrow()
  })

  it('ném lỗi khi thiếu tên hiển thị', async () => {
    apiRequestMock.mockResolvedValue({ email: 'a@example.com', role: 'LEARNER' })

    await expect(fetchCurrentUser()).rejects.toThrow()
  })
})

describe('login', () => {
  it('gọi POST /api/auth/login với email và mật khẩu, trả về người dùng', async () => {
    apiRequestMock.mockResolvedValue({ displayName: 'A', email: 'a@example.com', role: 'ADMIN' })

    const user = await login('a@example.com', 'matkhau123')

    expect(apiRequestMock).toHaveBeenCalledWith('POST', '/api/auth/login', {
      email: 'a@example.com',
      password: 'matkhau123',
    })
    expect(user).toEqual({ displayName: 'A', email: 'a@example.com', role: 'ADMIN' })
  })

  it('không đổi mật khẩu (không cắt khoảng trắng) trước khi gửi', async () => {
    apiRequestMock.mockResolvedValue({ displayName: 'A', email: 'a@example.com', role: 'LEARNER' })

    await login('a@example.com', '  co khoang trang  ')

    expect(apiRequestMock).toHaveBeenCalledWith('POST', '/api/auth/login', {
      email: 'a@example.com',
      password: '  co khoang trang  ',
    })
  })

  it('đăng nhập sai (401) thì ném lại lỗi để trang hiện thông báo', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(401, 'Email hoặc mật khẩu không chính xác'))

    await expect(login('a@example.com', 'sai')).rejects.toMatchObject({
      status: 401,
      message: 'Email hoặc mật khẩu không chính xác',
    })
  })

  it('ném lỗi khi phản hồi thành công nhưng sai dạng', async () => {
    apiRequestMock.mockResolvedValue({ ok: true })

    await expect(login('a@example.com', 'matkhau123')).rejects.toThrow()
  })
})

describe('register', () => {
  it('gọi POST /api/auth/register với đủ 4 trường', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await register({
      displayName: 'Nguyễn Văn A',
      email: 'a@example.com',
      password: 'matkhau123',
      confirmPassword: 'matkhau123',
    })

    expect(apiRequestMock).toHaveBeenCalledWith('POST', '/api/auth/register', {
      displayName: 'Nguyễn Văn A',
      email: 'a@example.com',
      password: 'matkhau123',
      confirmPassword: 'matkhau123',
    })
  })

  it('email đã tồn tại (409) thì ném lại lỗi', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(409, 'Email đã được đăng ký'))

    await expect(
      register({ displayName: 'A', email: 'a@example.com', password: 'matkhau123', confirmPassword: 'matkhau123' }),
    ).rejects.toMatchObject({ status: 409 })
  })
})

describe('logout', () => {
  it('gọi POST /api/auth/logout', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await logout()

    expect(apiRequestMock).toHaveBeenCalledWith('POST', '/api/auth/logout')
  })
})

describe('changePassword', () => {
  it('gọi PUT /api/me/password với mật khẩu cũ, mới và xác nhận', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await changePassword({
      currentPassword: 'matkhau-cu-1',
      newPassword: 'matkhau-moi-2',
      confirmNewPassword: 'matkhau-moi-2',
    })

    expect(apiRequestMock).toHaveBeenCalledWith('PUT', '/api/me/password', {
      currentPassword: 'matkhau-cu-1',
      newPassword: 'matkhau-moi-2',
      confirmNewPassword: 'matkhau-moi-2',
    })
  })

  it('sai mật khẩu hiện tại (400) thì ném lại lỗi', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(400, 'Mật khẩu hiện tại không đúng'))

    await expect(
      changePassword({ currentPassword: 'sai', newPassword: 'matkhau-moi-2', confirmNewPassword: 'matkhau-moi-2' }),
    ).rejects.toMatchObject({ status: 400, message: 'Mật khẩu hiện tại không đúng' })
  })
})
