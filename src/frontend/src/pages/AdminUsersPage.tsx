import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router'
import { fetchAdminUsers, resetUserPassword, setUserEnabled, type AdminUser } from '../adminApi'
import { ApiError } from '../client'
import { errorMessages } from '../formErrors'
import { ErrorAlert } from '../FormParts'
import LoadError from '../LoadError'
import { useAuth } from '../useAuth'
import { useLoad } from '../useLoad'

const PATH = '/quan-tri/nguoi-dung'
const LINK = 'link'
const SECONDARY = `btn btn-secondary btn-sm`
const DANGER = `btn btn-danger btn-sm`
const NO_PERMISSION = 'Bạn không có quyền truy cập trang này.'

// Chỉ nhận số nguyên không âm; chữ, số âm hay số thập phân coi như trang 0.
function parsePage(raw: string | null): number {
  return raw !== null && /^\d{1,6}$/.test(raw) ? Number(raw) : 0
}

function pageHref(query: string, page: number): string {
  const params = new URLSearchParams()
  if (query !== '') params.set('q', query)
  params.set('page', String(page))
  return `${PATH}?${params}`
}

function formatDate(iso: string | null): string | null {
  if (iso === null) return null
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? null : date.toLocaleDateString('vi-VN')
}

function UserSearchForm({ initialQuery }: { initialQuery: string }) {
  const navigate = useNavigate()
  const [value, setValue] = useState(initialQuery)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const query = value.trim()
    navigate(query === '' ? PATH : `${PATH}?${new URLSearchParams({ q: query })}`)
  }

  return (
    <form role="search" onSubmit={handleSubmit} className="flex gap-2">
      <label htmlFor="user-search" className="sr-only">
        Tìm theo email hoặc tên
      </label>
      <input
        id="user-search"
        type="search"
        value={value}
        maxLength={100}
        placeholder="Tìm theo email hoặc tên"
        onChange={(event) => setValue(event.target.value)}
        className={`min-w-0 flex-1 field w-auto`}
      />
      <button
        type="submit"
        className={`btn btn-primary`}
      >
        Tìm
      </button>
    </form>
  )
}

type Confirming = 'lock' | 'reset' | null

// Mỗi dòng giữ trạng thái riêng (đang xác nhận gì, mật khẩu tạm vừa sinh) nên mật khẩu tạm chỉ tồn tại trong bộ nhớ của
// dòng này: đổi trang, tìm kiếm hoặc bấm Đóng là mất, không ghi vào địa chỉ, localStorage hay bất kỳ đâu.
function UserRow({ initial, isMe }: { initial: AdminUser; isMe: boolean }) {
  const [user, setUser] = useState(initial)
  const [confirming, setConfirming] = useState<Confirming>(null)
  const [busy, setBusy] = useState(false)
  const [messages, setMessages] = useState<string[]>([])
  const [temporaryPassword, setTemporaryPassword] = useState<string | null>(null)

  async function changeEnabled(enabled: boolean) {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      setUser(await setUserEnabled(user.id, enabled))
      setConfirming(null)
    } catch (error) {
      setMessages(
        errorMessages(
          error,
          enabled ? 'Không mở khóa được tài khoản. Hãy thử lại.' : 'Không khóa được tài khoản. Hãy thử lại.',
        ),
      )
    }
    setBusy(false)
  }

  async function resetPassword() {
    if (busy) return
    setBusy(true)
    setMessages([])
    try {
      const result = await resetUserPassword(user.id)
      setTemporaryPassword(result.temporaryPassword)
      setConfirming(null)
    } catch (error) {
      setMessages(errorMessages(error, 'Không đặt lại được mật khẩu. Hãy thử lại.'))
    }
    setBusy(false)
  }

  function cancel() {
    setConfirming(null)
    setMessages([])
  }

  const joined = formatDate(user.createdAt)
  const canReset = user.authProvider === 'LOCAL'

  return (
    <li className="border border-line bg-surface p-4">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <span className="font-semibold">{user.displayName}</span>
        {isMe && <span className="text-sm text-muted">(bạn)</span>}
        <span className="border border-line bg-sunken px-2 py-0.5 text-xs">
          {user.role === 'ADMIN' ? 'Quản trị viên' : 'Học sinh'}
        </span>
        {user.authProvider === 'GOOGLE' && (
          <span className="border border-line bg-sunken px-2 py-0.5 text-xs">Google</span>
        )}
        {/* Trạng thái bằng chữ, không chỉ bằng màu. */}
        <span
          className={
            user.enabled
              ? 'text-sm font-medium text-success'
              : 'text-sm font-medium text-danger'
          }
        >
          {user.enabled ? 'Đang hoạt động' : 'Đã khóa'}
        </span>
      </div>
      <p className="mt-1 break-all text-ink">{user.email}</p>
      {joined && <p className="mt-1 text-sm text-muted">{`Tham gia ${joined}`}</p>}

      {!isMe && confirming === null && (
        <div className="mt-3 flex flex-wrap gap-3">
          {user.enabled ? (
            <button
              type="button"
              aria-label={`Khóa ${user.email}`}
              onClick={() => setConfirming('lock')}
              className={SECONDARY}
            >
              Khóa
            </button>
          ) : (
            <button
              type="button"
              aria-label={`Mở khóa ${user.email}`}
              disabled={busy}
              onClick={() => changeEnabled(true)}
              className={SECONDARY}
            >
              Mở khóa
            </button>
          )}
          {canReset && (
            <button
              type="button"
              aria-label={`Đặt lại mật khẩu ${user.email}`}
              onClick={() => setConfirming('reset')}
              className={SECONDARY}
            >
              Đặt lại mật khẩu
            </button>
          )}
        </div>
      )}

      {confirming === 'lock' && (
        <div className="mt-3 border border-line bg-surface p-3">
          <p>{`Khóa tài khoản ${user.email}? Người này sẽ không đăng nhập được và bị đăng xuất khỏi mọi thiết bị.`}</p>
          <div className="mt-3 flex gap-3">
            <button type="button" disabled={busy} onClick={() => changeEnabled(false)} className={DANGER}>
              Xác nhận khóa
            </button>
            <button type="button" disabled={busy} onClick={cancel} className={SECONDARY}>
              Hủy
            </button>
          </div>
        </div>
      )}

      {confirming === 'reset' && (
        <div className="mt-3 border border-line bg-surface p-3">
          <p>
            {`Đặt lại mật khẩu của ${user.email}? Mật khẩu cũ sẽ không dùng được và người này bị đăng xuất khỏi mọi thiết bị.`}
          </p>
          <div className="mt-3 flex gap-3">
            <button type="button" disabled={busy} onClick={resetPassword} className={DANGER}>
              Xác nhận đặt lại
            </button>
            <button type="button" disabled={busy} onClick={cancel} className={SECONDARY}>
              Hủy
            </button>
          </div>
        </div>
      )}

      {messages.length > 0 && (
        <div className="mt-3">
          <ErrorAlert messages={messages} />
        </div>
      )}

      {temporaryPassword !== null && (
        <div
          role="status"
          className="mt-3 border-l-4 border-warn bg-sunken p-3 text-ink"
        >
          <p>
            {`Mật khẩu tạm của ${user.email}: `}
            <code className="select-all break-all font-mono text-base font-semibold">{temporaryPassword}</code>
          </p>
          <p className="mt-2 text-sm">
            Mật khẩu này chỉ hiện một lần. Hãy báo cho người dùng và nhắc đổi mật khẩu sau khi đăng nhập. Các thiết bị
            của họ đã bị đăng xuất.
          </p>
          <button type="button" onClick={() => setTemporaryPassword(null)} className={`mt-2 ${SECONDARY}`}>
            Đóng
          </button>
        </div>
      )}
    </li>
  )
}

