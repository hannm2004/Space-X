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

// ---- Khóa học, bài học, ảnh (CP6.4). Hình dạng khớp mục "Quản trị" của docs/API.md. ----

// DRAFT: đang soạn; PUBLISHED: đang hiển thị; ARCHIVED: ngừng bán.
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type CourseLevel = 'BASIC' | 'ADVANCED' | 'EXPERT'
// Nhóm chương trình (GET /api/categories, công khai).
export type Category = { id: number; slug: string; name: string }
export type AdminCourse = {
  categoryId: number
  level: CourseLevel
  // 0 là chưa nhập số giờ.
  totalHours: number
  id: number
  slug: string
  title: string
  description: string | null
  sortOrder: number
  status: CourseStatus
  // Đồng Việt Nam, số nguyên; 0 là chưa có học phí.
  tuitionVnd: number
  // Ảnh bìa khóa (công khai); null khi chưa có. Thiếu trường (máy chủ cũ) coi như null.
  coverImageUrl?: string | null
}
// Dữ liệu gửi lên khi tạo/sửa khóa: mô tả để trống thì gửi chuỗi rỗng, máy chủ lưu null.
export type CourseInput = {
  slug: string
  title: string
  description: string
  sortOrder: number
  status: CourseStatus
  tuitionVnd: number
  categoryId: number
  level: CourseLevel
  totalHours: number
}

export type LessonStatus = 'DRAFT' | 'PUBLISHED'
// Một dòng trong danh sách bài (không có nội dung); AdminLesson là bài đầy đủ để sửa.
export type AdminLessonSummary = {
  id: number
  courseId: number
  slug: string
  title: string
  summary: string | null
  status: LessonStatus
  sortOrder: number
}
export type AdminLesson = AdminLessonSummary & { contentMd: string }
export type AdminLessonPage = { items: AdminLessonSummary[]; page: number; size: number; totalItems: number }
export type LessonInput = {
  courseId: number
  slug: string
  title: string
  summary: string
  contentMd: string
  sortOrder: number
}
export type LessonFilter = { courseId: number | null; status: LessonStatus | null; page: number }

// cover: ảnh này đang là ảnh bìa của bài (mỗi bài tối đa một ảnh bìa; ảnh bìa hiện ở danh sách bài, kết quả tìm kiếm, đầu trang bài).
export type LessonImage = {
  id: number
  fileName: string
  contentType: string
  sizeBytes: number
  url: string
  cover: boolean
}
export type UploadedImage = { id: number; url: string }

const LESSON_PAGE_SIZE = 20

function asCourseStatus(value: unknown): CourseStatus {
  if (value !== 'DRAFT' && value !== 'PUBLISHED' && value !== 'ARCHIVED') throw new Error(BAD_SHAPE)
  return value
}

function asCourseLevel(value: unknown): CourseLevel {
  if (value !== 'BASIC' && value !== 'ADVANCED' && value !== 'EXPERT') throw new Error(BAD_SHAPE)
  return value
}

function parseCategory(value: unknown): Category {
  const record = asRecord(value)
  return { id: asNumber(record.id), slug: asString(record.slug), name: asString(record.name) }
}

export async function fetchCategories(): Promise<Category[]> {
  return asList(await apiRequest('GET', '/api/categories'), parseCategory)
}

function parseAdminCourse(value: unknown): AdminCourse {
  const record = asRecord(value)
  return {
    id: asNumber(record.id),
    slug: asString(record.slug),
    title: asString(record.title),
    description: asNullableString(record.description),
    sortOrder: asNumber(record.sortOrder),
    status: asCourseStatus(record.status),
    tuitionVnd: asNumber(record.tuitionVnd),
    categoryId: asNumber(record.categoryId),
    level: asCourseLevel(record.level),
    totalHours: asNumber(record.totalHours),
    coverImageUrl: record.coverImageUrl === undefined ? null : asNullableString(record.coverImageUrl),
  }
}

function asLessonStatus(value: unknown): LessonStatus {
  if (value !== 'DRAFT' && value !== 'PUBLISHED') throw new Error(BAD_SHAPE)
  return value
}

function parseLessonSummary(value: unknown): AdminLessonSummary {
  const record = asRecord(value)
  return {
    id: asNumber(record.id),
    courseId: asNumber(record.courseId),
    slug: asString(record.slug),
    title: asString(record.title),
    summary: asNullableString(record.summary),
    status: asLessonStatus(record.status),
    sortOrder: asNumber(record.sortOrder),
  }
}

function parseAdminLesson(value: unknown): AdminLesson {
  return { ...parseLessonSummary(value), contentMd: asString(asRecord(value).contentMd) }
}

