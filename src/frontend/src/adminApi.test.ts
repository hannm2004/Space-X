import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createCourse,
  createLesson,
  deleteCourse,
  deleteImage,
  setImageCover,
  clearImageCover,
  deleteLesson,
  fetchAdminCourses,
  fetchAdminDashboard,
  fetchAdminLesson,
  fetchAdminLessons,
  fetchAdminUsers,
  fetchLessonImages,
  resetUserPassword,
  setLessonStatus,
  setUserEnabled,
  updateCourse,
  updateLesson,
  deleteCourseCover,
  uploadCourseCover,
  uploadLessonImage,
} from './adminApi'
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

const DASHBOARD_JSON = {
  users: { total: 12, newLast7Days: 3 },
  lessons: { total: 7, draft: 2, published: 5 },
  courses: [{ courseId: 1, slug: 'tin-hoc-10', title: 'Tin học 10', draft: 1, published: 3 }],
  learning: { activeLearners: 4, completedLessons: 9 },
}

describe('fetchAdminDashboard', () => {
  it('gọi GET /api/admin/dashboard và trả về số liệu đúng kiểu', async () => {
    apiRequestMock.mockResolvedValue(DASHBOARD_JSON)

    const result = await fetchAdminDashboard()

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/admin/dashboard')
    expect(result).toEqual(DASHBOARD_JSON)
  })

  it('khóa chưa có bài vẫn được nhận (danh sách khóa có thể rỗng)', async () => {
    apiRequestMock.mockResolvedValue({ ...DASHBOARD_JSON, courses: [] })

    expect((await fetchAdminDashboard()).courses).toEqual([])
  })

  it('báo lỗi dạng khi thiếu khối users', async () => {
    apiRequestMock.mockResolvedValue({ ...DASHBOARD_JSON, users: undefined })

    await expect(fetchAdminDashboard()).rejects.toThrow('không đúng dạng')
  })

  it('báo lỗi dạng khi một số là chuỗi', async () => {
    apiRequestMock.mockResolvedValue({ ...DASHBOARD_JSON, lessons: { total: '7', draft: 2, published: 5 } })

    await expect(fetchAdminDashboard()).rejects.toThrow('không đúng dạng')
  })

  it('báo lỗi dạng khi courses không phải mảng', async () => {
    apiRequestMock.mockResolvedValue({ ...DASHBOARD_JSON, courses: 'x' })

    await expect(fetchAdminDashboard()).rejects.toThrow('không đúng dạng')
  })

  it('báo lỗi dạng khi toàn bộ phản hồi không phải object', async () => {
    apiRequestMock.mockResolvedValue('không phải số liệu')

    await expect(fetchAdminDashboard()).rejects.toThrow('không đúng dạng')
  })

  it('chuyển tiếp lỗi 403 của máy chủ', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(403, 'Bạn không có quyền thực hiện thao tác này'))

    await expect(fetchAdminDashboard()).rejects.toMatchObject({ status: 403 })
  })
})

const COURSE_JSON = {
  id: 5,
  slug: 'tin-hoc-10',
  title: 'Tin học 10',
  description: 'Lớp 10',
  sortOrder: 1,
  status: 'PUBLISHED',
  tuitionVnd: 1500000,
  coverImageUrl: null,
}
const COURSE_INPUT = {
  slug: 'tin-hoc-10',
  title: 'Tin học 10',
  description: 'Lớp 10',
  sortOrder: 1,
  status: 'PUBLISHED' as const,
  tuitionVnd: 1500000,
}
const LESSON_SUMMARY_JSON = {
  id: 3,
  courseId: 5,
  slug: 'may-tinh-la-gi',
  title: 'Máy tính là gì',
  summary: null,
  status: 'DRAFT',
  sortOrder: 1,
}
const LESSON_JSON = { ...LESSON_SUMMARY_JSON, contentMd: '# Máy tính' }
const LESSON_INPUT = {
  courseId: 5,
  slug: 'may-tinh-la-gi',
  title: 'Máy tính là gì',
  summary: '',
  contentMd: '# Máy tính',
  sortOrder: 1,
}
const IMAGE_JSON = {
  id: 7,
  fileName: 'a.png',
  contentType: 'image/png',
  sizeBytes: 123,
  url: '/api/images/7',
  cover: false,
}

