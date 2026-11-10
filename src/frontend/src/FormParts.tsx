const FOCUS = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'

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
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        autoComplete={autoComplete}
        aria-describedby={hint ? `${id}-hint` : undefined}
        onChange={(event) => onChange(event.target.value)}
        className={`mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 ${FOCUS}`}
      />
      {hint && (
        <p id={`${id}-hint`} className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          {hint}
        </p>
      )}
    </div>
  )
}

// Thông báo lỗi của form; trình đọc màn hình đọc ngay khi xuất hiện nhờ role="alert".
export function ErrorAlert({ messages }: { messages: string[] }) {
  if (messages.length === 0) return null
  return (
    <div
      role="alert"
      className="rounded-lg border border-red-300 bg-red-50 px-3 py-2 text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-200"
    >
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
    <button
      type="submit"
      disabled={pending}
      className={`rounded-lg bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-60 ${FOCUS}`}
    >
      {pending ? pendingLabel : label}
    </button>
  )
}
