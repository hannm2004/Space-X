import { ApiError, apiRequest } from './client'

export type Role = 'LEARNER' | 'ADMIN'
export type CurrentUser = { displayName: string; email: string; role: Role }

export type RegisterInput = {
  displayName: string
  email: string
  password: string
  confirmPassword: string
}

export type ChangePasswordInput = {
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}

// Phản hồi đăng nhập và /api/me đều có ba trường này; kiểm đúng dạng thay vì tin dữ liệu từ mạng.
function parseUser(value: unknown): CurrentUser {
  if (typeof value === 'object' && value !== null) {
    const { displayName, email, role } = value as Record<string, unknown>
    if (
      typeof displayName === 'string' &&
      typeof email === 'string' &&
      (role === 'LEARNER' || role === 'ADMIN')
    ) {
      return { displayName, email, role }
    }
  }
  throw new Error('Thông tin người dùng từ máy chủ không đúng dạng')
}

// null nghĩa là chưa đăng nhập (401). Các lỗi khác (mất mạng, 500) ném lại để không nhầm với "chưa đăng nhập".
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    return parseUser(await apiRequest('GET', '/api/me'))
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null
    throw error
  }
}

// Mật khẩu gửi nguyên văn, không cắt khoảng trắng (khoảng trắng có thể là một phần của mật khẩu, D25).
export async function login(email: string, password: string): Promise<CurrentUser> {
  return parseUser(await apiRequest('POST', '/api/auth/login', { email, password }))
}

export async function register(input: RegisterInput): Promise<void> {
  await apiRequest('POST', '/api/auth/register', input)
}

export async function logout(): Promise<void> {
  await apiRequest('POST', '/api/auth/logout')
}

export async function changePassword(input: ChangePasswordInput): Promise<void> {
  await apiRequest('PUT', '/api/me/password', input)
}
