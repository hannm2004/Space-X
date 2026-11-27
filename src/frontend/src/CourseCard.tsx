import { Link } from 'react-router'
import type { CatalogCourse } from './catalogApi'
import CourseCover from './CourseCover'
import { formatVnd, LEVEL_LABELS } from './courseLabels'
import type { CourseProgress } from './learningApi'
import ProgressBar from './ProgressBar'

type Props = {
  course: CatalogCourse
  /** Tiến độ của người đang đăng nhập cho khóa này, nếu có. */
  mine?: CourseProgress
  /** Cấp tiêu đề theo vị trí trên trang: dưới h1 thì h2, dưới h2 thì h3. */
  heading: 'h2' | 'h3'
}

// Thẻ một khóa, dùng ở trang chủ và trang danh sách: cả thẻ là một liên kết tới trang khóa.
export default function CourseCard({ course, mine, heading: Heading }: Props) {
  return (
    <li>
      <Link
        to={`/khoa-hoc/${encodeURIComponent(course.slug)}`}
        className="group flex h-full flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-card transition hover:-translate-y-0.5 hover:border-accent hover:shadow-lift"
      >
        {/* Ảnh bìa công khai (khách cũng thấy). Khóa chưa có ảnh thì CourseCover vẽ hình mặc định. */}
        <CourseCover slug={course.slug} coverImageUrl={course.coverImageUrl} />
        <span className="flex flex-1 flex-col p-5">
          <span className="mb-3 flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="rounded-full bg-accent-soft px-2.5 py-0.5 text-xs font-semibold text-accent">
              {course.category.name}
            </span>
            {/* Số bài đã đăng; khóa chưa có bài ghi "Sắp có bài" thay vì "0 bài" để không giống một lỗi. */}
            <span className="ml-auto text-xs font-medium text-muted">
              {course.lessonCount > 0 ? `${course.lessonCount} bài` : 'Sắp có bài'}
            </span>
          </span>
          <Heading className="text-lg font-bold tracking-tight group-hover:text-accent">{course.title}</Heading>
          {course.description && <span className="mt-2 block text-sm text-muted">{course.description}</span>}
          <span className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-sm text-muted">
            <span>{LEVEL_LABELS[course.level]}</span>
            {course.totalHours > 0 && <span>{`${course.totalHours} giờ`}</span>}
          </span>
          <span className="mt-auto block pt-4">
            {course.tuitionVnd > 0 ? (
              <span className="text-lg font-bold tabular-nums">{formatVnd(course.tuitionVnd)}</span>
            ) : (
              <span className="text-sm font-medium text-muted">Học phí đang cập nhật</span>
            )}
          </span>
          {mine && mine.totalLessons > 0 && (
            <span className="mt-3 block">
              <ProgressBar percent={mine.percent} label={`Tiến độ ${course.title}`} />
              <span className="mt-1.5 block text-sm text-muted">
                {mine.completedLessons}/{mine.totalLessons} bài hoàn thành
              </span>
            </span>
          )}
        </span>
      </Link>
    </li>
  )
}
