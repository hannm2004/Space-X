// Chữ "G" bốn màu theo bộ nhận diện của Google (đường vẽ SVG chính thức, giữ nguyên màu và tỉ lệ, không đổi màu theo
// chế độ tối). aria-hidden vì chữ bên cạnh đã nói rõ nút làm gì; trình đọc màn hình không đọc hình trang trí.
function GoogleLogo() {
  return (
    <svg viewBox="0 0 48 48" width="18" height="18" aria-hidden="true" focusable="false">
      <path
        fill="#EA4335"
        d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
      />
      <path
        fill="#4285F4"
        d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
      />
      <path
        fill="#FBBC05"
        d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
      />
      <path
        fill="#34A853"
        d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
      />
    </svg>
  )
}

// Đăng ký và đăng nhập bằng Google cùng một đường dẫn: lần đầu máy chủ tự tạo tài khoản (docs/API.md), nên hai trang
// chỉ khác chữ trên nút. Thẻ <a> thường, không dùng <Link>: đây là yêu cầu GET chuyển hướng sang Google do máy chủ xử
// lý, phải tải trang đầy đủ.
export default function GoogleSignIn({ label }: { label: string }) {
  return (
    <div className="mt-6">
      {/* Đường kẻ hai bên chữ "hoặc" ngăn cách form mật khẩu với đăng nhập Google. */}
      <p className="mb-5 flex items-center gap-4 text-sm text-muted before:h-px before:flex-1 before:bg-line after:h-px after:flex-1 after:bg-line">
        hoặc
      </p>
      <a href="/oauth2/authorization/google" className="btn btn-neutral w-full">
        <GoogleLogo />
        {label}
      </a>
    </div>
  )
}
