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
  h1: heading('h2', 'mt-12 font-serif text-3xl font-bold tracking-tight'),
  h2: heading('h3', 'mt-10 border-t border-line pt-6 font-serif text-2xl font-bold tracking-tight'),
  h3: heading('h4', 'mt-8 font-serif text-xl font-bold'),
  h4: heading('h5', 'mt-6 font-semibold'),
  h5: heading('h6', 'mt-4 font-semibold'),
  h6: heading('h6', 'mt-4 font-semibold'),
  p: ({ children }) => <p className="mt-4 leading-[1.75]">{children}</p>,
  ul: ({ children }) => <ul className="mt-4 list-disc space-y-1.5 pl-6 marker:text-accent">{children}</ul>,
  ol: ({ children }) => <ol className="mt-4 list-decimal space-y-1.5 pl-6 marker:font-semibold marker:text-accent">{children}</ol>,
  blockquote: ({ children }) => (
    <blockquote className="mt-6 border-l-4 border-accent bg-sunken px-5 py-1 text-muted">{children}</blockquote>
  ),
  pre: ({ children }) => (
    <pre className="mt-5 overflow-x-auto border border-line bg-sunken p-4 text-sm [&>code]:border-0 [&>code]:bg-transparent [&>code]:p-0">
      {children}
    </pre>
  ),
  code: ({ children }) => (
    <code className="border border-line bg-sunken px-1 py-0.5 font-mono text-[0.9em]">{children}</code>
  ),
  table: ({ children }) => (
    <div className="mt-5 overflow-x-auto">
      <table className="w-full border-collapse text-left">{children}</table>
    </div>
  ),
  th: ({ children }) => (
    <th className="border border-line-strong bg-sunken px-3 py-2 text-sm font-semibold">{children}</th>
  ),
  td: ({ children }) => <td className="border border-line-strong px-3 py-2">{children}</td>,
  hr: () => <hr className="my-10 border-line" />,
  a: ({ href, children }) => {
    // react-markdown đã bỏ địa chỉ javascript:/data:, khi đó href rỗng và chỉ giữ chữ.
    if (!href) return <span>{children}</span>
    const external = /^https?:\/\//i.test(href)
    return (
      <a
        href={href}
        {...(external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
        className="link"
      >
        {children}
      </a>
    )
  },
  img: ({ src, alt }) => {
    if (typeof src === 'string' && OWN_IMAGE.test(src)) {
      return <img src={src} alt={alt ?? ''} loading="lazy" className="mt-5 h-auto max-w-full border border-line" />
    }
    return <span className="italic text-muted">{alt}</span>
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
