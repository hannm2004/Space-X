// Thanh tiến độ dùng chung (trang chủ, trang khóa). Phần trăm do máy chủ tính; ở đây chỉ đưa về 0–100 để thanh không tràn.
// Thanh mảnh, góc vuông, màu nhấn: giống thanh đánh dấu trang hơn là thanh tải.
export default function ProgressBar({ percent, label }: { percent: number; label: string }) {
  const value = Number.isFinite(percent) ? Math.min(100, Math.max(0, Math.round(percent))) : 0
  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={value}
      className="h-1.5 w-full overflow-hidden bg-sunken"
    >
      <div data-testid="progress-fill" className="h-full bg-accent" style={{ width: `${value}%` }} />
    </div>
  )
}
