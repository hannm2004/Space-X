import type { Role } from './authApi'
import { apiRequest } from './client'
import { asBoolean, asList, asNullableString, asNumber, asRecord, asString, BAD_SHAPE } from './parse'

export type AuthProvider = 'LOCAL' | 'GOOGLE'

// Một dòng trong danh sách người dùng (docs/API.md). Máy chủ không bao giờ gửi mã băm mật khẩu.
export type AdminUser = {
  id: number
  email: string
  displayName: string
  role: Role
  authProvider: AuthProvider
  enabled: boolean
  createdAt: string | null
}
export type AdminUserPage = { items: AdminUser[]; page: number; size: number; totalItems: number }
// Mật khẩu tạm chỉ có trong phản hồi này, một lần duy nhất.
export type TemporaryPassword = { id: number; email: string; temporaryPassword: string }

const USER_PAGE_SIZE = 20

function asRole(value: unknown): Role {
  if (value !== 'LEARNER' && value !== 'ADMIN') throw new Error(BAD_SHAPE)
  return value
}

function asAuthProvider(value: unknown): AuthProvider {
  if (value !== 'LOCAL' && value !== 'GOOGLE') throw new Error(BAD_SHAPE)
  return value
}

function parseAdminUser(value: unknown): AdminUser {
  const record = asRecord(value)
  return {
    id: asNumber(record.id),
    email: asString(record.email),
    displayName: asString(record.displayName),
    role: asRole(record.role),
    authProvider: asAuthProvider(record.authProvider),
    enabled: asBoolean(record.enabled),
    createdAt: asNullableString(record.createdAt),
  }
}

function parseAdminUserPage(value: unknown): AdminUserPage {
  const record = asRecord(value)
  return {
    items: asList(record.items, parseAdminUser),
    page: asNumber(record.page),
    size: asNumber(record.size),
    totalItems: asNumber(record.totalItems),
  }
}

function parseTemporaryPassword(value: unknown): TemporaryPassword {
  const record = asRecord(value)
  return {
    id: asNumber(record.id),
    email: asString(record.email),
    temporaryPassword: asString(record.temporaryPassword),
  }
}

// Từ khóa đi qua URLSearchParams nên ký tự như & = # không chen thêm tham số; từ khóa trống thì bỏ tham số q.
export async function fetchAdminUsers(query: string, page: number): Promise<AdminUserPage> {
  const params = new URLSearchParams()
  const trimmed = query.trim()
  if (trimmed !== '') params.set('q', trimmed)
  params.set('page', String(page))
  params.set('size', String(USER_PAGE_SIZE))
  return parseAdminUserPage(await apiRequest('GET', `/api/admin/users?${params}`))
}

// Trả về người dùng sau khi đổi để giao diện hiện đúng trạng thái máy chủ đã ghi, không tự đoán.
export async function setUserEnabled(id: number, enabled: boolean): Promise<AdminUser> {
  return parseAdminUser(await apiRequest('PATCH', `/api/admin/users/${id}/status`, { enabled }))
}

export async function resetUserPassword(id: number): Promise<TemporaryPassword> {
  return parseTemporaryPassword(await apiRequest('POST', `/api/admin/users/${id}/reset-password`))
}