describe('khóa học (Admin)', () => {
  it('fetchAdminCourses gọi GET /api/admin/courses và trả danh sách có sortOrder', async () => {
    apiRequestMock.mockResolvedValue([COURSE_JSON, { ...COURSE_JSON, id: 6, description: null }])

    const result = await fetchAdminCourses()

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/admin/courses')
    expect(result).toEqual([COURSE_JSON, { ...COURSE_JSON, id: 6, description: null }])
  })

  it('fetchAdminCourses từ chối phản hồi thiếu sortOrder', async () => {
    apiRequestMock.mockResolvedValue([{ id: 5, slug: 'a', title: 'A', description: null }])

    await expect(fetchAdminCourses()).rejects.toThrow()
  })

  it('fetchAdminCourses từ chối trạng thái lạ, thiếu trạng thái hoặc học phí không phải số', async () => {
    const thieuTrangThai: Record<string, unknown> = { ...COURSE_JSON }
    delete thieuTrangThai.status

    for (const bad of [{ ...COURSE_JSON, status: 'HIDDEN' }, thieuTrangThai, { ...COURSE_JSON, tuitionVnd: '0' }]) {
      apiRequestMock.mockResolvedValue([bad])

      await expect(fetchAdminCourses()).rejects.toThrow()
    }
  })

  it('fetchAdminCourses từ chối phản hồi không phải mảng', async () => {
    apiRequestMock.mockResolvedValue({ items: [] })

    await expect(fetchAdminCourses()).rejects.toThrow()
  })

  it('createCourse gọi POST với đúng nội dung và trả khóa vừa tạo', async () => {
    apiRequestMock.mockResolvedValue(COURSE_JSON)

    await expect(createCourse(COURSE_INPUT)).resolves.toEqual(COURSE_JSON)

    expect(apiRequestMock).toHaveBeenCalledWith('POST', '/api/admin/courses', COURSE_INPUT)
  })

  it('updateCourse gọi PUT theo id và trả khóa sau khi sửa', async () => {
    apiRequestMock.mockResolvedValue(COURSE_JSON)

    await expect(updateCourse(5, COURSE_INPUT)).resolves.toEqual(COURSE_JSON)

    expect(apiRequestMock).toHaveBeenCalledWith('PUT', '/api/admin/courses/5', COURSE_INPUT)
  })

  it('deleteCourse gọi DELETE theo id', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await deleteCourse(5)

    expect(apiRequestMock).toHaveBeenCalledWith('DELETE', '/api/admin/courses/5')
  })

  it('giữ nguyên ApiError 409 (slug trùng, khóa còn bài) để trang hiện đúng câu của máy chủ', async () => {
    const conflict = new ApiError(409, 'Slug khóa học đã tồn tại', { detail: 'Slug khóa học đã tồn tại' })
    apiRequestMock.mockRejectedValue(conflict)

    await expect(createCourse(COURSE_INPUT)).rejects.toBe(conflict)
  })
})

describe('bài học (Admin)', () => {
  it('fetchAdminLessons không lọc: chỉ gửi page và size, trả trang bài không có nội dung', async () => {
    apiRequestMock.mockResolvedValue({ items: [LESSON_SUMMARY_JSON], page: 0, size: 20, totalItems: 1 })

    const result = await fetchAdminLessons({ courseId: null, status: null, page: 0 })

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/admin/lessons?page=0&size=20')
    expect(result).toEqual({ items: [LESSON_SUMMARY_JSON], page: 0, size: 20, totalItems: 1 })
  })

  it('fetchAdminLessons gửi courseId, status và trang khi có lọc', async () => {
    apiRequestMock.mockResolvedValue({ items: [], page: 2, size: 20, totalItems: 0 })

    await fetchAdminLessons({ courseId: 5, status: 'PUBLISHED', page: 2 })

    expect(apiRequestMock).toHaveBeenCalledWith(
      'GET',
      `/api/admin/lessons?${new URLSearchParams({ courseId: '5', status: 'PUBLISHED', page: '2', size: '20' })}`,
    )
  })

  it('từ chối trạng thái bài lạ', async () => {
    apiRequestMock.mockResolvedValue({
      items: [{ ...LESSON_SUMMARY_JSON, status: 'ARCHIVED' }],
      page: 0,
      size: 20,
      totalItems: 1,
    })

    await expect(fetchAdminLessons({ courseId: null, status: null, page: 0 })).rejects.toThrow()
  })

  it('fetchAdminLesson gọi GET theo id và trả bài kèm contentMd', async () => {
    apiRequestMock.mockResolvedValue(LESSON_JSON)

    await expect(fetchAdminLesson(3)).resolves.toEqual(LESSON_JSON)

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/admin/lessons/3')
  })

  it('fetchAdminLesson từ chối bài thiếu contentMd', async () => {
    apiRequestMock.mockResolvedValue(LESSON_SUMMARY_JSON)

    await expect(fetchAdminLesson(3)).rejects.toThrow()
  })

  it('createLesson gọi POST với đúng nội dung', async () => {
    apiRequestMock.mockResolvedValue(LESSON_JSON)

    await expect(createLesson(LESSON_INPUT)).resolves.toEqual(LESSON_JSON)

    expect(apiRequestMock).toHaveBeenCalledWith('POST', '/api/admin/lessons', LESSON_INPUT)
  })

  it('updateLesson gọi PUT theo id với đúng nội dung', async () => {
    apiRequestMock.mockResolvedValue(LESSON_JSON)

    await expect(updateLesson(3, LESSON_INPUT)).resolves.toEqual(LESSON_JSON)

    expect(apiRequestMock).toHaveBeenCalledWith('PUT', '/api/admin/lessons/3', LESSON_INPUT)
  })

  it('setLessonStatus gọi PATCH chỉ với { status } và trả bài sau khi đổi', async () => {
    apiRequestMock.mockResolvedValue({ ...LESSON_JSON, status: 'PUBLISHED' })

    const result = await setLessonStatus(3, 'PUBLISHED')

    expect(apiRequestMock).toHaveBeenCalledWith('PATCH', '/api/admin/lessons/3/status', { status: 'PUBLISHED' })
    expect(result.status).toBe('PUBLISHED')
  })

  it('deleteLesson gọi DELETE theo id', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await deleteLesson(3)

    expect(apiRequestMock).toHaveBeenCalledWith('DELETE', '/api/admin/lessons/3')
  })
})

