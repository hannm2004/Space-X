import { useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router'
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

// Nút kính lúp dẫn tới trang tìm kiếm. Cùng kiểu viền với nút đổi giao diện để hai nút đứng cạnh nhau cân đối.
// Hình chỉ để trang trí (aria-hidden); tên cho trình đọc màn hình nằm ở aria-label, chữ khi rê chuột ở title.
function SearchLink() {
  return (
    <Link
      to="/tim-kiem"
      aria-label="Tìm bài học"
      title="Tìm bài học"
      className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700"
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        className="h-5 w-5"
      >
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-3.5-3.5" />
      </svg>
    </Link>
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
        <div className="flex items-center gap-4">
          <AccountNav />
          <SearchLink />
          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
        <Outlet />
      </main>
    </div>
  )
}
