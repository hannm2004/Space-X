import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './client'
import {
  fetchCourses,
  fetchLesson,
  fetchLessons,
  fetchProgress,
  recordProgress,
  restartCourse,
  searchLessons,
} from './learningApi'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./client')>()
  return { ...actual, apiRequest: vi.fn() }
})

const apiRequestMock = vi.mocked(apiRequest)

beforeEach(() => {
  apiRequestMock.mockReset()
})

describe('fetchCourses', () => {
  it('gọi GET /api/courses và trả về danh sách khóa', async () => {
    apiRequestMock.mockResolvedValue([
      { id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: 'Khóa lớp 10' },
      { id: 2, slug: 'tin-hoc-11', title: 'Tin học 11', description: null },
    ])

    const courses = await fetchCourses()

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/courses')
    expect(courses).toEqual([
      { id: 1, slug: 'tin-hoc-10', title: 'Tin học 10', description: 'Khóa lớp 10' },
      { id: 2, slug: 'tin-hoc-11', title: 'Tin học 11', description: null },
    ])
  })

  it('chưa có khóa nào thì trả về mảng rỗng', async () => {
    apiRequestMock.mockResolvedValue([])

    await expect(fetchCourses()).resolves.toEqual([])
  })

  it('ném lỗi khi phản hồi không phải mảng', async () => {
    apiRequestMock.mockResolvedValue({ courses: [] })

    await expect(fetchCourses()).rejects.toThrow('không đúng dạng')
  })

  it('ném lỗi khi một khóa thiếu trường bắt buộc hoặc sai kiểu', async () => {
    apiRequestMock.mockResolvedValue([{ id: 1, slug: 'tin-hoc-10' }])
    await expect(fetchCourses()).rejects.toThrow('không đúng dạng')

    apiRequestMock.mockResolvedValue([{ id: '1', slug: 'a', title: 'A', description: null }])
    await expect(fetchCourses()).rejects.toThrow('không đúng dạng')
  })

  it('giữ nguyên ApiError của máy chủ để giao diện biết mã lỗi', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(502, 'Máy chủ trả mã lỗi 502'))

    await expect(fetchCourses()).rejects.toMatchObject({ status: 502 })
  })
})

describe('fetchLessons', () => {
  it('gọi đúng đường dẫn của khóa và trả về danh sách bài', async () => {
    apiRequestMock.mockResolvedValue([
      { id: 1, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: 'Tóm tắt' },
      { id: 2, slug: 'mang-may-tinh', title: 'Mạng máy tính', summary: null },
    ])

    const lessons = await fetchLessons('tin-hoc-10')

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/courses/tin-hoc-10/lessons')
    expect(lessons).toHaveLength(2)
    expect(lessons[1]).toEqual({ id: 2, slug: 'mang-may-tinh', title: 'Mạng máy tính', summary: null })
  })

  it('mã hóa slug trong đường dẫn để ký tự lạ không đổi đích của yêu cầu', async () => {
    apiRequestMock.mockResolvedValue([])

    await fetchLessons('a b/../admin')

    expect(apiRequestMock).toHaveBeenCalledWith(
      'GET',
      '/api/courses/a%20b%2F..%2Fadmin/lessons',
    )
  })

  it('ném lỗi khi một bài sai dạng', async () => {
    apiRequestMock.mockResolvedValue([{ id: 1, slug: 'a', summary: 'thiếu title' }])

    await expect(fetchLessons('tin-hoc-10')).rejects.toThrow('không đúng dạng')
  })
})

