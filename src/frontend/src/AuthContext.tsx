import { useEffect, useState, type ReactNode } from 'react'
import { fetchCurrentUser, login as loginRequest, logout as logoutRequest, type CurrentUser } from './authApi'
import { AuthContext, type AuthState } from './useAuth'

// Giữ "ai đang đăng nhập" cho cả ứng dụng. Nguồn sự thật là session ở máy chủ: mở trang thì hỏi /api/me một lần.
// signIn/signOut ném lại lỗi của máy chủ để trang gọi tự hiện thông báo; trạng thái chỉ đổi khi máy chủ đã đồng ý.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    fetchCurrentUser()
      .then((user) => {
        if (!cancelled) setState(user ? { status: 'authenticated', user } : { status: 'anonymous' })
      })
      .catch(() => {
        // Không hỏi được (mất mạng, 500): coi như chưa đăng nhập; đăng nhập hay gọi API sau đó sẽ tự báo lỗi cụ thể.
        if (!cancelled) setState({ status: 'anonymous' })
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function signIn(email: string, password: string) {
    const user = await loginRequest(email, password)
    setState({ status: 'authenticated', user })
  }

  async function signOut() {
    await logoutRequest()
    setState({ status: 'anonymous' })
  }

  // Chỉ gọi sau khi máy chủ đã lưu thay đổi; nếu đã đăng xuất giữa chừng thì bỏ qua để không "đăng nhập lại" ngầm.
  function updateUser(user: CurrentUser) {
    setState((current) => (current.status === 'authenticated' ? { status: 'authenticated', user } : current))
  }

  return <AuthContext.Provider value={{ state, signIn, signOut, updateUser }}>{children}</AuthContext.Provider>
}
