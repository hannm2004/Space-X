import { SECONDARY } from './buttons'

export default function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div role="alert" className="mt-4 rounded-lg border border-danger/40 bg-danger-soft px-4 py-3">
      <p className="font-medium text-danger">{message}</p>
      <button type="button" onClick={onRetry} className={`${SECONDARY} mt-3`}>
        Thử lại
      </button>
    </div>
  )
}
