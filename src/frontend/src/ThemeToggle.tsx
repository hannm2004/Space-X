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

// Biểu tượng vẽ bằng SVG (đường nét như kính lúp ở thanh đầu trang), không dùng emoji vì emoji hiện khác nhau tùy hệ điều hành.
function SunIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" className="h-5 w-5">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
    </svg>
  )
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
    <button type="button" onClick={handleClick} aria-label={label} title={label} className="btn-icon">
      {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}
