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

// Số liệu tổng hợp của trang Tổng quan (GET /api/admin/dashboard). Chỉ có số, không có thông tin của ai.
export type AdminDashboard = {
  users: { total: number; newLast7Days: number }
  lessons: { total: number; draft: number; published: number }
  courses: { courseId: number; slug: string; title: string; draft: number; published: number }[]
  learning: { activeLearners: number; completedLessons: number }
}

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

function parseAdminDashboard(value: unknown): AdminDashboard {
  const record = asRecord(value)
  const users = asRecord(record.users)
  const lessons = asRecord(record.lessons)
  const learning = asRecord(record.learning)
  return {
    users: { total: asNumber(users.total), newLast7Days: asNumber(users.newLast7Days) },
    lessons: {
      total: asNumber(lessons.total),
      draft: asNumber(lessons.draft),
      published: asNumber(lessons.published),
    },
    courses: asList(record.courses, (item) => {
      const course = asRecord(item)
      return {
        courseId: asNumber(course.courseId),
        slug: asString(course.slug),
        title: asString(course.title),
        draft: asNumber(course.draft),
        published: asNumber(course.published),
      }
    }),
    learning: {
      activeLearners: asNumber(learning.activeLearners),
      completedLessons: asNumber(learning.completedLessons),
    },
  }
}

export async function fetchAdminDashboard(): Promise<AdminDashboard> {
  return parseAdminDashboard(await apiRequest('GET', '/api/admin/dashboard'))
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