function parseLessonImage(value: unknown): LessonImage {
  const record = asRecord(value)
  return {
    id: asNumber(record.id),
    fileName: asString(record.fileName),
    contentType: asString(record.contentType),
    sizeBytes: asNumber(record.sizeBytes),
    url: asString(record.url),
    cover: record.cover === true,
  }
}

export async function fetchAdminCourses(): Promise<AdminCourse[]> {
  return asList(await apiRequest('GET', '/api/admin/courses'), parseAdminCourse)
}

export async function createCourse(input: CourseInput): Promise<AdminCourse> {
  return parseAdminCourse(await apiRequest('POST', '/api/admin/courses', input))
}

export async function updateCourse(id: number, input: CourseInput): Promise<AdminCourse> {
  return parseAdminCourse(await apiRequest('PUT', `/api/admin/courses/${id}`, input))
}

export async function deleteCourse(id: number): Promise<void> {
  await apiRequest('DELETE', `/api/admin/courses/${id}`)
}

export async function fetchAdminLessons(filter: LessonFilter): Promise<AdminLessonPage> {
  const params = new URLSearchParams()
  if (filter.courseId !== null) params.set('courseId', String(filter.courseId))
  if (filter.status !== null) params.set('status', filter.status)
  params.set('page', String(filter.page))
  params.set('size', String(LESSON_PAGE_SIZE))
  const record = asRecord(await apiRequest('GET', `/api/admin/lessons?${params}`))
  return {
    items: asList(record.items, parseLessonSummary),
    page: asNumber(record.page),
    size: asNumber(record.size),
    totalItems: asNumber(record.totalItems),
  }
}

export async function fetchAdminLesson(id: number): Promise<AdminLesson> {
  return parseAdminLesson(await apiRequest('GET', `/api/admin/lessons/${id}`))
}

export async function createLesson(input: LessonInput): Promise<AdminLesson> {
  return parseAdminLesson(await apiRequest('POST', '/api/admin/lessons', input))
}

export async function updateLesson(id: number, input: LessonInput): Promise<AdminLesson> {
  return parseAdminLesson(await apiRequest('PUT', `/api/admin/lessons/${id}`, input))
}

// Trả bài sau khi đổi để giao diện hiện đúng trạng thái máy chủ đã ghi.
export async function setLessonStatus(id: number, status: LessonStatus): Promise<AdminLesson> {
  return parseAdminLesson(await apiRequest('PATCH', `/api/admin/lessons/${id}/status`, { status }))
}

export async function deleteLesson(id: number): Promise<void> {
  await apiRequest('DELETE', `/api/admin/lessons/${id}`)
}

export async function fetchLessonImages(lessonId: number): Promise<LessonImage[]> {
  return asList(await apiRequest('GET', `/api/admin/lessons/${lessonId}/images`), parseLessonImage)
}

// Phần multipart phải tên "file" (docs/API.md). Máy chủ kiểm lại loại tệp và cỡ, nên kiểm ở trình duyệt chỉ để báo sớm.
export async function uploadLessonImage(lessonId: number, file: File): Promise<UploadedImage> {
  const form = new FormData()
  form.append('file', file)
  const record = asRecord(await apiRequest('POST', `/api/admin/lessons/${lessonId}/images`, form))
  return { id: asNumber(record.id), url: asString(record.url) }
}

// Ảnh bìa khóa học: mỗi khóa một ảnh, tải ảnh mới là thay ảnh cũ. Phần multipart tên "file" như ảnh bài học; trả về
// đường dẫn ảnh mới (có ?v= đổi theo lần tải để trình duyệt không giữ ảnh cũ).
export async function uploadCourseCover(courseId: number, file: File): Promise<{ coverImageUrl: string }> {
  const form = new FormData()
  form.append('file', file)
  const record = asRecord(await apiRequest('POST', `/api/admin/courses/${courseId}/cover`, form))
  return { coverImageUrl: asString(record.coverImageUrl) }
}

export async function deleteCourseCover(courseId: number): Promise<void> {
  await apiRequest('DELETE', `/api/admin/courses/${courseId}/cover`)
}

// Đặt ảnh làm ảnh bìa của bài (máy chủ tự bỏ ảnh bìa cũ của bài đó) hoặc bỏ ảnh bìa.
export async function setImageCover(id: number): Promise<void> {
  await apiRequest('PUT', `/api/admin/images/${id}/cover`)
}

export async function clearImageCover(id: number): Promise<void> {
  await apiRequest('DELETE', `/api/admin/images/${id}/cover`)
}

export async function deleteImage(id: number): Promise<void> {
  await apiRequest('DELETE', `/api/admin/images/${id}`)
}
