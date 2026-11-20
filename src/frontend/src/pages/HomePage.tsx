import { useEffect, useId, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router'
import CourseCover from '../CourseCover'
import { gradeOf, groupCourses, levelOf, type CourseGroup } from '../courseGroups'
import LoadError from '../LoadError'
import { fetchCourses, type Course, type CourseProgress } from '../learningApi'
import SearchForm from '../SearchForm'
import ProgressBar from '../ProgressBar'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'
import { useMyProgress } from '../useMyProgress'
import { FEEDBACK_URL } from '../links'

// Thẻ một khóa: ảnh bìa, nhãn cấp học và lớp, tên, mô tả; tiến độ nếu đã học, không thì lời mời vào học.
function CourseCard({ course, mine }: { course: Course; mine: CourseProgress | undefined }) {
  const grade = gradeOf(course.slug)
  const level = levelOf(grade)
  return (
    <li>
      <Link
        to={`/khoa-hoc/${encodeURIComponent(course.slug)}`}
        className="group flex h-full flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-card transition hover:-translate-y-0.5 hover:border-accent hover:shadow-lift"
      >
        {/* Ảnh bìa công khai (khách cũng thấy). Khóa chưa có ảnh thì CourseCover vẽ hình mặc định theo lớp. */}
        <CourseCover slug={course.slug} coverImageUrl={course.coverImageUrl} />
        <span className="flex flex-1 flex-col p-5">
          {(level || grade !== null) && (
            <span className="mb-3 flex items-center gap-2">
              {level && (
                <span className="rounded-full bg-accent-soft px-2.5 py-0.5 text-xs font-semibold text-accent">{level}</span>
              )}
              {grade !== null && <span className="text-xs font-medium text-muted">Lớp {grade}</span>}
            </span>
          )}
          <h4 className="text-lg font-bold tracking-tight group-hover:text-accent">{course.title}</h4>
          {course.description && <span className="mt-2 block text-sm text-muted">{course.description}</span>}
          <span className="mt-auto block pt-5">
            {mine && mine.totalLessons > 0 ? (
              <>
                <ProgressBar percent={mine.percent} label={`Tiến độ ${course.title}`} />
                <span className="mt-1.5 block text-sm text-muted">
                  {mine.completedLessons}/{mine.totalLessons} bài hoàn thành
                </span>
              </>
            ) : (
              <span aria-hidden="true" className="text-sm font-semibold text-accent">
                Vào học →
              </span>
            )}
          </span>
        </span>
      </Link>
    </li>
  )
}

function CourseGroupSection({ group, progress }: { group: CourseGroup; progress: CourseProgress[] }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className="mt-10 first:mt-6">
      <h3 id={headingId} className="flex items-center gap-3 text-lg font-semibold tracking-tight">
        {group.title}
        <span className="rounded-full bg-sunken px-2.5 py-0.5 text-xs font-medium text-muted">{group.courses.length} khóa</span>
      </h3>
      <ul className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {group.courses.map((course) => (
          <CourseCard key={course.id} course={course} mine={progress.find((p) => p.courseSlug === course.slug)} />
        ))}
      </ul>
    </section>
  )
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

// Lưới ô chọn lớp ở bên phải phần đầu trang: bấm một ô là vào thẳng khóa của lớp đó.
function GradePicker({ grades }: { grades: { course: Course; grade: number }[] }) {
  const groups = [
    { label: 'Trung học cơ sở', items: grades.filter((g) => levelOf(g.grade) === 'THCS') },
    { label: 'Trung học phổ thông', items: grades.filter((g) => levelOf(g.grade) === 'THPT') },
  ].filter((group) => group.items.length > 0)
  return (
    <nav aria-label="Chọn lớp" className="rounded-2xl border border-line bg-surface p-5 shadow-card sm:p-6">
      <p className="text-sm font-semibold">Bạn đang học lớp mấy?</p>
      <div className="mt-4 space-y-5">
        {groups.map((group) => (
          <div key={group.label}>
            <p className="text-xs font-medium text-muted">{group.label}</p>
            <div className="mt-2 grid grid-cols-4 gap-2">
              {group.items.map(({ course, grade }) => (
                <Link
                  key={course.id}
                  to={`/khoa-hoc/${encodeURIComponent(course.slug)}`}
                  aria-label={`Lớp ${grade}`}
                  className="flex flex-col items-center rounded-lg border border-line bg-paper px-2 py-3 transition hover:border-accent hover:bg-accent-soft hover:text-accent"
                >
                  <span aria-hidden="true" className="text-2xl font-bold leading-none">
                    {grade}
                  </span>
                  <span aria-hidden="true" className="mt-1 text-xs text-muted">
                    Lớp
                  </span>
                </Link>
              ))}
            </div>
          </div>
        ))}
      </div>
    </nav>
  )
}

function Hero({ courses }: { courses: Course[] | null }) {
  const grades = (courses ?? [])
    .map((course) => ({ course, grade: gradeOf(course.slug) }))
    .filter((item): item is { course: Course; grade: number } => item.grade !== null)
  return (
    <section className="grid items-center gap-10 lg:grid-cols-[1.15fr_1fr]">
      <div>
        <p className="inline-flex rounded-full bg-accent-soft px-3 py-1 text-sm font-semibold text-accent">
          Chương trình GDPT 2018 · Miễn phí
        </p>
        <h1 className="mt-4 text-4xl font-extrabold leading-tight tracking-tight sm:text-5xl">
          Học Tin học từ lớp 6 đến lớp 12
        </h1>
        <p className="mt-4 max-w-xl text-lg text-muted">
          Bài học Tin học THCS và THPT bằng tiếng Việt. Học theo từng khóa, tìm bài nhanh và lưu lại tiến độ của bạn.
        </p>
        <section aria-labelledby="search-title" className="mt-7 max-w-xl">
          <h2 id="search-title" className="sr-only">
            Tìm bài học
          </h2>
          <SearchForm />
        </section>
        {courses && (
          <dl className="mt-8 grid max-w-xl grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-4">
            {[
              [`${courses.length} khóa học`, 'từ lớp 6 đến lớp 12'],
              ['Miễn phí', 'không có thanh toán'],
              ['Theo chương trình', 'GDPT 2018'],
              ['Lưu tiến độ', 'khi đã đăng nhập'],
            ].map(([value, label]) => (
              // Đọc theo thứ tự thuật ngữ rồi định nghĩa (dt trước dd); flex-col-reverse đảo lại để số nổi bật ở trên.
              <div key={value} className="flex flex-col-reverse border-l-2 border-accent pl-3">
                <dt className="text-xs text-muted">{label}</dt>
                <dd className="font-bold">{value}</dd>
              </div>
            ))}
          </dl>
        )}
      </div>
      {grades.length > 0 && <GradePicker grades={grades} />}
    </section>
  )
}

function Courses({ courses, onReload, state }: { courses: Course[] | null; onReload: () => void; state: 'loading' | 'error' | 'ok' }) {
  const progress = useMyProgress()
  // Tiến độ chỉ là phần phụ: chưa đăng nhập, đang tải hoặc lỗi thì thẻ khóa hiện như cũ, không báo lỗi.
  const mine = progress.state.kind === 'ok' ? (progress.state.data ?? []) : []

  return (
    <section id="khoa-hoc" aria-labelledby="courses-title" className="mt-16 scroll-mt-20 sm:mt-20">
      <SectionHeading id="courses-title" title="Khóa học" subtitle="Mỗi lớp là một khóa. Chọn khóa của lớp bạn và học theo thứ tự các bài." />
      {state === 'loading' && <p className="mt-4 text-muted">Đang tải danh sách khóa học...</p>}
      {state === 'error' && <LoadError message="Không tải được danh sách khóa học." onRetry={onReload} />}
      {state === 'ok' && courses !== null && courses.length === 0 && <p className="mt-4 text-muted">Chưa có khóa học nào.</p>}
      {state === 'ok' &&
        courses !== null &&
        groupCourses(courses).map((group) => <CourseGroupSection key={group.key} group={group} progress={mine} />)}
    </section>
  )
}

// Biểu tượng nét mảnh 24x24 cho bốn ô tính năng; chỉ trang trí nên ẩn với trình đọc màn hình.
const FEATURES: { title: string; text: string; icon: ReactNode }[] = [
  {
    title: 'Bài viết ngắn gọn, bám chương trình',
    text: 'Mỗi bài theo khung Chương trình GDPT 2018 môn Tin học, có ví dụ, bảng và mã mẫu.',
    icon: <path d="M6 3h8l5 5v13H6z M14 3v5h5 M9 13h6 M9 17h6" />,
  },
  {
    title: 'Kiểm tra nhanh cuối bài',
    text: 'Vài câu hỏi để tự xem mình đã hiểu bài chưa, trước khi sang bài mới.',
    icon: <path d="M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M8 12.5l3 3 5-6" />,
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
      <SectionHeading id="features-title" title="Mỗi bài học có gì" subtitle="Những thứ giúp bạn học đều tay, không bị rối." />
      <ul className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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

const LINK_EXTERNAL = { target: '_blank', rel: 'noopener noreferrer' } as const

function Faq() {
  const items: [string, ReactNode][] = [
    ['Học trên Space-X có mất phí không?', 'Hiện tại không. Xem bài, tìm kiếm và lưu tiến độ đều miễn phí; hệ thống không có thanh toán.'],
    [
      'Có cần tạo tài khoản không?',
      'Xem danh sách khóa học thì không cần. Đọc bài, tìm kiếm và lưu tiến độ cần đăng nhập bằng email hoặc tài khoản Google.',
    ],
    [
      'Nội dung theo chương trình nào?',
      <>
        Bám khung Chương trình giáo dục phổ thông 2018 môn Tin học, soạn lại bằng lời mới. Nội dung chỉ để tự học và tham
        khảo, không thay thế sách giáo khoa hay bài giảng của thầy cô. Thấy nội dung sai, hãy{' '}
        <a href={FEEDBACK_URL} className="link" {...LINK_EXTERNAL}>
          gửi góp ý
        </a>
        .
      </>,
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
          Bắt đầu học ngay hôm nay
        </h2>
        <p className="mt-1">Tạo tài khoản trong một phút để đọc bài và lưu tiến độ.</p>
      </div>
      <div className="mt-6 flex flex-wrap items-center gap-4 sm:mt-0">
        <Link to="/dang-ky" className="btn btn-light">
          Đăng ký miễn phí
        </Link>
        <Link to="/dang-nhap" className="text-sm font-semibold underline underline-offset-4">
          Đã có tài khoản? Đăng nhập
        </Link>
      </div>
    </section>
  )
}

export default function HomePage() {
  const { state, reload } = useLoad(fetchCourses, 'courses')
  const { hash } = useLocation()

  // Liên kết kiểu /#hoi-dap từ trang khác: trình duyệt không tự cuộn khi chuyển trang trong ứng dụng.
  useEffect(() => {
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView?.()
  }, [hash])

  const courses = state.kind === 'ok' ? state.data : null
  return (
    <>
      <Hero courses={courses} />
      <Courses courses={courses} state={state.kind === 'loading' ? 'loading' : state.kind === 'error' ? 'error' : 'ok'} onReload={reload} />
      <Features />
      <Faq />
      <SignUpBand />
    </>
  )
}