describe('ảnh bài học (Admin)', () => {
  it('fetchLessonImages gọi GET và trả danh sách ảnh', async () => {
    apiRequestMock.mockResolvedValue([IMAGE_JSON])

    await expect(fetchLessonImages(3)).resolves.toEqual([IMAGE_JSON])

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/admin/lessons/3/images')
  })

  it('uploadLessonImage gửi tệp trong phần tên "file" bằng FormData và trả { id, url }', async () => {
    apiRequestMock.mockResolvedValue({ id: 7, url: '/api/images/7' })
    const file = new File(['x'], 'a.png', { type: 'image/png' })

    await expect(uploadLessonImage(3, file)).resolves.toEqual({ id: 7, url: '/api/images/7' })

    const [method, path, body] = apiRequestMock.mock.calls[0]
    expect(method).toBe('POST')
    expect(path).toBe('/api/admin/lessons/3/images')
    expect(body).toBeInstanceOf(FormData)
    expect((body as FormData).get('file')).toBeInstanceOf(File)
  })

  it('từ chối phản hồi tải ảnh thiếu url', async () => {
    apiRequestMock.mockResolvedValue({ id: 7 })

    await expect(uploadLessonImage(3, new File(['x'], 'a.png', { type: 'image/png' }))).rejects.toThrow()
  })

  it('deleteImage gọi DELETE /api/admin/images/{id}', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await deleteImage(7)

    expect(apiRequestMock).toHaveBeenCalledWith('DELETE', '/api/admin/images/7')
  })
})

describe('ảnh bìa khóa học (Admin)', () => {
  it('fetchAdminCourses giữ đường dẫn ảnh bìa; thiếu trường thì là null', async () => {
    apiRequestMock.mockResolvedValue([
      { ...COURSE_JSON, coverImageUrl: '/api/courses/tin-hoc-10/cover?v=9' },
      { id: 6, slug: 'a', title: 'A', description: null, sortOrder: 2, status: 'DRAFT', tuitionVnd: 0 },
    ])

    const result = await fetchAdminCourses()

    expect(result.map((course) => course.coverImageUrl)).toEqual(['/api/courses/tin-hoc-10/cover?v=9', null])
  })

  it('fetchAdminCourses từ chối coverImageUrl không phải chuỗi', async () => {
    apiRequestMock.mockResolvedValue([{ ...COURSE_JSON, coverImageUrl: 3 }])

    await expect(fetchAdminCourses()).rejects.toThrow()
  })

  it('uploadCourseCover gửi tệp trong phần tên "file" bằng POST và trả { coverImageUrl }', async () => {
    apiRequestMock.mockResolvedValue({ coverImageUrl: '/api/courses/tin-hoc-10/cover?v=1' })
    const file = new File(['x'], 'bia.png', { type: 'image/png' })

    await expect(uploadCourseCover(5, file)).resolves.toEqual({ coverImageUrl: '/api/courses/tin-hoc-10/cover?v=1' })

    const [method, path, body] = apiRequestMock.mock.calls[0]
    expect(method).toBe('POST')
    expect(path).toBe('/api/admin/courses/5/cover')
    expect(body).toBeInstanceOf(FormData)
    expect((body as FormData).get('file')).toBeInstanceOf(File)
  })

  it('uploadCourseCover từ chối phản hồi thiếu coverImageUrl', async () => {
    apiRequestMock.mockResolvedValue({})

    await expect(uploadCourseCover(5, new File(['x'], 'bia.png', { type: 'image/png' }))).rejects.toThrow()
  })

  it('deleteCourseCover gọi DELETE /api/admin/courses/{id}/cover', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await deleteCourseCover(5)

    expect(apiRequestMock).toHaveBeenCalledWith('DELETE', '/api/admin/courses/5/cover')
  })
})

describe('ảnh bìa (Admin)', () => {
  it('fetchLessonImages trả cờ ảnh bìa', async () => {
    apiRequestMock.mockResolvedValue([{ ...IMAGE_JSON, cover: true }, IMAGE_JSON])

    const images = await fetchLessonImages(3)

    expect(images.map((image) => image.cover)).toEqual([true, false])
  })

  it('setImageCover gọi PUT /api/admin/images/{id}/cover', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await setImageCover(7)

    expect(apiRequestMock).toHaveBeenCalledWith('PUT', '/api/admin/images/7/cover')
  })

  it('clearImageCover gọi DELETE /api/admin/images/{id}/cover', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await clearImageCover(7)

    expect(apiRequestMock).toHaveBeenCalledWith('DELETE', '/api/admin/images/7/cover')
  })
})
