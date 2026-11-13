export const BAD_SHAPE = 'Phản hồi từ máy chủ không đúng dạng'

// Dữ liệu từ máy chủ có kiểu unknown; các hàm dưới đây kiểm đúng dạng rồi mới trả về, sai dạng thì ném lỗi.
export function asRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) throw new Error(BAD_SHAPE)
  return value as Record<string, unknown>
}

export function asString(value: unknown): string {
  if (typeof value !== 'string') throw new Error(BAD_SHAPE)
  return value
}

export function asNullableString(value: unknown): string | null {
  return value === null ? null : asString(value)
}

export function asNumber(value: unknown): number {
  if (typeof value !== 'number') throw new Error(BAD_SHAPE)
  return value
}

export function asBoolean(value: unknown): boolean {
  if (typeof value !== 'boolean') throw new Error(BAD_SHAPE)
  return value
}

export function asList<T>(value: unknown, parse: (item: unknown) => T): T[] {
  if (!Array.isArray(value)) throw new Error(BAD_SHAPE)
  return value.map(parse)
}
