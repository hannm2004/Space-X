// Thanh tiến độ dùng chung (trang chủ, trang khóa). Phần trăm do máy chủ tính; ở đây chỉ đưa về 0–100 để thanh không tràn.
export default function ProgressBar({ percent, label }: { percent: number; label: string }) {
  const value = Number.isFinite(percent) ? Math.min(100, Math.max(0, Math.round(percent))) : 0
  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={value}
      className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700"
    >
      <div data-testid="progress-fill" className="h-full rounded-full bg-blue-700 dark:bg-blue-400" style={{ width: `${value}%` }} />
    </div>
  )
}
