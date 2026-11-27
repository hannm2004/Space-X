import { apiRequest } from './client'
import { parseCourse, type Course } from './learningApi'
import { asList, asNumber, asRecord, asString, BAD_SHAPE } from './parse'

// Danh mục khóa công khai: GET /api/courses (có bộ lọc) và GET /api/categories (docs/API.md).

export type CourseLevel = 'BASIC' | 'ADVANCED' | 'EXPERT'
export type Category = { id: number; slug: string; name: string }
export type CatalogCourse = Course & {
  category: Category
  level: CourseLevel
  // 0 là chưa nhập số giờ.
  totalHours: number
  // Đồng Việt Nam; 0 là chưa có học phí.
  tuitionVnd: number
}
export type CourseSort = 'price_asc' | 'price_desc'
// duration (số giờ) và price (đồng) là khoảng "min-max" được bỏ một đầu, ví dụ "31-60" hoặc "61-".
export type CatalogQuery = {
  category?: string
  level?: CourseLevel
  duration?: string
  price?: string
  sort?: CourseSort
}

export function asCourseLevel(value: unknown): CourseLevel {
  if (value !== 'BASIC' && value !== 'ADVANCED' && value !== 'EXPERT') throw new Error(BAD_SHAPE)
  return value
}

function parseCategory(value: unknown): Category {
  const record = asRecord(value)
  return { id: asNumber(record.id), slug: asString(record.slug), name: asString(record.name) }
}

function parseCatalogCourse(value: unknown): CatalogCourse {
  const record = asRecord(value)
  return {
    ...parseCourse(value),
    category: parseCategory(record.category),
    level: asCourseLevel(record.level),
    totalHours: asNumber(record.totalHours),
    tuitionVnd: asNumber(record.tuitionVnd),
  }
}

export async function fetchCatalog(query: CatalogQuery = {}): Promise<CatalogCourse[]> {
  // URLSearchParams mã hóa giá trị nên ký tự như & = trong địa chỉ trang không chen thêm tham số.
  const params = new URLSearchParams()
  for (const key of ['category', 'level', 'duration', 'price', 'sort'] as const) {
    const value = query[key]
    if (value) params.set(key, value)
  }
  const search = params.toString()
  return asList(await apiRequest('GET', search === '' ? '/api/courses' : `/api/courses?${search}`), parseCatalogCourse)
}

export async function fetchCategories(): Promise<Category[]> {
  return asList(await apiRequest('GET', '/api/categories'), parseCategory)
}
