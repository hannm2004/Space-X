import { fetchProgress, type CourseProgress } from './learningApi'
import { useAuth } from './useAuth'
import { useLoad } from './useLoad'

// Tiến độ của người đang đăng nhập. Khách chưa đăng nhập thì không gọi API (kết quả là null): tiến độ chỉ có nghĩa với
// tài khoản, và máy chủ cũng chỉ trả cho chính người đó. Đăng nhập xong thì key đổi nên tự tải lại.
export function useMyProgress() {
  const { state } = useAuth()
  const signedIn = state.status === 'authenticated'
  return useLoad<CourseProgress[] | null>(
    () => (signedIn ? fetchProgress() : Promise.resolve(null)),
    signedIn ? 'progress' : 'anonymous',
  )
}
