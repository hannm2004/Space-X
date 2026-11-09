import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Không tìm thấy trang</h1>
      <p className="mt-3 text-slate-600 dark:text-slate-400">
        Đường dẫn bạn mở không tồn tại hoặc đã được đổi.
      </p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-lg border border-slate-300 px-3 py-1.5 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-600 dark:hover:bg-slate-800"
      >
        Về trang chủ
      </Link>
    </>
  )
}
