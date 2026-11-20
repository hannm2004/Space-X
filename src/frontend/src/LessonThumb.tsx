// Ảnh bìa nhỏ của bài (do Admin đặt). Chỉ trang trí nên alt rỗng: tên bài đã có ngay cạnh.
// Không có ảnh bìa thì không vẽ gì để dòng bài vẫn gọn như trước.
export default function LessonThumb({ url }: { url?: string | null }) {
  if (!url) return null
  return <img src={url} alt="" loading="lazy" className="h-14 w-20 shrink-0 rounded-md border border-line object-cover sm:h-16 sm:w-24" />
}
