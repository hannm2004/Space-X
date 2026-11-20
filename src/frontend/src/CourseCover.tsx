import { useState } from 'react'
import { gradeOf } from './courseGroups'

type Props = {
  slug: string
  /** Ảnh bìa do Admin tải lên; thiếu hoặc null thì vẽ hình mặc định theo lớp. */
  coverImageUrl?: string | null
  className?: string
  /** Ảnh nằm ở màn hình đầu (đầu trang khóa): tải ngay và ưu tiên cao. Mặc định tải lười cho ảnh nằm dưới. */
  priority?: boolean
}

const WIDTH = 320
const HEIGHT = 180
const GRID = 20

// Đường kẻ ô vuông như giấy kẻ ly: một đường path duy nhất cho cả lưới.
const GRID_PATH = (() => {
  const parts: string[] = []
  for (let x = GRID; x < WIDTH; x += GRID) parts.push(`M${x} 0V${HEIGHT}`)
  for (let y = GRID; y < HEIGHT; y += GRID) parts.push(`M0 ${y}H${WIDTH}`)
  return parts.join('')
})()

// Dãy 0/1 trang trí (gợi dữ liệu máy tính). Sinh theo số hạt giống nên cùng một khóa luôn ra cùng một hình.
function binaryRows(seed: number): string[] {
  let state = seed * 7919 + 13
  const rows: string[] = []
  for (let row = 0; row < 4; row++) {
    let digits = ''
    for (let i = 0; i < 10; i++) {
      state = (state * 9301 + 49297) % 233280
      digits += state % 2
    }
    rows.push(digits)
  }
  return rows
}

function seedOf(slug: string, grade: number | null): number {
  if (grade !== null) return grade
  return Array.from(slug).reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

// Hình mặc định khi khóa chưa có ảnh bìa: số lớp lớn trên nền giấy kẻ ô, đúng bộ màu của trang nên cả hai chế độ sáng
// và tối đều đọc được. Nhờ vậy mọi khóa đều có ảnh bìa, kể cả khi Admin chưa tải ảnh lên.
function GeneratedCover({ slug }: { slug: string }) {
  const grade = gradeOf(slug)
  const label = grade !== null ? String(grade) : (slug.trim().charAt(0) || '·').toUpperCase()
  const rows = binaryRows(seedOf(slug, grade))
  return (
    <svg
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      preserveAspectRatio="xMidYMid slice"
      className="block h-full w-full"
      aria-hidden="true"
      focusable="false"
    >
      <rect width={WIDTH} height={HEIGHT} className="fill-sunken" />
      <path d={GRID_PATH} className="stroke-line" strokeWidth={1} fill="none" />
      {rows.map((digits, index) => (
        <text
          key={index}
          x={WIDTH - 16}
          y={44 + index * 22}
          textAnchor="end"
          fontSize={15}
          letterSpacing={4}
          className="fill-line-strong font-mono"
        >
          {digits}
        </text>
      ))}
      <text x={24} y={34} fontSize={12} fontWeight={700} letterSpacing={3} className="fill-muted">
        TIN HỌC
      </text>
      <rect x={24} y={44} width={36} height={3} className="fill-accent" />
      <text x={22} y={152} fontSize={112} fontWeight={700} className="fill-accent font-serif">
        {label}
      </text>
    </svg>
  )
}

// Ảnh bìa khóa học, tỉ lệ 16:9. Ảnh là phần trang trí (tên khóa luôn nằm ngay cạnh) nên alt để trống.
export default function CourseCover({ slug, coverImageUrl = null, className = '', priority = false }: Props) {
  // Nhớ đường dẫn đã lỗi để chuyển sang hình mặc định; đổi sang đường dẫn mới thì thử lại.
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const showImage = coverImageUrl !== null && coverImageUrl !== failedUrl
  return (
    <div
      data-testid="course-cover"
      data-kind={showImage ? 'image' : 'generated'}
      className={`aspect-video overflow-hidden bg-sunken ${className}`}
    >
      {showImage ? (
        <img
          src={coverImageUrl}
          alt=""
          width={WIDTH}
          height={HEIGHT}
          loading={priority ? 'eager' : 'lazy'}
          fetchPriority={priority ? 'high' : undefined}
          decoding="async"
          onError={() => setFailedUrl(coverImageUrl)}
          className="h-full w-full object-cover"
        />
      ) : (
        <GeneratedCover slug={slug} />
      )}
    </div>
  )
}
