export type Theme = 'light' | 'dark'

export function resolveInitialTheme(stored: string | null, prefersDark: boolean): Theme {
  if (stored === 'light' || stored === 'dark') {
    return stored
  }
  return prefersDark ? 'dark' : 'light'
}

export function toggleTheme(theme: Theme): Theme {
  return theme === 'light' ? 'dark' : 'light'
}

export function applyTheme(
  theme: Theme,
  element: { classList: { toggle(token: string, force?: boolean): boolean } },
): void {
  element.classList.toggle('dark', theme === 'dark')
}
