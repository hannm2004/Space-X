type TextFieldProps = {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  type?: 'text' | 'email' | 'password'
  autoComplete?: string
  hint?: string
}

export function TextField({ id, label, value, onChange, type = 'text', autoComplete, hint }: TextFieldProps) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-semibold">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        autoComplete={autoComplete}
        aria-describedby={hint ? `${id}-hint` : undefined}
        onChange={(event) => onChange(event.target.value)}
        className="field mt-1"
      />
      {hint && (
        <p id={`${id}-hint`} className="mt-1 text-sm text-muted">
          {hint}
        </p>
      )}
    </div>
  )
}

// Thông báo lỗi của form; trình đọc màn hình đọc ngay khi xuất hiện nhờ role="alert". Vạch đỏ bên trái như ghi chú bên lề.
export function ErrorAlert({ messages }: { messages: string[] }) {
  if (messages.length === 0) return null
  return (
    <div role="alert" className="rounded-lg border border-danger/40 bg-danger-soft px-4 py-3 text-danger">
      {messages.length === 1 ? (
        <p>{messages[0]}</p>
      ) : (
        <ul className="list-disc pl-5">
          {messages.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function SubmitButton({ pending, label, pendingLabel }: { pending: boolean; label: string; pendingLabel: string }) {
  return (
    <button type="submit" disabled={pending} className="btn btn-primary">
      {pending ? pendingLabel : label}
    </button>
  )
}
