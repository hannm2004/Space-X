import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { FEEDBACK_URL } from '../links'

const UPDATED = '19/11/2026'

function Feedback({ children }: { children: ReactNode }) {
  return (
    <a href={FEEDBACK_URL} target="_blank" rel="noopener noreferrer" className="link">
      {children}
    </a>
  )
}

function PolicyPage({ title, intro, children }: { title: string; intro: ReactNode; children: ReactNode }) {
  return (
    <article className="mx-auto max-w-2xl">
      <p className="eyebrow">Thông tin pháp lý</p>
      <h1 className="mt-3 text-4xl font-bold leading-tight tracking-tight">{title}</h1>
      <p className="mt-3 text-sm text-muted">Cập nhật lần cuối: {UPDATED}</p>
      <p className="mt-6 text-lg">{intro}</p>
      {children}
    </article>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-10">
      <h2 className="border-t border-line pt-4 text-2xl font-bold tracking-tight">{title}</h2>
      <div className="mt-3 space-y-3 leading-[1.75]">{children}</div>
    </section>
  )
}

function Bullets({ items }: { items: ReactNode[] }) {
  return (
    <ul className="list-disc space-y-1.5 pl-6 marker:text-accent">
      {items.map((item, index) => (
        <li key={index}>{item}</li>
      ))}
    </ul>
  )
}

export function PrivacyPage() {
  return (
    <PolicyPage
      title="Chính sách bảo mật"
      intro="Trang này nói rõ Space-X lưu những dữ liệu nào của bạn, để làm gì và bạn có những quyền gì."
    >
      <Section title="Dữ liệu chúng tôi lưu">
        <Bullets
          items={[
            <>
              <strong>Tài khoản email và mật khẩu:</strong> tên hiển thị, email và mật khẩu. Chúng tôi không lưu mật khẩu
              dạng gốc, chỉ lưu giá trị băm một chiều.
            </>,
            <>
              <strong>Tài khoản Google:</strong> mã định danh tài khoản Google, email đã được Google xác minh và tên hiển
              thị.
            </>,
            <>
              <strong>Tiến độ học:</strong> bài bạn đã mở hoặc hoàn thành và thời điểm tương ứng.
            </>,
            <>
              <strong>Nhật ký kỹ thuật:</strong> nhà cung cấp máy chủ có thể ghi nhật ký truy cập thông thường (địa chỉ
              IP, thời gian, đường dẫn) để vận hành và bảo vệ hệ thống.
            </>,
          ]}
        />
        <p>Dữ liệu chỉ dùng để đăng nhập, hiển thị tên của bạn, lưu tiến độ học và quản trị tài khoản. Chúng tôi không bán hay chia sẻ dữ liệu cho bên khác để quảng cáo.</p>
      </Section>

      <Section title="Cookie và bộ nhớ trình duyệt">
        <Bullets
          items={[
            <>
              <code>SESSION</code>: giữ phiên đăng nhập của bạn (cookie HttpOnly, chỉ gửi qua HTTPS).
            </>,
            <>
              <code>XSRF-TOKEN</code>: chống giả mạo yêu cầu khi bạn gửi biểu mẫu.
            </>,
            <>
              <code>theme</code> (bộ nhớ trình duyệt): nhớ bạn chọn giao diện sáng hay tối.
            </>,
          ]}
        />
        <p>Space-X không có quảng cáo và không dùng công cụ phân tích hay theo dõi của bên thứ ba.</p>
      </Section>

      <Section title="Đăng nhập bằng Google">
        <p>
          Khi bạn chọn đăng nhập bằng Google, chúng tôi chỉ nhận tên và email đã xác minh của bạn. Chúng tôi không đọc thư,
          tệp hay dữ liệu nào khác trong tài khoản Google, và không đăng gì thay bạn. Nếu email đó đã đăng ký bằng mật khẩu,
          bạn cần đăng nhập bằng email và mật khẩu để tránh nhầm tài khoản.
        </p>
      </Section>

      <Section title="Lưu trữ và bảo vệ">
        <p>
          Dữ liệu nằm trong cơ sở dữ liệu của nhà cung cấp dịch vụ đám mây mà dự án dùng. Kết nối tới trang đi qua HTTPS. Quản
          trị viên xem được danh sách tài khoản (tên, email, trạng thái) để quản lý nhưng không xem được mật khẩu của bạn.
        </p>
      </Section>

      <Section title="Quyền của bạn">
        <Bullets
          items={[
            'Xem và sửa tên hiển thị ở trang Hồ sơ; đổi mật khẩu nếu bạn dùng tài khoản email.',
            'Xóa tiến độ của một khóa bằng nút Học lại.',
            <>
              Yêu cầu xóa tài khoản: hiện chưa có nút tự xóa, bạn hãy <Feedback>gửi yêu cầu</Feedback> và quản trị viên sẽ xử lý.
            </>,
          ]}
        />
      </Section>

      <Section title="Thay đổi chính sách">
        <p>
          Khi thêm tính năng mới có dùng dữ liệu của bạn, chính sách này sẽ được cập nhật trước. Xem thêm{' '}
          <Link to="/dieu-khoan-su-dung" className="link">
            Điều khoản sử dụng
          </Link>{' '}
          và{' '}
          <Link to="/ban-quyen" className="link">
            Bản quyền
          </Link>
          .
        </p>
      </Section>
    </PolicyPage>
  )
}

