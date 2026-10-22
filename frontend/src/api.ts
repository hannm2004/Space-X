export async function fetchHealth(): Promise<string> {
  const response = await fetch('/api/health')
  if (!response.ok) {
    throw new Error(`Máy chủ trả mã lỗi ${response.status}`)
  }

  const data: unknown = await response.json()
  if (
    typeof data !== 'object' ||
    data === null ||
    !('status' in data) ||
    typeof data.status !== 'string'
  ) {
    throw new Error('Phản hồi từ máy chủ không đúng dạng')
  }
  return data.status
}