describe('fetchLesson', () => {
  it('gọi GET /api/lessons/{slug} và trả về nội dung Markdown', async () => {
    apiRequestMock.mockResolvedValue({
      id: 5,
      slug: 'thong-tin-va-du-lieu',
      title: 'Thông tin và dữ liệu',
      summary: 'Tóm tắt',
      contentMd: '# Thông tin và dữ liệu\n\nNội dung',
    })

    const lesson = await fetchLesson('thong-tin-va-du-lieu')

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/lessons/thong-tin-va-du-lieu')
    expect(lesson.contentMd).toBe('# Thông tin và dữ liệu\n\nNội dung')
    expect(lesson.summary).toBe('Tóm tắt')
  })

  it('mã hóa slug trong đường dẫn', async () => {
    apiRequestMock.mockResolvedValue({
      id: 1,
      slug: 'x',
      title: 'X',
      summary: null,
      contentMd: '',
    })

    await fetchLesson('../me')

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/lessons/..%2Fme')
  })

  it('ném lỗi khi thiếu contentMd', async () => {
    apiRequestMock.mockResolvedValue({ id: 1, slug: 'x', title: 'X', summary: null })

    await expect(fetchLesson('x')).rejects.toThrow('không đúng dạng')
  })

  it('chưa đăng nhập: giữ nguyên ApiError 401 để giao diện mời đăng nhập', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(401, 'Bạn cần đăng nhập'))

    await expect(fetchLesson('x')).rejects.toMatchObject({ status: 401 })
  })
})

describe('searchLessons', () => {
  const PAGE = {
    items: [{ id: 11, slug: 'thong-tin-va-du-lieu', title: 'Thông tin và dữ liệu', summary: null }],
    page: 0,
    size: 20,
    totalItems: 1,
  }

  it('gọi GET /api/lessons với từ khóa, trang và kích thước trang, rồi trả về trang kết quả', async () => {
    apiRequestMock.mockResolvedValue(PAGE)

    const result = await searchLessons('thông tin', 0)

    expect(apiRequestMock).toHaveBeenCalledWith(
      'GET',
      '/api/lessons?q=th%C3%B4ng+tin&page=0&size=20',
    )
    expect(result).toEqual(PAGE)
  })

  it('mã hóa ký tự đặc biệt của từ khóa, không để chen thêm tham số', async () => {
    apiRequestMock.mockResolvedValue(PAGE)

    await searchLessons('a&page=9#x', 2)

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/lessons?q=a%26page%3D9%23x&page=2&size=20')
  })

  it('nhận kích thước trang tùy chọn (gợi ý ở thanh đầu trang chỉ lấy vài bài)', async () => {
    apiRequestMock.mockResolvedValue({ ...PAGE, size: 5 })

    await searchLessons('mạng', 0, 5)

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/lessons?q=m%E1%BA%A1ng&page=0&size=5')
  })

  it('ném lỗi khi "items" không phải mảng', async () => {
    apiRequestMock.mockResolvedValue({ ...PAGE, items: 'x' })

    await expect(searchLessons('mạng', 0)).rejects.toThrow('không đúng dạng')
  })

  it('ném lỗi khi thiếu tổng số bài hoặc tổng số sai kiểu', async () => {
    apiRequestMock.mockResolvedValue({ ...PAGE, totalItems: '1' })

    await expect(searchLessons('mạng', 0)).rejects.toThrow('không đúng dạng')
  })

  it('ném lỗi khi một bài trong kết quả thiếu slug', async () => {
    apiRequestMock.mockResolvedValue({ ...PAGE, items: [{ id: 1, title: 'X', summary: null }] })

    await expect(searchLessons('mạng', 0)).rejects.toThrow('không đúng dạng')
  })

  it('chưa đăng nhập: giữ nguyên ApiError 401 để giao diện mời đăng nhập', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(401, 'Bạn cần đăng nhập'))

    await expect(searchLessons('mạng', 0)).rejects.toMatchObject({ status: 401 })
  })
})

const KHOA_10 = {
  courseId: 3,
  courseSlug: 'tin-hoc-10',
  courseTitle: 'Tin học 10',
  completedLessons: 1,
  totalLessons: 3,
  percent: 33,
  continueLesson: { id: 12, slug: 'mang-may-tinh', title: 'Mạng máy tính' },
}

