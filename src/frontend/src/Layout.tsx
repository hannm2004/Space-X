import { Link, Outlet } from 'react-router'
import ThemeToggle from './ThemeToggle'

// Khung chung của mọi trang: thanh đầu trang và vùng nội dung của trang đang mở.
export default function Layout() {
  return (
    <div className="min-h-screen bg-white text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <header className="mx-auto flex max-w-3xl items-center justify-between px-4 py-6 sm:px-6">
        <Link
          to="/"
          className="rounded text-xl font-bold tracking-tight focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
        >
          Space-X
        </Link>
        <ThemeToggle />
      </header>

      <main className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
        <Outlet />
      </main>
    </div>
  )
}
