import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <>
      <h1 className="font-serif text-4xl font-bold leading-tight tracking-tight">Không tìm thấy trang</h1>
      <p className="mt-3 text-muted">
        Đường dẫn bạn mở không tồn tại hoặc đã được đổi.
      </p>
      <Link
        to="/"
        className="btn btn-secondary mt-6"
      >
        Về trang chủ
      </Link>
    </>
  )
}
