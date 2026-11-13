import { useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router'
import HeaderSearch from './HeaderSearch'
import ThemeToggle from './ThemeToggle'
import { useAuth } from './useAuth'

const LINK =
  'rounded text-blue-700 underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:text-blue-400'

// Khu vực tài khoản ở đầu trang. Chưa biết trạng thái (đang hỏi /api/me) thì không hiện gì để tránh nháy
// giữa "Đăng nhập" và tên người dùng.
function AccountNav() {
  const { state, signOut } = useAuth()
  const navigate = useNavigate()
  const [logoutFailed, setLogoutFailed] = useState(false)

  if (state.status === 'loading') return null

  if (state.status === 'anonymous') {
    return (
      <nav aria-label="Tài khoản" className="flex items-center gap-4 text-sm">
        <Link to="/dang-nhap" className={LINK}>
          Đăng nhập
        </Link>
        <Link to="/dang-ky" className={LINK}>
          Đăng ký
        </Link>
      </nav>
    )
  }

  async function handleLogout() {
    setLogoutFailed(false)
    try {
      // Về trang chủ (công khai) trước khi đổi trạng thái; nếu để sau, trang đang mở (ví dụ đổi mật khẩu) thấy
      // "chưa đăng nhập" và kịp tự chuyển sang trang đăng nhập.
      navigate('/')
      await signOut()
    } catch {
      // Đăng xuất lỗi thì session vẫn còn ở máy chủ: giữ trạng thái đăng nhập để người dùng thử lại.
      setLogoutFailed(true)
    }
  }

  return (
    <nav aria-label="Tài khoản" className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
      <span className="max-w-40 truncate font-medium">{state.user.displayName}</span>
      {state.user.role === 'ADMIN' && (
        <Link to="/quan-tri/nguoi-dung" className={LINK}>
          Quản lý người dùng
        </Link>
      )}
      <Link to="/doi-mat-khau" className={LINK}>
        Đổi mật khẩu
      </Link>
      <button type="button" onClick={handleLogout} className={LINK}>
        Đăng xuất
      </button>
      {logoutFailed && (
        <p role="alert" className="w-full text-red-700 dark:text-red-400">
          Không đăng xuất được. Hãy thử lại.
        </p>
      )}
    </nav>
  )
}

// Khung chung của mọi trang: thanh đầu trang và vùng nội dung của trang đang mở.
export default function Layout() {
  return (
    <div className="min-h-screen bg-white text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <header className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-6 sm:px-6">
        <Link
          to="/"
          className="rounded text-xl font-bold tracking-tight focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
        >
          Space-X
        </Link>
        <div className="flex flex-wrap items-center justify-end gap-4">
          <AccountNav />
          <HeaderSearch />
          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
        <Outlet />
      </main>
    </div>
  )
}
