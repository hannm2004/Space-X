export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export type FieldError = { field: string; message: string }

const XSRF_COOKIE = 'XSRF-TOKEN'
const XSRF_HEADER = 'X-XSRF-TOKEN'

// Lỗi từ máy chủ (định dạng problem+json, xem docs/API.md) hoặc không kết nối được (status 0).
// `message` luôn là câu an toàn để hiện cho người dùng, không bao giờ là nội dung thô của máy chủ.
export class ApiError extends Error {
  readonly status: number
  readonly title: string | undefined
  readonly detail: string | undefined
  readonly fieldErrors: FieldError[]

  constructor(
    status: number,
    message: string,
    extra: { title?: string; detail?: string; fieldErrors?: FieldError[] } = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.title = extra.title
    this.detail = extra.detail
    this.fieldErrors = extra.fieldErrors ?? []
  }
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : null
}

function readFieldErrors(problem: Record<string, unknown>): FieldError[] {
  if (!Array.isArray(problem.errors)) return []
  const result: FieldError[] = []
  for (const item of problem.errors) {
    const entry = asRecord(item)
    if (entry && typeof entry.field === 'string' && typeof entry.message === 'string') {
      result.push({ field: entry.field, message: entry.message })
    }
  }
  return result
}

async function toApiError(response: Response): Promise<ApiError> {
  const fallback = `Máy chủ trả mã lỗi ${response.status}`
  let problem: Record<string, unknown> | null = null
  try {
    problem = asRecord(await response.json())
  } catch {
    // Thân lỗi không phải JSON (ví dụ trang lỗi của proxy): chỉ dùng mã trạng thái.
  }
  if (!problem) return new ApiError(response.status, fallback)

  const title = typeof problem.title === 'string' ? problem.title : undefined
  const detail = typeof problem.detail === 'string' ? problem.detail : undefined
  return new ApiError(response.status, detail ?? fallback, {
    title,
    detail,
    fieldErrors: readFieldErrors(problem),
  })
}

function readCookie(name: string): string | null {
  for (const part of document.cookie.split('; ')) {
    const equals = part.indexOf('=')
    if (equals !== -1 && part.slice(0, equals) === name) {
      const value = part.slice(equals + 1)
      try {
        return decodeURIComponent(value)
      } catch {
        return value
      }
    }
  }
  return null
}

async function send(
  method: HttpMethod,
  path: string,
  headers: Record<string, string>,
  body?: string,
): Promise<unknown> {
  let response: Response
  try {
    response = await fetch(path, { method, headers, body })
  } catch {
    throw new ApiError(0, 'Không kết nối được tới máy chủ')
  }
  if (!response.ok) throw await toApiError(response)

  const text = await response.text()
  if (text === '') return undefined
  try {
    return JSON.parse(text)
  } catch {
    throw new ApiError(response.status, 'Phản hồi từ máy chủ không đúng dạng')
  }
}

// Máy chủ cấp cookie XSRF-TOKEN cho mọi yêu cầu GET (docs/API.md). Chưa có cookie thì gọi một GET nhẹ để lấy.
async function xsrfToken(): Promise<string | null> {
  const existing = readCookie(XSRF_COOKIE)
  if (existing) return existing
  await send('GET', '/api/health', { Accept: 'application/json' })
  return readCookie(XSRF_COOKIE)
}

// Gọi API của Space-X. Yêu cầu ghi (POST/PUT/PATCH/DELETE) tự gắn header X-XSRF-TOKEN.
// Trả về nội dung JSON đã đọc (kiểu unknown, nơi gọi phải kiểm tra dạng dữ liệu), hoặc undefined nếu không có nội dung.
export async function apiRequest(
  method: HttpMethod,
  path: string,
  data?: unknown,
): Promise<unknown> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  let body: string | undefined
  if (data !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(data)
  }
  if (method !== 'GET') {
    const token = await xsrfToken()
    if (token) headers[XSRF_HEADER] = token
  }
  return send(method, path, headers, body)
}
