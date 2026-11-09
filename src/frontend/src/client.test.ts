import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './client'

const fetchMock = vi.fn()

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function problemResponse(body: unknown, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function setXsrfCookie(value: string) {
  document.cookie = `XSRF-TOKEN=${value}; path=/`
}

function clearCookie(name: string) {
  document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`
}

// Lấy lần gọi fetch thứ `index`: đường dẫn, phương thức, header và thân đã gửi.
function sentRequest(index = 0) {
  const [url, init] = fetchMock.mock.calls[index] as [string, RequestInit]
  return {
    url,
    method: init.method,
    headers: (init.headers ?? {}) as Record<string, string>,
    body: init.body,
  }
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
  clearCookie('XSRF-TOKEN')
  clearCookie('SESSION')
})

describe('apiRequest — GET', () => {
  it('gọi đúng đường dẫn, trả về nội dung JSON và không gửi header CSRF', async () => {
    setXsrfCookie('abc')
    fetchMock.mockResolvedValue(jsonResponse([{ slug: 'tin-hoc-10' }]))

    await expect(apiRequest('GET', '/api/courses')).resolves.toEqual([{ slug: 'tin-hoc-10' }])

    const sent = sentRequest()
    expect(sent.url).toBe('/api/courses')
    expect(sent.method).toBe('GET')
    expect(sent.headers['Accept']).toBe('application/json')
    expect(sent.headers['X-XSRF-TOKEN']).toBeUndefined()
    expect(sent.body).toBeUndefined()
  })
})

async function expectWriteSendsToken(method: 'POST' | 'PUT' | 'PATCH' | 'DELETE') {
  setXsrfCookie('token-123')
  fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

  await apiRequest(method, '/api/me')

  expect(fetchMock).toHaveBeenCalledTimes(1)
  const sent = sentRequest()
  expect(sent.method).toBe(method)
  expect(sent.headers['X-XSRF-TOKEN']).toBe('token-123')
}

describe('apiRequest — CSRF cho yêu cầu ghi', () => {
  it('POST gửi header X-XSRF-TOKEN lấy từ cookie XSRF-TOKEN', () => expectWriteSendsToken('POST'))
  it('PUT gửi header X-XSRF-TOKEN lấy từ cookie XSRF-TOKEN', () => expectWriteSendsToken('PUT'))
  it('PATCH gửi header X-XSRF-TOKEN lấy từ cookie XSRF-TOKEN', () => expectWriteSendsToken('PATCH'))
  it('DELETE gửi header X-XSRF-TOKEN lấy từ cookie XSRF-TOKEN', () => expectWriteSendsToken('DELETE'))

  it('gửi thân JSON kèm Content-Type khi có dữ liệu', async () => {
    setXsrfCookie('t')
    fetchMock.mockResolvedValue(jsonResponse({}, 201))

    await apiRequest('POST', '/api/auth/login', { email: 'a@example.com', password: 'matkhau123' })

    const sent = sentRequest()
    expect(sent.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(sent.body as string)).toEqual({
      email: 'a@example.com',
      password: 'matkhau123',
    })
  })

  it('không gửi Content-Type và thân khi không có dữ liệu', async () => {
    setXsrfCookie('t')
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await apiRequest('POST', '/api/auth/logout')

    const sent = sentRequest()
    expect(sent.headers['Content-Type']).toBeUndefined()
    expect(sent.body).toBeUndefined()
  })

  it('giải mã giá trị cookie đã mã hóa phần trăm', async () => {
    setXsrfCookie('a%2Bb%3D')
    fetchMock.mockResolvedValue(jsonResponse({}))

    await apiRequest('PUT', '/api/me', { displayName: 'An' })

    expect(sentRequest().headers['X-XSRF-TOKEN']).toBe('a+b=')
  })

  it('chỉ lấy cookie đúng tên XSRF-TOKEN, không nhầm cookie có tên chứa nó', async () => {
    document.cookie = 'SESSION=phien-dang-nhap; path=/'
    document.cookie = 'OLD-XSRF-TOKEN=sai; path=/'
    setXsrfCookie('dung')
    fetchMock.mockResolvedValue(jsonResponse({}))

    try {
      await apiRequest('DELETE', '/api/me/progress/courses/1')
      expect(sentRequest().headers['X-XSRF-TOKEN']).toBe('dung')
    } finally {
      clearCookie('OLD-XSRF-TOKEN')
    }
  })

  it('chưa có cookie thì gọi một GET để server cấp cookie rồi mới gửi yêu cầu ghi', async () => {
    fetchMock
      .mockImplementationOnce(() => {
        setXsrfCookie('moi-cap')
        return Promise.resolve(jsonResponse({ status: 'UP' }))
      })
      .mockResolvedValueOnce(jsonResponse({}))

    await apiRequest('POST', '/api/auth/login', { email: 'a@example.com', password: 'x' })

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(sentRequest(0).method).toBe('GET')
    expect(sentRequest(0).url).toBe('/api/health')
    expect(sentRequest(1).method).toBe('POST')
    expect(sentRequest(1).url).toBe('/api/auth/login')
    expect(sentRequest(1).headers['X-XSRF-TOKEN']).toBe('moi-cap')
  })

  it('server vẫn không cấp cookie thì gửi yêu cầu không kèm header (để server trả 403)', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ status: 'UP' }))
      .mockResolvedValueOnce(problemResponse({ status: 403, detail: 'Bạn không có quyền' }, 403))

    await expect(apiRequest('POST', '/api/auth/logout')).rejects.toMatchObject({ status: 403 })

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(sentRequest(1).headers['X-XSRF-TOKEN']).toBeUndefined()
  })
})

describe('apiRequest — kết quả thành công', () => {
  it('204 không có nội dung trả về undefined', async () => {
    setXsrfCookie('t')
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(apiRequest('DELETE', '/api/admin/images/1')).resolves.toBeUndefined()
  })

  it('200 nhưng nội dung không phải JSON thì ném ApiError', async () => {
    fetchMock.mockResolvedValue(new Response('<html>', { status: 200 }))

    await expect(apiRequest('GET', '/api/courses')).rejects.toBeInstanceOf(ApiError)
  })
})

describe('apiRequest — lỗi', () => {
  it('đọc problem+json thành ApiError có status, title và detail', async () => {
    fetchMock.mockResolvedValue(
      problemResponse(
        { type: 'about:blank', title: 'Not Found', status: 404, detail: 'Không tìm thấy bài học' },
        404,
      ),
    )

    const error = await apiRequest('GET', '/api/lessons/khong-co').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 404,
      title: 'Not Found',
      detail: 'Không tìm thấy bài học',
      message: 'Không tìm thấy bài học',
    })
  })

  it('đọc danh sách lỗi theo trường của mã 400', async () => {
    setXsrfCookie('t')
    fetchMock.mockResolvedValue(
      problemResponse(
        {
          status: 400,
          detail: 'Dữ liệu không hợp lệ',
          errors: [
            { field: 'email', message: 'Email không đúng định dạng' },
            { field: 'password', message: 'Mật khẩu tối thiểu 8 ký tự' },
          ],
        },
        400,
      ),
    )

    const error = (await apiRequest('POST', '/api/auth/register', {}).catch(
      (e: unknown) => e,
    )) as ApiError

    expect(error.status).toBe(400)
    expect(error.fieldErrors).toEqual([
      { field: 'email', message: 'Email không đúng định dạng' },
      { field: 'password', message: 'Mật khẩu tối thiểu 8 ký tự' },
    ])
  })

  it('bỏ qua phần tử errors sai dạng thay vì làm hỏng ApiError', async () => {
    fetchMock.mockResolvedValue(
      problemResponse({ status: 400, errors: [{ field: 'email' }, 'rác', null] }, 400),
    )

    const error = (await apiRequest('GET', '/api/x').catch((e: unknown) => e)) as ApiError

    expect(error.fieldErrors).toEqual([])
  })

  it('thân lỗi không phải JSON thì vẫn có status và thông báo chung, không lộ nội dung thô', async () => {
    fetchMock.mockResolvedValue(new Response('<html>Bad Gateway nginx/1.2</html>', { status: 502 }))

    const error = (await apiRequest('GET', '/api/courses').catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(502)
    expect(error.message).toContain('502')
    expect(error.message).not.toContain('nginx')
  })

  it('không kết nối được tới server thì ném ApiError status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = (await apiRequest('GET', '/api/courses').catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(0)
    expect(error.message).toBe('Không kết nối được tới máy chủ')
  })
})
