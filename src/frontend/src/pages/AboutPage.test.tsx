import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import AboutPage from './AboutPage'

function renderPage() {
  return render(
    <MemoryRouter>
      <AboutPage />
    </MemoryRouter>,
  )
}

describe('AboutPage', () => {
  it('có tiêu đề chính và các mục: đối tượng, nội dung, tính năng, đang phát triển, góp ý', () => {
    renderPage()

    expect(screen.getByRole('heading', { level: 1, name: 'Giới thiệu Space-X' })).not.toBeNull()
    for (const muc of ['Space-X dành cho ai', 'Nội dung học', 'Bạn làm được gì', 'Đang phát triển', 'Góp ý và báo lỗi']) {
      expect(screen.getByRole('heading', { level: 2, name: muc })).not.toBeNull()
    }
  })

  it('giới thiệu đúng mô hình: Trung tâm Tin học SPACE-X hư cấu, tự học trực tuyến, đồ án không thu tiền thật', () => {
    const { container } = renderPage()

    expect(container.textContent).toContain('Trung tâm Tin học SPACE-X')
    expect(container.textContent).toMatch(/tự học Tin học trực tuyến/i)
    expect(container.textContent).toMatch(/hư cấu/i)
    expect(screen.getByText(/đồ án chuyên ngành/i)).not.toBeNull()
    expect(container.textContent).toMatch(/không thu tiền thật/i)
    expect(screen.getByText(/không sao chép sách giáo khoa/i)).not.toBeNull()
  })

  it('không còn nội dung của đề tài cũ: lớp 6 đến lớp 12, THCS, THPT, GDPT', () => {
    const { container } = renderPage()

    expect(container.textContent).not.toMatch(/lớp 6|lớp 12|trung học|THCS|THPT|GDPT/i)
  })

  it('ghi mua khóa, chatbot và trợ lý AI là việc đang làm, không khẳng định đã có', () => {
    renderPage()

    const section = screen.getByRole('heading', { level: 2, name: 'Đang phát triển' }).closest('section')!
    expect(within(section).getByText(/mua khóa/i)).not.toBeNull()
    expect(within(section).getByText(/chatbot/i)).not.toBeNull()
    expect(within(section).getByText(/chưa có trên bản hiện tại/i)).not.toBeNull()
  })

  it('có liên kết bắt đầu học và liên kết góp ý mở trang ngoài an toàn', () => {
    renderPage()

    expect(screen.getByRole('link', { name: 'Xem các khóa học' }).getAttribute('href')).toBe('/#khoa-hoc')
    const feedback = screen.getByRole('link', { name: /GitHub/ })
    expect(feedback.getAttribute('href')).toMatch(/^https:\/\/github\.com\//)
    expect(feedback.getAttribute('target')).toBe('_blank')
    expect(feedback.getAttribute('rel')).toContain('noopener')
  })

  it('không chứa thông tin cá nhân của người làm (email, mã số sinh viên)', () => {
    const { container } = renderPage()

    expect(container.textContent).not.toMatch(/@[a-z0-9-]+\.[a-z]{2,}/i)
    expect(container.textContent).not.toMatch(/\b\d{10}\b/)
  })
})
