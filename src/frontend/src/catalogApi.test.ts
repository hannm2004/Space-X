import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchCatalog, fetchCategories } from './catalogApi'
import { apiRequest } from './client'

vi.mock('./client', async (original) => ({ ...(await original<typeof import('./client')>()), apiRequest: vi.fn() }))

const apiRequestMock = vi.mocked(apiRequest)

const KHOA = {
  id: 1,
  slug: 'excel-co-ban',
  title: 'Excel cơ bản',
  description: null,
  coverImageUrl: null,
  lessonCount: 2,
  category: { id: 4, slug: 'van-phong', name: 'Tin học văn phòng' },
  level: 'ADVANCED',
  totalHours: 40,
  tuitionVnd: 1500000,
}

beforeEach(() => {
  apiRequestMock.mockReset()
})

describe('fetchCatalog', () => {
  it('không có bộ lọc thì gọi đúng /api/courses và trả đủ các trường', async () => {
    apiRequestMock.mockResolvedValue([KHOA])

    await expect(fetchCatalog()).resolves.toEqual([KHOA])

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/courses')
  })

  it('chỉ gửi những bộ lọc có giá trị', async () => {
    apiRequestMock.mockResolvedValue([])

    await fetchCatalog({
      category: 'van-phong',
      level: 'ADVANCED',
      duration: '31-60',
      price: '3000001-',
      sort: 'price_asc',
    })
    await fetchCatalog({ level: 'BASIC' })

    expect(apiRequestMock).toHaveBeenNthCalledWith(
      1,
      'GET',
      '/api/courses?category=van-phong&level=ADVANCED&duration=31-60&price=3000001-&sort=price_asc',
    )
    expect(apiRequestMock).toHaveBeenNthCalledWith(2, 'GET', '/api/courses?level=BASIC')
  })

  it('ký tự đặc biệt trong tên nhóm không chen thêm được tham số', async () => {
    apiRequestMock.mockResolvedValue([])

    await fetchCatalog({ category: 'a&level=EXPERT' })

    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/courses?category=a%26level%3DEXPERT')
  })

  it('từ chối phản hồi thiếu nhóm, trình độ lạ hoặc học phí không phải số', async () => {
    const thieuNhom: Record<string, unknown> = { ...KHOA }
    delete thieuNhom.category

    for (const bad of [thieuNhom, { ...KHOA, level: 'MASTER' }, { ...KHOA, tuitionVnd: '0' }]) {
      apiRequestMock.mockResolvedValue([bad])

      await expect(fetchCatalog()).rejects.toThrow()
    }
  })
})

describe('fetchCategories', () => {
  it('gọi GET /api/categories và từ chối phản hồi sai dạng', async () => {
    apiRequestMock.mockResolvedValue([KHOA.category])

    await expect(fetchCategories()).resolves.toEqual([KHOA.category])
    expect(apiRequestMock).toHaveBeenCalledWith('GET', '/api/categories')

    apiRequestMock.mockResolvedValue({})
    await expect(fetchCategories()).rejects.toThrow()
  })
})
