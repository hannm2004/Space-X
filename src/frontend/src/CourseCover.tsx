import { useState } from 'react'

type Props = {
  slug: string
  /** Ảnh bìa do Admin tải lên; thiếu hoặc null thì vẽ hình mặc định. */
  coverImageUrl?: string | null
  className?: string
  /** Ảnh nằm ở màn hình đầu (đầu trang khóa): tải ngay và ưu tiên cao. Mặc định tải lười cho ảnh nằm dưới. */
  priority?: boolean
}

const WIDTH = 320
const HEIGHT = 180

// Chấm trang trí ở góc dưới phải: cố định nên cùng một khóa luôn ra cùng một hình.
const DOTS = Array.from({ length: 5 }, (_, row) => Array.from({ length: 6 }, (_, col) => ({ x: 214 + col * 16, y: 112 + row * 16 }))).flat()

// Chữ tắt của khóa: chữ đầu mỗi từ trong slug, số giữ nguyên ("tin-hoc-10" thành "TH10"), tối đa bốn ký tự.
function initials(slug: string): string {
  const parts = slug.split('-').filter(Boolean)
  const text = parts.map((part) => (/^\d+$/.test(part) ? part : part.charAt(0))).join('')
  return text.slice(0, 4).toUpperCase() || '·'
}

// Tổng mã ký tự của slug quyết định một trong hai kiểu nền, để các thẻ cạnh nhau có nhịp màu.
function isLight(slug: string): boolean {
  return [...slug].reduce((sum, character) => sum + character.charCodeAt(0), 0) % 2 === 0
}

// Hình mặc định khi khóa chưa có ảnh bìa: chữ tắt lớn trên khối màu phẳng, đúng bộ màu của trang nên cả hai chế độ sáng
// và tối đều đọc được. Nhờ vậy mọi khóa đều có ảnh bìa, kể cả khi Admin chưa tải ảnh lên.
function GeneratedCover({ slug }: { slug: string }) {
  const light = isLight(slug)
  const background = light ? 'fill-accent-soft' : 'fill-accent'
  const ink = light ? 'fill-accent' : 'fill-on-accent'
  return (
    <svg
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      preserveAspectRatio="xMidYMid slice"
      className="block h-full w-full"
      aria-hidden="true"
      focusable="false"
    >
      <rect width={WIDTH} height={HEIGHT} className={background} />
      <circle cx={282} cy={34} r={96} className={ink} opacity={0.12} />
      <circle cx={236} cy={172} r={58} className={ink} opacity={0.1} />
      {DOTS.map((dot) => (
        <circle key={`${dot.x}-${dot.y}`} cx={dot.x} cy={dot.y} r={2} className={ink} opacity={0.3} />
      ))}
      <text x={24} y={36} fontSize={12} fontWeight={700} letterSpacing={2} className={ink}>
        SPACE-X
      </text>
      <text x={20} y={148} fontSize={84} fontWeight={800} letterSpacing={-2} className={ink}>
        {initials(slug)}
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
