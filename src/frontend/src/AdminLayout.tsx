import { NavLink, Outlet } from 'react-router'
import { useAuth } from './useAuth'

const ITEMS = [
  { to: '/quan-tri', label: 'Tổng quan', end: true },
  { to: '/quan-tri/nguoi-dung', label: 'Người dùng', end: false },
  { to: '/quan-tri/khoa-hoc', label: 'Khóa học', end: false },
  { to: '/quan-tri/bai-hoc', label: 'Bài học', end: false },
]

// Khung của khu quản trị: thanh mục riêng phía trên nội dung. Chỉ hiện với Admin; khách và học sinh vẫn thấy câu
// "không có quyền" hoặc được chuyển sang đăng nhập do từng trang tự xử lý, không thấy các mục quản lý.
export default function AdminLayout() {
  const { state } = useAuth()
  const isAdmin = state.status === 'authenticated' && state.user.role === 'ADMIN'

  return (
    <>
      {isAdmin && (
        <nav aria-label="Quản trị" className="-mt-2 mb-8 flex flex-wrap gap-x-6 gap-y-1 border-b border-line">
          {ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className="-mb-px border-b-2 border-transparent py-2 text-[0.9375rem] font-medium hover:border-line-strong aria-[current=page]:border-accent"
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      )}
      <Outlet />
    </>
  )
}
