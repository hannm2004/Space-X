import type { ReactNode } from 'react'
import Markdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'

// Chỉ nhận ảnh do chính hệ thống lưu (/api/images/{id}). Ảnh từ trang ngoài có thể dùng để theo dõi người học.
const OWN_IMAGE = /^\/api\/images\/\d+$/

// Tiêu đề của bài là h1 của trang, nên tiêu đề trong nội dung hạ một bậc (# thành h2, ## thành h3...).
function heading(Tag: 'h2' | 'h3' | 'h4' | 'h5' | 'h6', className: string) {
  return function Heading({ children }: { children?: ReactNode }) {
    return <Tag className={className}>{children}</Tag>
  }
}

const components: Components = {
  h1: heading('h2', 'mt-8 text-2xl font-bold tracking-tight'),
  h2: heading('h3', 'mt-8 text-xl font-semibold tracking-tight'),
  h3: heading('h4', 'mt-6 text-lg font-semibold'),
  h4: heading('h5', 'mt-6 font-semibold'),
  h5: heading('h6', 'mt-4 font-semibold'),
  h6: heading('h6', 'mt-4 font-semibold'),
  p: ({ children }) => <p className="mt-4 leading-relaxed">{children}</p>,
  ul: ({ children }) => <ul className="mt-4 list-disc space-y-1 pl-6">{children}</ul>,
  ol: ({ children }) => <ol className="mt-4 list-decimal space-y-1 pl-6">{children}</ol>,
  blockquote: ({ children }) => (
    <blockquote className="mt-4 border-l-4 border-slate-300 pl-4 text-slate-600 dark:border-slate-600 dark:text-slate-400">
      {children}
    </blockquote>
  ),
  pre: ({ children }) => (
    <pre className="mt-4 overflow-x-auto rounded-lg bg-slate-100 p-4 text-sm dark:bg-slate-800 [&>code]:bg-transparent [&>code]:p-0">
      {children}
    </pre>
  ),
  code: ({ children }) => (
    <code className="rounded bg-slate-100 px-1 py-0.5 font-mono text-[0.9em] dark:bg-slate-800">
      {children}
    </code>
  ),
  table: ({ children }) => (
    <div className="mt-4 overflow-x-auto">
      <table className="w-full border-collapse text-left">{children}</table>
    </div>
  ),
  th: ({ children }) => (
    <th className="border border-slate-300 bg-slate-100 px-3 py-2 font-semibold dark:border-slate-600 dark:bg-slate-800">
      {children}
    </th>
  ),
  td: ({ children }) => (
    <td className="border border-slate-300 px-3 py-2 dark:border-slate-600">{children}</td>
  ),
  hr: () => <hr className="my-8 border-slate-200 dark:border-slate-700" />,
  a: ({ href, children }) => {
    // react-markdown đã bỏ địa chỉ javascript:/data:, khi đó href rỗng và chỉ giữ chữ.
    if (!href) return <span>{children}</span>
    const external = /^https?:\/\//i.test(href)
    return (
      <a
        href={href}
        {...(external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
        className="text-blue-700 underline hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300"
      >
        {children}
      </a>
    )
  },
  img: ({ src, alt }) => {
    if (typeof src === 'string' && OWN_IMAGE.test(src)) {
      return <img src={src} alt={alt ?? ''} loading="lazy" className="mt-4 h-auto max-w-full rounded-lg" />
    }
    return <span className="italic text-slate-600 dark:text-slate-400">{alt}</span>
  },
}

// Hiển thị nội dung bài (Markdown) an toàn: HTML nhúng không được dựng thành phần tử, địa chỉ nguy hiểm bị bỏ.
export default function MarkdownContent({ source }: { source: string }) {
  return (
    <Markdown remarkPlugins={[remarkGfm]} components={components}>
      {source}
    </Markdown>
  )
}