function UserList({ query, page, myEmail }: { query: string; page: number; myEmail: string }) {
  const { state, reload } = useLoad(() => fetchAdminUsers(query, page), `${page}:${query}`)

  if (state.kind === 'loading') {
    return <p className="mt-6 text-muted">Đang tải...</p>
  }
  if (state.kind === 'error') {
    if (state.error instanceof ApiError && state.error.status === 403) {
      return (
        <p role="alert" className="mt-6 font-medium text-danger">
          {NO_PERMISSION}
        </p>
      )
    }
    return <LoadError message="Không tải được danh sách người dùng." onRetry={reload} />
  }

  const result = state.data
  if (result.totalItems === 0) {
    return <p className="mt-6 text-lg">Không có người dùng phù hợp.</p>
  }
  // Có người dùng nhưng trang này rỗng (địa chỉ chứa số trang quá lớn).
  if (result.items.length === 0) {
    return (
      <p className="mt-6">
        Không có kết quả ở trang này.{' '}
        <Link to={pageHref(query, 0)} className={LINK}>
          Về trang đầu
        </Link>
      </p>
    )
  }

  const totalPages = Math.ceil(result.totalItems / result.size)
  const me = myEmail.toLowerCase()
  return (
    <>
      <p className="mt-6 text-muted">{`Tìm thấy ${result.totalItems} người dùng`}</p>
      <ul className="mt-4 space-y-3">
        {result.items.map((user) => (
          <UserRow key={user.id} initial={user} isMe={user.email.toLowerCase() === me} />
        ))}
      </ul>
      {totalPages > 1 && (
        <nav aria-label="Phân trang" className="mt-6 flex items-center gap-4">
          {result.page > 0 && (
            <Link to={pageHref(query, result.page - 1)} className={LINK}>
              Trang trước
            </Link>
          )}
          <span>{`Trang ${result.page + 1} / ${totalPages}`}</span>
          {result.page + 1 < totalPages && (
            <Link to={pageHref(query, result.page + 1)} className={LINK}>
              Trang sau
            </Link>
          )}
        </nav>
      )}
    </>
  )
}

// Giao diện chỉ để tiện dùng: ẩn trang với người không phải Admin. Quyền thật do backend kiểm ở mọi API
// (`/api/admin/**` → 401/403), nên sửa giao diện cũng không vượt được quyền.
export default function AdminUsersPage() {
  const { state } = useAuth()
  const location = useLocation()
  const [params] = useSearchParams()

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

  const query = (params.get('q') ?? '').trim()
  const page = parsePage(params.get('page'))
  return (
    <>
      <h1 className="font-serif text-4xl font-bold leading-tight tracking-tight">Quản lý người dùng</h1>
      <div className="mt-6">
        {/* key theo từ khóa để ô nhập đồng bộ lại khi từ khóa trong địa chỉ đổi */}
        <UserSearchForm key={query} initialQuery={query} />
      </div>
      <UserList query={query} page={page} myEmail={state.user.email} />
    </>
  )
}
