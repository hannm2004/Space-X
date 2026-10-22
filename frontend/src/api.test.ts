import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchHealth } from './api'

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubFetch(result: () => Promise<Response>) {
  vi.stubGlobal('fetch', vi.fn(result))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchHealth', () => {
  it('gọi đúng đường dẫn và trả về trạng thái khi server trả UP', async () => {
    stubFetch(() => Promise.resolve(jsonResponse({ status: 'UP' })))

    await expect(fetchHealth()).resolves.toBe('UP')
    expect(fetch).toHaveBeenCalledWith('/api/health')
  })

  it('ném lỗi khi server trả mã lỗi HTTP', async () => {
    stubFetch(() => Promise.resolve(jsonResponse({ status: 'DOWN' }, 503)))

    await expect(fetchHealth()).rejects.toThrow()
  })

  it('ném lỗi khi nội dung trả về sai dạng', async () => {
    stubFetch(() => Promise.resolve(jsonResponse({ foo: 1 })))

    await expect(fetchHealth()).rejects.toThrow()
  })

  it('ném lỗi khi không kết nối được tới server', async () => {
    stubFetch(() => Promise.reject(new TypeError('Failed to fetch')))

    await expect(fetchHealth()).rejects.toThrow()
  })
})
