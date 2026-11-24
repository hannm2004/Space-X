import { useState, type ReactNode } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { SECONDARY } from './buttons'
import HealthStatus from './HealthStatus'
import HeaderSearch from './HeaderSearch'
import ThemeToggle from './ThemeToggle'
import { FEEDBACK_URL } from './links'
import { useAuth } from './useAuth'

// Mục điều hướng ở đầu trang: nền nhạt màu nhấn khi đang ở khu vực đó (aria-current báo cho trình đọc màn hình).
const NAV_LINK =
  'rounded-md px-3 py-2 text-[0.9375rem] font-medium text-muted hover:bg-sunken hover:text-ink aria-[current=page]:bg-accent-soft aria-[current=page]:text-accent'

// Logo SPACE-X có hai tệp: bản vẽ bằng xanh đậm cho nền sáng, bản vẽ bằng trắng cho nền tối. Hai thẻ <img> cùng có
// mặt, class `dark` trên <html> (do ThemeToggle và script ở index.html đặt) chọn thẻ nào hiện, nên không cần JavaScript
// và không nháy sai logo khi tải trang. Dùng `dark:` ở đây vì token màu của index.css không đổi được tệp ảnh.
// alt rỗng: tên thương hiệu đã có bằng chữ ngay bên cạnh, để trình đọc màn hình không đọc hai lần.
// width/height theo tỉ lệ 200:140 của tệp SVG để trình duyệt giữ chỗ.
function BrandLogo() {
  return (
    <>
      <img src="/logo-mark.svg" alt="" width={46} height={32} className="h-8 w-auto dark:hidden" />
      <img src="/logo-mark-on-dark.svg" alt="" width={46} height={32} className="hidden h-8 w-auto dark:block" />
    </>
  )
}

function PrimaryNav({ isAdmin }: { isAdmin: boolean }) {
  const { pathname } = useLocation()
  // Trang khóa và trang bài đều thuộc mục "Khóa học".
  const inCourses = pathname === '/' || pathname.startsWith('/khoa-hoc') || pathname.startsWith('/bai/')
  const inAdmin = pathname.startsWith('/quan-tri')

  return (
    <nav aria-label="Chính" className="flex items-center gap-1">
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
      <nav aria-label="Tài khoản" className="flex items-center gap-2">
        <Link to="/dang-nhap" className={NAV_LINK}>
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

  const initial = state.user.displayName.trim().charAt(0).toUpperCase() || '?'
  return (
    <nav aria-label="Tài khoản" className="flex flex-wrap items-center gap-x-3 gap-y-1">
      <Link to="/ho-so" className="flex items-center gap-2 rounded-md py-1 pr-2 hover:bg-sunken">
        <span aria-hidden="true" className="grid size-8 place-items-center rounded-full bg-accent-soft text-sm font-bold text-accent">
          {initial}
        </span>
        <span className="max-w-32 truncate text-[0.9375rem] font-medium">{state.user.displayName}</span>
      </Link>
      <button type="button" onClick={handleLogout} className={SECONDARY}>
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
      <h2 className="text-sm font-semibold text-ink">{title}</h2>
      <ul className="mt-3 space-y-2">{children}</ul>
    </div>
  )
}

const FOOTER_LINK = 'text-muted hover:text-ink hover:underline'

// Khung chung của mọi trang: thanh đầu trang, vùng nội dung của trang đang mở và chân trang.
export default function Layout() {
  const { state } = useAuth()
  const isAdmin = state.status === 'authenticated' && state.user.role === 'ADMIN'

  return (
    <div className="flex min-h-screen flex-col">
      {/* Mục đầu tiên khi bấm Tab: nhảy thẳng tới nội dung, khỏi đi qua cả thanh đầu trang ở mỗi trang. Chỉ hiện khi có focus. */}
      <a
        href="#noi-dung"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-surface focus:px-4 focus:py-2 focus:font-semibold focus:shadow-lift"
      >
        Bỏ qua tới nội dung
      </a>
      <header className="sticky top-0 z-30 border-b border-line bg-surface/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3 sm:px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <BrandLogo />
            <span className="flex flex-col leading-none">
              {/* translate="no": tên riêng, trình duyệt không được dịch. Dòng phụ là tiếng Việt thường nên vẫn cho dịch. */}
              <span translate="no" className="text-xl font-extrabold tracking-tight">
                SPACE-X
              </span>
              <span className="mt-1 text-[0.6875rem] font-semibold uppercase tracking-[0.14em] text-muted">Trung tâm Tin học</span>
            </span>
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

      <main id="noi-dung" tabIndex={-1} className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 sm:py-10">
        <Outlet />
      </main>

      <footer className="border-t border-line bg-surface">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 text-sm sm:px-6 md:grid-cols-[1.5fr_1fr_1fr_1fr_1fr]">
          <div>
            <p className="flex items-center gap-2.5 text-lg font-extrabold tracking-tight">
              <BrandLogo />
              <span translate="no">SPACE-X</span>
            </p>
            <p className="mt-3 max-w-xs text-muted">
              Học liệu Tin học lớp 6 đến lớp 12 theo Chương trình GDPT 2018. Dự án học tập, miễn phí.
            </p>
          </div>
          <FooterColumn title="Trung học cơ sở">
            {[6, 7, 8, 9].map((grade) => (
              <li key={grade}>
                <Link to={`/khoa-hoc/tin-hoc-${grade}`} className={FOOTER_LINK}>
                  Lớp {grade}
                </Link>
              </li>
            ))}
          </FooterColumn>
          <FooterColumn title="Trung học phổ thông">
            {[10, 11, 12].map((grade) => (
              <li key={grade}>
                <Link to={`/khoa-hoc/tin-hoc-${grade}`} className={FOOTER_LINK}>
                  Lớp {grade}
                </Link>
              </li>
            ))}
          </FooterColumn>
          <FooterColumn title="Hỗ trợ">
            <li>
              <Link to="/gioi-thieu" className={FOOTER_LINK}>
                Giới thiệu
              </Link>
            </li>
            <li>
              <Link to="/#hoi-dap" className={FOOTER_LINK}>
                Câu hỏi thường gặp
              </Link>
            </li>
            <li>
              <a href={FEEDBACK_URL} target="_blank" rel="noopener noreferrer" className={FOOTER_LINK}>
                Góp ý, báo lỗi
              </a>
            </li>
          </FooterColumn>
          <FooterColumn title="Pháp lý">
            <li>
              <Link to="/chinh-sach-bao-mat" className={FOOTER_LINK}>
                Chính sách bảo mật
              </Link>
            </li>
            <li>
              <Link to="/dieu-khoan-su-dung" className={FOOTER_LINK}>
                Điều khoản sử dụng
              </Link>
            </li>
            <li>
              <Link to="/ban-quyen" className={FOOTER_LINK}>
                Bản quyền
              </Link>
            </li>
          </FooterColumn>
        </div>
        <div className="border-t border-line">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-4 text-sm text-muted sm:px-6">
            <p>© 2026 Space-X</p>
            <HealthStatus />
          </div>
        </div>
      </footer>
    </div>
  )
}
