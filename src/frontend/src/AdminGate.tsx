import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { NO_PERMISSION } from './adminUi'
import { useAuth } from './useAuth'

// Giao diện chỉ để tiện dùng: ẩn trang với người không phải Admin. Quyền thật do backend kiểm ở mọi API
// (`/api/admin/**` → 401/403), nên sửa giao diện cũng không vượt được quyền. `children` chỉ được dựng khi đã chắc là Admin,
// nên các hook tải dữ liệu bên trong không chạy (không gọi API) với khách hoặc học sinh.
export default function AdminGate({ children }: { children: ReactNode }) {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'loading') {
    return <p className="text-muted">Đang tải...</p>
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/dang-nhap" replace state={{ from: location.pathname + location.search }} />
  }
  if (state.user.role !== 'ADMIN') {
    return (
      <p role="alert" className="font-medium text-danger">
        {NO_PERMISSION}
      </p>
    )
  }
  return <>{children}</>
}