describe('fetchProgress', () => {
  it('gọi GET /api/me/progress và trả về tiến độ từng khóa, kể cả khóa không còn bài để học tiếp', async () => {
    const xongHet = { ...KHOA_10, courseId: 4, completedLessons: 3, percent: 100, continueLesson: null }
    apiRequestMock.mockResolvedValue([KHOA_10, xongHet])

    const progress = await fetchProgress()

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/me/progress')
    expect(progress).toEqual([KHOA_10, xongHet])
  })

  it('ném lỗi khi phản hồi không phải mảng', async () => {
    apiRequestMock.mockResolvedValue({ courses: [] })

    await expect(fetchProgress()).rejects.toThrow('không đúng dạng')
  })

  it('ném lỗi khi một khóa thiếu trường hoặc sai kiểu', async () => {
    apiRequestMock.mockResolvedValue([{ ...KHOA_10, percent: '33' }])
    await expect(fetchProgress()).rejects.toThrow('không đúng dạng')

    const { totalLessons: _bo, ...thieu } = KHOA_10
    apiRequestMock.mockResolvedValue([thieu])
    await expect(fetchProgress()).rejects.toThrow('không đúng dạng')
  })

  it('ném lỗi khi "học tiếp" sai dạng (không phải đối tượng hoặc null)', async () => {
    apiRequestMock.mockResolvedValue([{ ...KHOA_10, continueLesson: 'mang-may-tinh' }])
    await expect(fetchProgress()).rejects.toThrow('không đúng dạng')

    apiRequestMock.mockResolvedValue([{ ...KHOA_10, continueLesson: { id: 12, slug: 'mang-may-tinh' } }])
    await expect(fetchProgress()).rejects.toThrow('không đúng dạng')
  })

  it('giữ nguyên ApiError 401 để giao diện biết chưa đăng nhập', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(401, 'Bạn cần đăng nhập'))

    await expect(fetchProgress()).rejects.toMatchObject({ status: 401 })
  })
})

describe('recordProgress', () => {
  it('gọi PUT đúng đường dẫn của bài kèm trạng thái và trả về trạng thái thật máy chủ đã ghi', async () => {
    apiRequestMock.mockResolvedValue({ lessonId: 11, status: 'COMPLETED' })

    const result = await recordProgress(11, 'COMPLETED')

    expect(apiRequestMock).toHaveBeenCalledWith('PUT', '/api/me/progress/lessons/11', { status: 'COMPLETED' })
    expect(result).toEqual({ lessonId: 11, status: 'COMPLETED' })
  })

  it('mở bài đã hoàn thành: gửi IN_PROGRESS nhưng nhận lại COMPLETED thì trả đúng COMPLETED', async () => {
    apiRequestMock.mockResolvedValue({ lessonId: 11, status: 'COMPLETED' })

    const result = await recordProgress(11, 'IN_PROGRESS')

    expect(apiRequestMock).toHaveBeenCalledWith('PUT', '/api/me/progress/lessons/11', { status: 'IN_PROGRESS' })
    expect(result.status).toBe('COMPLETED')
  })

  it('ném lỗi khi máy chủ trả trạng thái lạ hoặc sai dạng', async () => {
    apiRequestMock.mockResolvedValue({ lessonId: 11, status: 'DONE' })
    await expect(recordProgress(11, 'COMPLETED')).rejects.toThrow('không đúng dạng')

    apiRequestMock.mockResolvedValue({ lessonId: '11', status: 'COMPLETED' })
    await expect(recordProgress(11, 'COMPLETED')).rejects.toThrow('không đúng dạng')

    apiRequestMock.mockResolvedValue(undefined)
    await expect(recordProgress(11, 'COMPLETED')).rejects.toThrow('không đúng dạng')
  })

  it('giữ nguyên ApiError 404 khi bài không tồn tại hoặc chưa đăng', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(404, 'Không tìm thấy bài học'))

    await expect(recordProgress(99, 'COMPLETED')).rejects.toMatchObject({ status: 404 })
  })
})

describe('restartCourse', () => {
  it('gọi DELETE đúng đường dẫn của khóa và không trả dữ liệu', async () => {
    apiRequestMock.mockResolvedValue(undefined)

    await expect(restartCourse(3)).resolves.toBeUndefined()

    expect(apiRequestMock).toHaveBeenCalledWith('DELETE', '/api/me/progress/courses/3')
  })

  it('giữ nguyên ApiError khi khóa không tồn tại', async () => {
    apiRequestMock.mockRejectedValue(new ApiError(404, 'Không tìm thấy khóa học'))

    await expect(restartCourse(99)).rejects.toMatchObject({ status: 404 })
  })
})
