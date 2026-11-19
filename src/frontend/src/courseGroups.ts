import type { Course } from './learningApi'

export type Level = 'THCS' | 'THPT'

// Khóa đặt slug dạng tin-hoc-<lớp> (tin-hoc-6 ... tin-hoc-12). Slug khác dạng đó thì không biết lớp.
export function gradeOf(slug: string): number | null {
  const match = /^tin-hoc-(\d{1,2})$/.exec(slug)
  return match ? Number(match[1]) : null
}

export function levelOf(grade: number | null): Level | null {
  if (grade === null) return null
  if (grade >= 6 && grade <= 9) return 'THCS'
  if (grade >= 10 && grade <= 12) return 'THPT'
  return null
}

export type CourseGroup = {
  key: 'thcs' | 'thpt' | 'khac'
  title: string
  courses: Course[]
}

// Giữ thứ tự máy chủ trả về (sortOrder) trong từng nhóm; nhóm rỗng bị bỏ.
export function groupCourses(courses: Course[]): CourseGroup[] {
  const groups: CourseGroup[] = [
    { key: 'thcs', title: 'Trung học cơ sở (lớp 6–9)', courses: [] },
    { key: 'thpt', title: 'Trung học phổ thông (lớp 10–12)', courses: [] },
    { key: 'khac', title: 'Khác', courses: [] },
  ]
  for (const course of courses) {
    const level = levelOf(gradeOf(course.slug))
    groups[level === 'THCS' ? 0 : level === 'THPT' ? 1 : 2].courses.push(course)
  }
  return groups.filter((group) => group.courses.length > 0)
}
