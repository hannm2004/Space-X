import { useState } from 'react'
import { applyTheme, resolveInitialTheme, toggleTheme, type Theme } from './theme'

const STORAGE_KEY = 'theme'

function readStoredTheme(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    // Trình duyệt chặn localStorage (ví dụ chế độ riêng tư): coi như chưa lưu gì.
    return null
  }
}

function saveTheme(theme: Theme): void {
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // Vẫn đổi được giao diện, chỉ là không nhớ được lựa chọn cho lần sau.
  }
}

export default function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(() =>
    resolveInitialTheme(
      readStoredTheme(),
      window.matchMedia('(prefers-color-scheme: dark)').matches,
    ),
  )

  function handleClick() {
    const next = toggleTheme(theme)
    setTheme(next)
    applyTheme(next, document.documentElement)
    saveTheme(next)
  }

  const label = theme === 'dark' ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'

  return (
    <button
      type="button"
      onClick={handleClick}
      aria-label={label}
      title={label}
      className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700"
    >
      <span aria-hidden="true">{theme === 'dark' ? '☀️' : '🌙'}</span>
    </button>
  )
}
