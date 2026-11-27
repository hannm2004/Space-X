import { render, screen, within } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { CopyrightPage, PrivacyPage, TermsPage } from './PolicyPages'

function renderPage(page: ReactElement) {
  return render(<MemoryRouter>{page}</MemoryRouter>)
}

describe('PrivacyPage', () => {
  it('nói rõ dữ liệu nào được lưu và để làm gì', () => {
    renderPage(<PrivacyPage />)

    expect(screen.getByRole('heading', { level: 1, name: 'Chính sách bảo mật' })).not.toBeNull()
    for (const muc of ['Dữ liệu chúng tôi lưu', 'Cookie và bộ nhớ trình duyệt', 'Đăng nhập bằng Google', 'Quyền của bạn']) {
      expect(screen.getByRole('heading', { level: 2, name: muc })).not.toBeNull()
    }
  })

  it('liệt kê đúng các cookie ứng dụng dùng và nói không có quảng cáo hay theo dõi bên thứ ba', () => {
    renderPage(<PrivacyPage />)

    expect(screen.getAllByText(/SESSION/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/XSRF-TOKEN/).length).toBeGreaterThan(0)
    expect(screen.getByText(/không có quảng cáo/i)).not.toBeNull()
  })

  it('nói mật khẩu không lưu dạng gốc và có ngày cập nhật', () => {
    renderPage(<PrivacyPage />)

    expect(screen.getByText(/không lưu mật khẩu dạng gốc/i)).not.toBeNull()
    expect(screen.getByText(/Cập nhật lần cuối/)).not.toBeNull()
  })

  it('có liên kết sang Điều khoản sử dụng và Bản quyền', () => {
    renderPage(<PrivacyPage />)

    expect(screen.getByRole('link', { name: 'Điều khoản sử dụng' }).getAttribute('href')).toBe('/dieu-khoan-su-dung')
    expect(screen.getByRole('link', { name: 'Bản quyền' }).getAttribute('href')).toBe('/ban-quyen')
  })
})

describe('TermsPage', () => {
  it('có các mục chính và nói đây là dự án học tập, nội dung chỉ để tham khảo', () => {
    renderPage(<TermsPage />)

    expect(screen.getByRole('heading', { level: 1, name: 'Điều khoản sử dụng' })).not.toBeNull()
    for (const muc of ['Phạm vi dịch vụ', 'Tài khoản', 'Quy tắc sử dụng', 'Giới hạn trách nhiệm']) {
      expect(screen.getByRole('heading', { level: 2, name: muc })).not.toBeNull()
    }
    expect(screen.getByText(/dự án học tập/i)).not.toBeNull()
  })

  it('mô tả đúng dịch vụ tự học trực tuyến của Trung tâm Tin học SPACE-X, không còn lớp 6 đến lớp 12', () => {
    const { container } = renderPage(<TermsPage />)

    expect(container.textContent).toContain('Trung tâm Tin học SPACE-X')
    expect(container.textContent).toMatch(/tự học trực tuyến/i)
    expect(container.textContent).not.toMatch(/lớp 6|lớp 12|THCS|THPT|GDPT/i)
    expect(screen.getByText(/Cập nhật lần cuối: 27\/11\/2026/)).not.toBeNull()
  })

  it('có mục Thanh toán thử nghiệm: chưa mở mua khóa, không thu tiền thật, không chuyển tiền thật', () => {
    renderPage(<TermsPage />)

    const section = screen.getByRole('heading', { level: 2, name: 'Thanh toán thử nghiệm' }).closest('section')!
    expect(section.textContent).toMatch(/chưa mở/i)
    expect(section.textContent).toMatch(/môi trường thử nghiệm/i)
    expect(section.textContent).toMatch(/không thu tiền thật/i)
    expect(section.textContent).toMatch(/đừng chuyển tiền thật/i)
  })

  it('có mục Hoàn tiền: quản trị viên xử lý thủ công, thu hồi quyền học và giữ tiến độ', () => {
    renderPage(<TermsPage />)

    const section = screen.getByRole('heading', { level: 2, name: 'Hoàn tiền' }).closest('section')!
    expect(section.textContent).toMatch(/quản trị viên xử lý thủ công/i)
    expect(section.textContent).toMatch(/thu hồi/i)
    expect(section.textContent).toMatch(/tiến độ học vẫn được giữ/i)
    expect(within(section).getByRole('link', { name: 'gửi yêu cầu' }).getAttribute('rel')).toContain('noopener')
  })
})

describe('CopyrightPage', () => {
  it('nói nguồn khung chương trình, nội dung soạn bằng lời mới và cách báo vi phạm', () => {
    renderPage(<CopyrightPage />)

    expect(screen.getByRole('heading', { level: 1, name: 'Bản quyền' })).not.toBeNull()
    expect(screen.getByText(/Thông tư 32\/2018\/TT-BGDĐT/)).not.toBeNull()
    expect(screen.getByText(/không sao chép sách giáo khoa/i)).not.toBeNull()
    const bao = screen.getByRole('region', { name: 'Báo vi phạm' })
    expect(within(bao).getByRole('link', { name: /Góp ý, báo lỗi/ }).getAttribute('rel')).toContain('noopener')
  })
})
