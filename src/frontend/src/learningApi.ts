import { apiRequest } from './client'

export type Course = { id: number; slug: string; title: string; description: string | null }
export type LessonSummary = { id: number; slug: string; title: string; summary: string | null }
export type Lesson = LessonSummary & { contentMd: string }
// Trang kết quả tìm kiếm: cùng hình dạng phân trang chung của API (docs/API.md); trang đếm từ 0.
export type LessonSearchPage = { items: LessonSummary[]; page: number; size: number; totalItems: number }

// Khớp giá trị mặc định của API; để rõ ràng ở đây thay vì dựa vào mặc định phía máy chủ.
const SEARCH_PAGE_SIZE = 20

const BAD_SHAPE = 'Phản hồi từ máy chủ không đúng dạng'

// Dữ liệu từ máy chủ có kiểu unknown; mỗi hàm dưới đây kiểm đúng dạng rồi mới trả về, sai dạng thì ném lỗi.
function asRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) throw new Error(BAD_SHAPE)
  return value as Record<string, unknown>
}

function asString(value: unknown): string {
  if (typeof value !== 'string') throw new Error(BAD_SHAPE)
  return value
}

function asNullableString(value: unknown): string | null {
  return value === null ? null : asString(value)
}

function asNumber(value: unknown): number {
  if (typeof value !== 'number') throw new Error(BAD_SHAPE)
  return value
}

function asList<T>(value: unknown, parse: (item: unknown) => T): T[] {
  if (!Array.isArray(value)) throw new Error(BAD_SHAPE)
  return value.map(parse)
}

function parseCourse(value: unknown): Course {
  const record = asRecord(value)
  return {
    id: asNumber(record.id),
    slug: asString(record.slug),
    title: asString(record.title),
    description: asNullableString(record.description),
  }
}

function parseLessonSummary(value: unknown): LessonSummary {
  const record = asRecord(value)
  return {
    id: asNumber(record.id),
    slug: asString(record.slug),
    title: asString(record.title),
    summary: asNullableString(record.summary),
  }
}

function parseLesson(value: unknown): Lesson {
  const record = asRecord(value)
  return { ...parseLessonSummary(record), contentMd: asString(record.contentMd) }
}

function parseSearchPage(value: unknown): LessonSearchPage {
  const record = asRecord(value)
  return {
    items: asList(record.items, parseLessonSummary),
    page: asNumber(record.page),
    size: asNumber(record.size),
    totalItems: asNumber(record.totalItems),
  }
}

export async function fetchCourses(): Promise<Course[]> {
  return asList(await apiRequest('GET', '/api/courses'), parseCourse)
}

export async function fetchLessons(courseSlug: string): Promise<LessonSummary[]> {
  const path = `/api/courses/${encodeURIComponent(courseSlug)}/lessons`
  return asList(await apiRequest('GET', path), parseLessonSummary)
}

export async function fetchLesson(lessonSlug: string): Promise<Lesson> {
  return parseLesson(await apiRequest('GET', `/api/lessons/${encodeURIComponent(lessonSlug)}`))
}

// Từ khóa đi qua URLSearchParams nên ký tự như & = # không chen thêm tham số.
export async function searchLessons(query: string, page: number): Promise<LessonSearchPage> {
  const params = new URLSearchParams({ q: query, page: String(page), size: String(SEARCH_PAGE_SIZE) })
  return parseSearchPage(await apiRequest('GET', `/api/lessons?${params}`))
}
