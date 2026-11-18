// Chuỗi class dùng chung cho các trang quản trị nội dung (khóa học, bài học).
export const FOCUS = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'
export const LINK = 'text-blue-700 underline dark:text-blue-400'
export const SECONDARY = `rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-900 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-600 dark:text-slate-100 dark:hover:bg-slate-800 ${FOCUS}`
export const DANGER = `rounded-lg bg-red-700 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-800 disabled:cursor-not-allowed disabled:opacity-60 ${FOCUS}`
export const CARD = 'rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900'
export const SUCCESS_BOX =
  'mt-4 rounded-lg border border-green-300 bg-green-50 px-3 py-2 text-green-900 dark:border-green-800 dark:bg-green-950 dark:text-green-200'
export const NO_PERMISSION = 'Bạn không có quyền truy cập trang này.'
