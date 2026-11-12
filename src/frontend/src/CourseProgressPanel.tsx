import { useState } from 'react'
import { Link } from 'react-router'
import type { LessonLinkState } from './lessonLinkState'
import { restartCourse, type CourseProgress } from './learningApi'
import ProgressBar from './ProgressBar'
import type { LoadState } from './useLoad'

const FOCUS = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'
const PRIMARY = `rounded-lg bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 ${FOCUS}`
const SECONDARY = `rounded-lg border border-slate-300 px-3 py-1.5 text-slate-900 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-600 dark:text-slate-100 dark:hover:bg-slate-800 ${FOCUS}`
const MUTED = 'text-sm text-slate-600 dark:text-slate-400'

type Props = {
  progress: LoadState<CourseProgress[] | null>
  courseSlug: string
  linkState: LessonLinkState
  // Gọi sau khi học lại thành công để trang tải lại tiến độ.
  onChanged: () => void
}

// Khối tiến độ ở trang khóa. Khách (kết quả null), khóa chưa có bài đã đăng hoặc chưa có trong danh sách thì không hiện gì.
export default function CourseProgressPanel({ progress, courseSlug, linkState, onChanged }: Props) {
  const [confirming, setConfirming] = useState(false)
  const [restarting, setRestarting] = useState(false)
  const [failed, setFailed] = useState(false)

  if (progress.kind === 'loading') return null
  if (progress.kind === 'error') {
    return <p className={`mt-4 ${MUTED}`}>Không tải được tiến độ học.</p>
  }
  const course = progress.data?.find((c) => c.courseSlug === courseSlug)
  if (!course || course.totalLessons === 0) return null

  function confirmRestart(courseId: number) {
    setRestarting(true)
    setFailed(false)
    restartCourse(courseId).then(
      () => {
        // Panel vẫn được gắn trong lúc tiến độ tải lại (chỉ trả null), nên phải tự đóng hộp xác nhận ở đây.
        setRestarting(false)
        setConfirming(false)
        onChanged()
      },
      () => {
        setRestarting(false)
        setFailed(true)
      },
    )
  }

  const finished = course.continueLesson === null && course.completedLessons >= course.totalLessons

  return (
    <section aria-label="Tiến độ học" className="mt-6 rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
      <ProgressBar percent={course.percent} label={`Tiến độ ${course.courseTitle}`} />
      <p className={`mt-2 ${MUTED}`}>
        {course.completedLessons}/{course.totalLessons} bài hoàn thành
      </p>

      <div className="mt-4 flex flex-wrap items-center gap-3">
        {course.continueLesson && (
          <Link
            to={`/bai/${encodeURIComponent(course.continueLesson.slug)}`}
            state={linkState}
            className={PRIMARY}
          >
            Học tiếp: {course.continueLesson.title}
          </Link>
        )}
        {finished && (
          <p role="status" className="font-medium text-green-700 dark:text-green-400">
            <span aria-hidden="true">✓ </span>Bạn đã hoàn thành khóa học này.
          </p>
        )}
        {course.completedLessons > 0 && !confirming && (
          <button type="button" onClick={() => setConfirming(true)} className={SECONDARY}>
            Học lại
          </button>
        )}
      </div>

      {confirming && (
        <div className="mt-4 rounded-lg border border-slate-200 p-4 dark:border-slate-700">
          <p>Xóa toàn bộ tiến độ của khóa này? Việc này không hoàn tác được.</p>
          {failed && (
            <p role="alert" className="mt-2 text-sm text-red-700 dark:text-red-400">
              Không học lại được, hãy thử lại.
            </p>
          )}
          <div className="mt-3 flex gap-3">
            <button
              type="button"
              disabled={restarting}
              onClick={() => confirmRestart(course.courseId)}
              className={SECONDARY}
            >
              Xác nhận học lại
            </button>
            <button
              type="button"
              disabled={restarting}
              onClick={() => {
                setConfirming(false)
                setFailed(false)
              }}
              className={SECONDARY}
            >
              Hủy
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
