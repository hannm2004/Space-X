import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './client'
import { fetchCourses, fetchLesson, fetchLessons, searchLessons } from './learningApi'

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
