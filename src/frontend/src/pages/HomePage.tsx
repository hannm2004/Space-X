import { useEffect, useId, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router'
import { gradeOf, groupCourses, levelOf, type CourseGroup } from '../courseGroups'
import LoadError from '../LoadError'
import { fetchCourses, type Course, type CourseProgress } from '../learningApi'
import SearchForm from '../SearchForm'
import ProgressBar from '../ProgressBar'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'
import { useMyProgress } from '../useMyProgress'
import { FEEDBACK_URL } from '../links'

// Thẻ một khóa: số lớp lớn, nhãn cấp học, tên, mô tả; tiến độ nếu đã học, không thì lời mời vào học.
function CourseCard({ course, mine }: { course: Course; mine: CourseProgress | undefined }) {
  const grade = gradeOf(course.slug)
  const level = levelOf(grade)
  return (
    <li>
      <Link
        to={`/khoa-hoc/${encodeURIComponent(course.slug)}`}
        className="group flex h-full flex-col border border-line bg-surface hover:border-accent"
      >
        <span className="flex items-end justify-between border-b border-line bg-sunken px-5 py-5">
          <span aria-hidden="true" className="font-serif text-6xl font-bold leading-none text-accent">
            {grade ?? '·'}
          </span>
          <span className="text-right">
            {level && <span className="block border border-ink px-2 py-0.5 text-xs font-bold tracking-wider">{level}</span>}
            {grade !== null && <span className="mt-1 block text-sm text-muted">Lớp {grade}</span>}
          </span>
        </span>
        <span className="flex flex-1 flex-col p-5">
          <h4 className="font-serif text-xl font-bold tracking-tight group-hover:underline group-hover:underline-offset-4">
            {course.title}
          </h4>
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
      <h3 id={headingId} className="flex items-baseline gap-3 font-serif text-xl font-bold tracking-tight">
        {group.title}
        <span className="text-sm font-normal text-muted">{group.courses.length} khóa</span>
      </h3>
      <ul className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {group.courses.map((course) => (
          <CourseCard key={course.id} course={course} mine={progress.find((p) => p.courseSlug === course.slug)} />
        ))}
      </ul>
    </section>
  )
}

function Hero({ courses }: { courses: Course[] | null }) {
  const grades = (courses ?? [])
    .map((course) => ({ course, grade: gradeOf(course.slug) }))
    .filter((item): item is { course: Course; grade: number } => item.grade !== null)
  return (
    <section className="max-w-3xl">
      <p className="eyebrow">Chương trình GDPT 2018 · Miễn phí</p>
      <h1 className="mt-3 font-serif text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
        Học Tin học từ lớp 6 đến lớp 12
      </h1>
      <p className="mt-4 text-lg text-muted">
        Bài học Tin học THCS và THPT bằng tiếng Việt. Học theo từng khóa, tìm bài nhanh và lưu lại tiến độ của bạn.
      </p>
      <section aria-labelledby="search-title" className="mt-7">
        <h2 id="search-title" className="sr-only">
          Tìm bài học
        </h2>
        <SearchForm />
      </section>
      {grades.length > 0 && (
        <nav aria-label="Chọn lớp" className="mt-5 flex flex-wrap items-center gap-2">
          <span className="mr-1 text-sm text-muted">Chọn lớp:</span>
          {grades.map(({ course, grade }) => (
            <Link
              key={course.id}
              to={`/khoa-hoc/${encodeURIComponent(course.slug)}`}
              className="border border-line-strong px-3 py-1 text-sm font-semibold hover:bg-ink hover:text-paper"
            >
              Lớp {grade}
            </Link>
          ))}
        </nav>
      )}
      {courses && (
        <dl className="mt-8 grid max-w-2xl grid-cols-2 gap-x-6 gap-y-4 border-t border-line pt-5 sm:grid-cols-4">
          {[
            [`${courses.length} khóa học`, 'từ lớp 6 đến lớp 12'],
            ['Miễn phí', 'không có thanh toán'],
            ['Theo chương trình', 'GDPT 2018'],
            ['Lưu tiến độ', 'khi đã đăng nhập'],
          ].map(([value, label]) => (
            <div key={value}>
              <dd className="font-serif text-lg font-bold">{value}</dd>
              <dt className="text-xs text-muted">{label}</dt>
            </div>
          ))}
        </dl>
      )}
    </section>
  )
}

function Courses({ courses, onReload, state }: { courses: Course[] | null; onReload: () => void; state: 'loading' | 'error' | 'ok' }) {
  const progress = useMyProgress()
  // Tiến độ chỉ là phần phụ: chưa đăng nhập, đang tải hoặc lỗi thì thẻ khóa hiện như cũ, không báo lỗi.
  const mine = progress.state.kind === 'ok' ? (progress.state.data ?? []) : []

  return (
    <section id="khoa-hoc" aria-labelledby="courses-title" className="mt-16 scroll-mt-6">
      <h2 id="courses-title" className="border-b-2 border-ink pb-2 font-serif text-2xl font-bold tracking-tight">
        Khóa học
      </h2>
      {state === 'loading' && <p className="mt-4 text-muted">Đang tải danh sách khóa học...</p>}
      {state === 'error' && <LoadError message="Không tải được danh sách khóa học." onRetry={onReload} />}
      {state === 'ok' && courses !== null && courses.length === 0 && <p className="mt-4 text-muted">Chưa có khóa học nào.</p>}
      {state === 'ok' &&
        courses !== null &&
        groupCourses(courses).map((group) => <CourseGroupSection key={group.key} group={group} progress={mine} />)}
    </section>
  )
}

const FEATURES = [
  ['Bài viết ngắn gọn, bám chương trình', 'Mỗi bài theo khung Chương trình GDPT 2018 môn Tin học, có ví dụ, bảng và mã mẫu.'],
  ['Kiểm tra nhanh cuối bài', 'Vài câu hỏi để tự xem mình đã hiểu bài chưa, trước khi sang bài mới.'],
  ['Lưu tiến độ học', 'Đánh dấu bài đã hoàn thành, xem phần trăm của từng khóa và bấm Học tiếp để vào đúng bài đang dở.'],
  ['Tìm kiếm toàn bộ bài', 'Gõ từ khóa ở ô tìm kiếm, gợi ý hiện ngay trong lúc bạn gõ.'],
]

function Features() {
  return (
    <section aria-labelledby="features-title" className="mt-16">
      <h2 id="features-title" className="border-b-2 border-ink pb-2 font-serif text-2xl font-bold tracking-tight">
        Mỗi bài học có gì
      </h2>
      <ol className="mt-6 grid gap-x-8 gap-y-6 sm:grid-cols-2">
        {FEATURES.map(([title, text], index) => (
          <li key={title} className="flex gap-4">
            <span aria-hidden="true" className="font-serif text-3xl font-bold leading-none text-accent">
              {String(index + 1).padStart(2, '0')}
            </span>
            <div>
              <h3 className="font-semibold">{title}</h3>
              <p className="mt-1 text-sm text-muted">{text}</p>
            </div>
          </li>
        ))}
      </ol>
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
    <section id="hoi-dap" aria-labelledby="faq-title" className="mt-16 scroll-mt-6">
      <h2 id="faq-title" className="border-b-2 border-ink pb-2 font-serif text-2xl font-bold tracking-tight">
        Câu hỏi thường gặp
      </h2>
      <div className="mt-2 divide-y divide-line border-b border-line">
        {items.map(([question, answer]) => (
          <details key={question} className="group py-4">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-4 font-semibold">
              {question}
              <span aria-hidden="true" className="text-xl leading-none text-accent group-open:rotate-45">
                +
              </span>
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
    <section aria-labelledby="cta-title" className="mt-16 border border-ink bg-sunken px-6 py-8 sm:flex sm:items-center sm:justify-between sm:gap-8">
      <div>
        <h2 id="cta-title" className="font-serif text-2xl font-bold tracking-tight">
          Bắt đầu học ngay hôm nay
        </h2>
        <p className="mt-1 text-muted">Tạo tài khoản trong một phút để đọc bài và lưu tiến độ.</p>
      </div>
      <div className="mt-5 flex flex-wrap items-center gap-4 sm:mt-0">
        <Link to="/dang-ky" className="btn btn-primary">
          Đăng ký miễn phí
        </Link>
        <Link to="/dang-nhap" className="link text-sm">
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