export function TermsPage() {
  return (
    <PolicyPage title="Điều khoản sử dụng" intro="Khi dùng Space-X, bạn đồng ý với các điều khoản ngắn gọn dưới đây.">
      <Section title="Phạm vi dịch vụ">
        <p>
          Space-X là dự án học tập cung cấp bài học Tin học lớp 6 đến lớp 12 để tự học và tham khảo, hiện miễn phí. Dịch vụ
          có thể thay đổi, gián đoạn hoặc ngừng mà không báo trước.
        </p>
      </Section>

      <Section title="Tài khoản">
        <Bullets
          items={[
            'Bạn chịu trách nhiệm giữ bí mật mật khẩu và mọi hoạt động trong tài khoản của mình.',
            'Cung cấp thông tin đúng; không dùng tên hoặc email của người khác.',
            'Quản trị viên có thể khóa tài khoản vi phạm điều khoản.',
          ]}
        />
      </Section>

      <Section title="Quy tắc sử dụng">
        <Bullets
          items={[
            'Không cố truy cập trái phép, dò mật khẩu hay làm gián đoạn hệ thống.',
            'Không dùng công cụ tự động để sao chép hàng loạt nội dung bài học.',
            'Không đăng hoặc gửi nội dung vi phạm pháp luật hay xâm phạm quyền người khác.',
          ]}
        />
      </Section>

      <Section title="Nội dung và bản quyền">
        <p>
          Nội dung bài học có thể dùng để học cá nhân. Quy định chi tiết về nguồn, hình ảnh và cách báo vi phạm ở trang{' '}
          <Link to="/ban-quyen" className="link">
            Bản quyền
          </Link>
          .
        </p>
      </Section>

      <Section title="Giới hạn trách nhiệm">
        <p>
          Chúng tôi cố gắng để nội dung chính xác nhưng không bảo đảm không có sai sót, và không thay thế sách giáo khoa hay
          bài giảng của giáo viên. Bạn tự chịu trách nhiệm khi dùng nội dung vào bài thi hay công việc. Thấy chỗ sai, hãy{' '}
          <Feedback>góp ý</Feedback>.
        </p>
      </Section>

      <Section title="Thay đổi điều khoản">
        <p>
          Điều khoản có thể được cập nhật; ngày cập nhật ở đầu trang. Việc dùng tiếp sau khi cập nhật được hiểu là bạn đồng ý
          bản mới. Việc xử lý dữ liệu cá nhân xem{' '}
          <Link to="/chinh-sach-bao-mat" className="link">
            Chính sách bảo mật
          </Link>
          .
        </p>
      </Section>
    </PolicyPage>
  )
}

export function CopyrightPage() {
  return (
    <PolicyPage title="Bản quyền" intro="Nguồn gốc nội dung của Space-X và cách bạn có thể dùng, báo lỗi hoặc báo vi phạm.">
      <Section title="Nguồn và cách soạn bài">
        <p>
          Bài học bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được
          soạn lại bằng lời mới, không sao chép sách giáo khoa. Tên môn, tên chương trình và tên các tổ chức được nhắc tới chỉ
          để chỉ nguồn tham khảo.
        </p>
      </Section>

      <Section title="Hình ảnh">
        <p>
          Hình minh họa và ảnh bìa do quản trị viên tải lên, và phải là ảnh mà quản trị viên có quyền sử dụng. Logo Google ở
          nút đăng nhập thuộc về Google theo hướng dẫn thương hiệu của họ.
        </p>
      </Section>

      <Section title="Bạn được dùng thế nào">
        <p>
          Bạn có thể đọc, in và dùng nội dung để học cá nhân hoặc giảng dạy trên lớp. Khi trích dẫn, hãy ghi nguồn Space-X.
          Không sao chép hàng loạt để xuất bản lại hoặc bán.
        </p>
      </Section>

      <section aria-labelledby="report-title" className="mt-10">
        <h2 id="report-title" className="border-t border-line pt-4 text-2xl font-bold tracking-tight">
          Báo vi phạm
        </h2>
        <div className="mt-3 space-y-3 leading-[1.75]">
          <p>
            Nếu bạn cho rằng nội dung hay hình ảnh trên Space-X xâm phạm quyền của mình, hãy mô tả rõ nội dung đó và đường dẫn
            của bài. Chúng tôi sẽ xem xét và gỡ hoặc sửa nếu cần.
          </p>
          <p>
            <Feedback>Góp ý, báo lỗi</Feedback> · Xem thêm{' '}
            <Link to="/dieu-khoan-su-dung" className="link">
              Điều khoản sử dụng
            </Link>
            .
          </p>
        </div>
      </section>
    </PolicyPage>
  )
}
