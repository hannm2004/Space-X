import { ApiError, apiRequest } from './client'

export type Role = 'LEARNER' | 'ADMIN'
export type CurrentUser = { displayName: string; email: string; role: Role }

export type LoginMethod = 'LOCAL' | 'GOOGLE'
// Hồ sơ đầy đủ của GET /api/me. Phản hồi đăng nhập không có hai trường sau, nên CurrentUser giữ riêng ba trường chung.
export type Profile = CurrentUser & { authProvider: LoginMethod; createdAt: string }

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

function parseProfile(value: unknown): Profile {
  const user = parseUser(value)
  const { authProvider, createdAt } = value as Record<string, unknown>
  if ((authProvider === 'LOCAL' || authProvider === 'GOOGLE') && typeof createdAt === 'string') {
    return { ...user, authProvider, createdAt }
  }
  throw new Error('Hồ sơ từ máy chủ không đúng dạng')
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

// Khác fetchCurrentUser: 401 ném lại, vì trang hồ sơ tự chuyển sang đăng nhập khi AuthContext báo chưa đăng nhập.
export async function fetchProfile(): Promise<Profile> {
  return parseProfile(await apiRequest('GET', '/api/me'))
}

// Chỉ tên hiển thị được sửa; email và vai trò do máy chủ giữ (docs/API.md).
export async function updateDisplayName(displayName: string): Promise<Profile> {
  return parseProfile(await apiRequest('PUT', '/api/me', { displayName }))
}

export async function changePassword(input: ChangePasswordInput): Promise<void> {
  await apiRequest('PUT', '/api/me/password', input)
}
