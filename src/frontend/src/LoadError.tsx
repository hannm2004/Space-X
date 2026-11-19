export default function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div role="alert" className="mt-4 border-l-4 border-danger bg-danger-soft px-4 py-3">
      <p className="font-medium text-danger">{message}</p>
      <button type="button" onClick={onRetry} className="btn btn-secondary btn-sm mt-3">
        Thử lại
      </button>
    </div>
  )
}
