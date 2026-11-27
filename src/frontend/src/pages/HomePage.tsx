import { useEffect, useState, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router'
import { PRIMARY, SECONDARY } from '../buttons'
import { fetchCatalog, type CatalogCourse, type Category } from '../catalogApi'
import CourseCard from '../CourseCard'
import LoadError from '../LoadError'
import SearchForm from '../SearchForm'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'
import { useMyProgress } from '../useMyProgress'

type Status = 'loading' | 'error' | 'ok'

// Các nhóm đang có khóa, theo thứ tự xuất hiện trong danh sách (nhóm rỗng không cần nút).
function categoriesOf(courses: CatalogCourse[]): Category[] {
  return [...new Map(courses.map((course) => [course.category.id, course.category])).values()]
}

// Tiêu đề một mục lớn của trang chủ: tiêu đề và một dòng mô tả ngắn bên dưới.
function SectionHeading({ id, title, subtitle }: { id: string; title: string; subtitle?: string }) {
  return (
    <div>
      <h2 id={id} className="text-2xl font-bold tracking-tight sm:text-3xl">
        {title}
      </h2>
      {subtitle && <p className="mt-2 max-w-2xl text-muted">{subtitle}</p>}
    </div>
  )
}

function Hero({ courses }: { courses: CatalogCourse[] | null }) {
  return (
    <section className="max-w-3xl">
      <p className="inline-flex rounded-full bg-accent-soft px-3 py-1 text-sm font-semibold text-accent">
        Trung tâm Tin học SPACE-X
      </p>
      <h1 className="mt-4 text-4xl font-extrabold leading-tight tracking-tight sm:text-5xl">
        Khóa học Tin học tại SPACE-X
      </h1>
      <p className="mt-4 max-w-xl text-lg text-muted">
        Xem chương trình, học phí và thời lượng từng khóa. Học viên đọc học liệu trực tuyến và theo dõi tiến độ của
        mình.
      </p>
      <section aria-labelledby="search-title" className="mt-7 max-w-xl">
        <h2 id="search-title" className="sr-only">
          Tìm bài học
        </h2>
        <SearchForm />
      </section>
      {courses && (
        <dl className="mt-8 grid max-w-xl grid-cols-3 gap-x-6 gap-y-4">
          {[
            [`${courses.length} khóa học`, 'đang mở'],
            [`${categoriesOf(courses).length} nhóm chương trình`, 'chọn theo nhu cầu'],
            [`${courses.reduce((sum, course) => sum + course.lessonCount, 0)} bài học`, 'học liệu đã đăng'],
          ].map(([value, label]) => (
            // Đọc theo thứ tự thuật ngữ rồi định nghĩa (dt trước dd); flex-col-reverse đảo lại để số nổi bật ở trên.
            <div key={label} className="flex flex-col-reverse border-l-2 border-accent pl-3">
              <dt className="text-xs text-muted">{label}</dt>
              <dd className="font-bold">{value}</dd>
            </div>
          ))}
        </dl>
      )}
    </section>
  )
}

function Courses({ courses, status, onReload }: { courses: CatalogCourse[] | null; status: Status; onReload: () => void }) {
  const progress = useMyProgress()
  // Tiến độ chỉ là phần phụ: chưa đăng nhập, đang tải hoặc lỗi thì thẻ khóa hiện như cũ, không báo lỗi.
  const mine = progress.state.kind === 'ok' ? (progress.state.data ?? []) : []
  // null là "Tất cả".
  const [selected, setSelected] = useState<Category | null>(null)
  const shown = (courses ?? []).filter((course) => selected === null || course.category.id === selected.id)
  const listPath = selected ? `/danh-sach-khoa-hoc?nhom=${encodeURIComponent(selected.slug)}` : '/danh-sach-khoa-hoc'

  return (
    <section id="khoa-hoc" aria-labelledby="courses-title" className="mt-16 scroll-mt-20 sm:mt-20">
      <SectionHeading id="courses-title" title="Khóa học" subtitle="Chọn nhóm chương trình để xem các khóa đang mở." />
      {status === 'loading' && <p className="mt-4 text-muted">Đang tải danh sách khóa học...</p>}
      {status === 'error' && <LoadError message="Không tải được danh sách khóa học." onRetry={onReload} />}
      {status === 'ok' && courses?.length === 0 && <p className="mt-4 text-muted">Chưa có khóa học nào.</p>}
      {status === 'ok' && courses !== null && courses.length > 0 && (
        <>
          <div role="group" aria-label="Chọn nhóm chương trình" className="mt-6 flex flex-wrap gap-2">
            {[null, ...categoriesOf(courses)].map((category) => {
              const pressed = (selected?.id ?? null) === (category?.id ?? null)
              return (
                // Nút đang chọn dùng kiểu nút chính, còn lại kiểu nút phụ; aria-pressed báo cho trình đọc màn hình.
                <button
                  key={category?.id ?? 0}
                  type="button"
                  aria-pressed={pressed}
                  onClick={() => setSelected(category)}
                  className={pressed ? `${PRIMARY} btn-sm` : SECONDARY}
                >
                  {category?.name ?? 'Tất cả'}
                </button>
              )
            })}
          </div>
          <ul className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {shown.map((course) => (
              <CourseCard
                key={course.id}
                course={course}
                heading="h3"
                mine={mine.find((p) => p.courseSlug === course.slug)}
              />
            ))}
          </ul>
          <p className="mt-6">
            <Link to={listPath} className="link font-semibold">
              Lọc và so sánh khóa học
            </Link>
          </p>
        </>
      )}
    </section>
  )
}

// Biểu tượng nét mảnh 24x24 cho ba ô; chỉ trang trí nên ẩn với trình đọc màn hình.
const FEATURES: { title: string; text: string; icon: ReactNode }[] = [
  {
    title: 'Bài viết ngắn gọn',
    text: 'Mỗi bài có ví dụ, bảng và mã mẫu, đọc được trên điện thoại.',
    icon: <path d="M6 3h8l5 5v13H6z M14 3v5h5 M9 13h6 M9 17h6" />,
  },
  {
    title: 'Lưu tiến độ học',
    text: 'Đánh dấu bài đã hoàn thành, xem phần trăm của từng khóa và bấm Học tiếp để vào đúng bài đang dở.',
    icon: <path d="M4 20h16 M7 20v-7 M12 20V5 M17 20v-10" />,
  },
  {
    title: 'Tìm kiếm toàn bộ bài',
    text: 'Gõ từ khóa ở ô tìm kiếm, gợi ý hiện ngay trong lúc bạn gõ.',
    icon: <path d="M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14z M16 16l4 4" />,
  },
]

function Features() {
  return (
    <section aria-labelledby="features-title" className="mt-16 sm:mt-20">
      <SectionHeading id="features-title" title="Học liệu trực tuyến" subtitle="Ôn lại bài bất cứ lúc nào." />
      <ul className="mt-6 grid gap-4 sm:grid-cols-3">
        {FEATURES.map(({ title, text, icon }) => (
          <li key={title} className="rounded-xl border border-line bg-surface p-5 shadow-card">
            <span aria-hidden="true" className="grid size-11 place-items-center rounded-lg bg-accent-soft text-accent">
              <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                {icon}
              </svg>
            </span>
            <h3 className="mt-4 font-semibold leading-snug">{title}</h3>
            <p className="mt-2 text-sm text-muted">{text}</p>
          </li>
        ))}
      </ul>
    </section>
  )
}

function Faq() {
  const items: [string, ReactNode][] = [
    [
      'Trung tâm SPACE-X có thật không?',
      'Không. SPACE-X là trung tâm hư cấu của một đồ án sinh viên, đây là môi trường thử nghiệm.',
    ],
    [
      'Đăng ký lớp và đóng học phí thế nào?',
      'Chức năng đăng ký lớp và thanh toán đang được xây dựng. Khi hoàn thành cũng chỉ chạy thử nghiệm, không thu tiền thật.',
    ],
    [
      'Có cần tạo tài khoản không?',
      'Xem danh sách khóa học thì không cần. Đọc bài, tìm kiếm và lưu tiến độ cần đăng nhập bằng email hoặc tài khoản Google.',
    ],
    ['Dùng trên điện thoại được không?', 'Được. Giao diện co giãn theo màn hình và có chế độ sáng, tối ở góc trên bên phải.'],
    [
      'Quên mật khẩu thì làm thế nào?',
      'Hiện chưa có email đặt lại mật khẩu. Hãy gửi góp ý để quản trị viên cấp mật khẩu tạm cho bạn. Nếu đăng nhập bằng Google thì bạn không cần mật khẩu.',
    ],
    [
      'Dữ liệu cá nhân của tôi được dùng thế nào?',
      <>
        Chỉ để đăng nhập, hiển thị tên và lưu tiến độ học; không có quảng cáo. Chi tiết ở{' '}
        <Link to="/chinh-sach-bao-mat" className="link">
          Chính sách bảo mật
        </Link>
        .
      </>,
    ],
  ]
  return (
    <section id="hoi-dap" aria-labelledby="faq-title" className="mt-16 scroll-mt-20 sm:mt-20">
      <SectionHeading id="faq-title" title="Câu hỏi thường gặp" />
      <div className="mt-6 divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface shadow-card">
        {items.map(([question, answer]) => (
          <details key={question} className="group px-5 py-4">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-4 font-semibold">
              {question}
              <svg
                aria-hidden="true"
                viewBox="0 0 24 24"
                className="size-5 shrink-0 text-muted transition group-open:rotate-180"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M6 9l6 6 6-6" />
              </svg>
            </summary>
            <p className="mt-3 max-w-3xl text-muted">{answer}</p>
          </details>
        ))}
      </div>
    </section>
  )
}

function SignUpBand() {
  const { state } = useAuth()
  if (state.status !== 'anonymous') return null
  return (
    <section
      aria-labelledby="cta-title"
      className="mt-16 rounded-2xl bg-accent px-6 py-10 text-on-accent sm:mt-20 sm:flex sm:items-center sm:justify-between sm:gap-8 sm:px-10"
    >
      <div>
        <h2 id="cta-title" className="text-2xl font-bold tracking-tight">
          Tạo tài khoản học viên
        </h2>
        <p className="mt-1">Một phút là xong, dùng để đọc bài và lưu tiến độ.</p>
      </div>
      <div className="mt-6 flex flex-wrap items-center gap-4 sm:mt-0">
        <Link to="/dang-ky" className="btn btn-light">
          Tạo tài khoản
        </Link>
        <Link to="/dang-nhap" className="text-sm font-semibold underline underline-offset-4">
          Đã có tài khoản? Đăng nhập
        </Link>
      </div>
    </section>
  )
}

export default function HomePage() {
  const { state, reload } = useLoad(() => fetchCatalog(), 'catalog')
  const { hash } = useLocation()

  // Liên kết kiểu /#hoi-dap từ trang khác: trình duyệt không tự cuộn khi chuyển trang trong ứng dụng.
  useEffect(() => {
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView?.()
  }, [hash])

  const courses = state.kind === 'ok' ? state.data : null
  return (
    <>
      <Hero courses={courses} />
      <Courses courses={courses} status={state.kind} onReload={reload} />
      <Features />
      <Faq />
      <SignUpBand />
    </>
  )
}
