import { useState, type ReactNode } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import HealthStatus from './HealthStatus'
import HeaderSearch from './HeaderSearch'
import ThemeToggle from './ThemeToggle'
import { FEEDBACK_URL } from './links'
import { useAuth } from './useAuth'

// Mục điều hướng ở đầu trang: gạch chân màu nhấn khi đang ở khu vực đó (aria-current báo cho trình đọc màn hình).
const NAV_LINK =
  'border-b-2 border-transparent py-1 text-[0.9375rem] font-medium hover:border-line-strong aria-[current=page]:border-accent'

function PrimaryNav({ isAdmin }: { isAdmin: boolean }) {
  const { pathname } = useLocation()
  // Trang khóa và trang bài đều thuộc mục "Khóa học".
  const inCourses = pathname === '/' || pathname.startsWith('/khoa-hoc') || pathname.startsWith('/bai/')
  const inAdmin = pathname.startsWith('/quan-tri')

  return (
    <nav aria-label="Chính" className="flex items-center gap-5">
      <Link to="/" className={NAV_LINK} aria-current={inCourses ? 'page' : undefined}>
        Khóa học
      </Link>
      <Link to="/#hoi-dap" className={NAV_LINK}>
        Hỏi đáp
      </Link>
      {isAdmin && (
        <Link to="/quan-tri" className={NAV_LINK} aria-current={inAdmin ? 'page' : undefined}>
          Quản trị
        </Link>
      )}
    </nav>
  )
}

// Khu vực tài khoản ở góc phải. Chưa biết trạng thái (đang hỏi /api/me) thì không hiện gì để tránh nháy
// giữa "Đăng nhập" và tên người dùng.
function AccountNav() {
  const { state, signOut } = useAuth()
  const navigate = useNavigate()
  const [logoutFailed, setLogoutFailed] = useState(false)

  if (state.status === 'loading') return null

  if (state.status === 'anonymous') {
    return (
      <nav aria-label="Tài khoản" className="flex items-center gap-4">
        <Link to="/dang-nhap" className="text-[0.9375rem] font-medium hover:underline hover:underline-offset-4">
          Đăng nhập
        </Link>
        <Link to="/dang-ky" className="btn btn-primary btn-sm">
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
    <nav aria-label="Tài khoản" className="flex flex-wrap items-center gap-x-3 gap-y-1">
      <Link
        to="/ho-so"
        className="max-w-36 truncate text-[0.9375rem] font-medium hover:underline hover:underline-offset-4"
      >
        {state.user.displayName}
      </Link>
      <button type="button" onClick={handleLogout} className="btn btn-secondary btn-sm">
        Đăng xuất
      </button>
      {logoutFailed && (
        <p role="alert" className="w-full text-sm text-danger">
          Không đăng xuất được. Hãy thử lại.
        </p>
      )}
    </nav>
  )
}

function FooterColumn({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <h2 className="eyebrow">{title}</h2>
      <ul className="mt-3 space-y-2">{children}</ul>
    </div>
  )
}

// Khung chung của mọi trang: thanh đầu trang, vùng nội dung của trang đang mở và chân trang.
export default function Layout() {
  const { state } = useAuth()
  const isAdmin = state.status === 'authenticated' && state.user.role === 'ADMIN'

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-line">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-8 gap-y-3 px-4 py-4 sm:px-6">
          <Link to="/" className="font-serif text-2xl font-bold tracking-tight">
            Space-<span className="text-accent">X</span>
          </Link>
          <PrimaryNav isAdmin={isAdmin} />
          <div className="ml-auto flex flex-wrap items-center justify-end gap-x-4 gap-y-2">
            <div className="flex items-center gap-2">
              <HeaderSearch />
              <ThemeToggle />
            </div>
            <AccountNav />
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-10 sm:px-6">
        <Outlet />
      </main>

      <footer className="border-t border-line bg-sunken">
        <div className="mx-auto grid max-w-5xl gap-8 px-4 py-10 text-sm sm:px-6 md:grid-cols-[1.4fr_1fr_1fr_1fr_1fr]">
          <div>
            <p className="font-serif text-xl font-bold tracking-tight">
              Space-<span className="text-accent">X</span>
            </p>
            <p className="mt-2 max-w-xs text-muted">
              Học liệu Tin học lớp 6 đến lớp 12 theo Chương trình GDPT 2018. Dự án học tập, miễn phí.
            </p>
          </div>
          <FooterColumn title="Trung học cơ sở">
            {[6, 7, 8, 9].map((grade) => (
              <li key={grade}>
                <Link to={`/khoa-hoc/tin-hoc-${grade}`} className="hover:underline">
                  Lớp {grade}
                </Link>
              </li>
            ))}
          </FooterColumn>
          <FooterColumn title="Trung học phổ thông">
            {[10, 11, 12].map((grade) => (
              <li key={grade}>
                <Link to={`/khoa-hoc/tin-hoc-${grade}`} className="hover:underline">
                  Lớp {grade}
                </Link>
              </li>
            ))}
          </FooterColumn>
          <FooterColumn title="Hỗ trợ">
            <li>
              <Link to="/#hoi-dap" className="hover:underline">
                Câu hỏi thường gặp
              </Link>
            </li>
            <li>
              <a href={FEEDBACK_URL} target="_blank" rel="noopener noreferrer" className="hover:underline">
                Góp ý, báo lỗi
              </a>
            </li>
          </FooterColumn>
          <FooterColumn title="Pháp lý">
            <li>
              <Link to="/chinh-sach-bao-mat" className="hover:underline">
                Chính sách bảo mật
              </Link>
            </li>
            <li>
              <Link to="/dieu-khoan-su-dung" className="hover:underline">
                Điều khoản sử dụng
              </Link>
            </li>
            <li>
              <Link to="/ban-quyen" className="hover:underline">
                Bản quyền
              </Link>
            </li>
          </FooterColumn>
        </div>
        <div className="border-t border-line">
          <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-4 text-sm text-muted sm:px-6">
            <p>© 2026 Space-X</p>
            <HealthStatus />
          </div>
        </div>
      </footer>
    </div>
  )
}
