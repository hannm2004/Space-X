import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { FEEDBACK_URL } from '../links'

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

export default function AboutPage() {
  return (
    <article className="mx-auto max-w-2xl">
      <p className="eyebrow">Về dự án</p>
      <h1 className="mt-3 text-4xl font-bold leading-tight tracking-tight">Giới thiệu Space-X</h1>
      <p className="mt-6 text-lg">
        Space-X là website học Tin học từ lớp 6 đến lớp 12 bằng tiếng Việt, theo Chương trình GDPT 2018. Tên Space-X
        chỉ là tên dự án, không liên quan đến vũ trụ.
      </p>
      <p className="mt-4">
        <Link to="/#khoa-hoc" className="btn btn-primary">
          Xem các khóa học
        </Link>
      </p>

      <Section title="Space-X dành cho ai">
        <p>
          Học sinh trung học cơ sở và trung học phổ thông muốn ôn và học Tin học theo từng lớp, ở nhà hoặc trên điện
          thoại. Giáo viên cũng có thể dùng để tham khảo cách chia bài.
        </p>
        <p>
          Đây là một đồ án chuyên ngành về công nghệ thông tin, được làm để học và thực hành xây dựng một sản phẩm web
          hoàn chỉnh. Vì vậy trang còn đang được bổ sung dần.
        </p>
      </Section>

      <Section title="Nội dung học">
        <p>
          Nội dung chia hai tầng: mỗi lớp là một khóa học, mỗi khóa gồm nhiều bài học. Bài học được tự biên soạn theo
          chương trình, không sao chép sách giáo khoa hay trang web khác.
        </p>
      </Section>

      <Section title="Bạn làm được gì">
        <Bullets
          items={[
            'Đăng ký miễn phí bằng email hoặc đăng nhập bằng tài khoản Google.',
            'Đọc bài học có bảng, ảnh minh họa; tìm bài theo từ khóa.',
            'Đánh dấu bài đã hoàn thành và xem phần trăm hoàn thành của từng khóa; tiến độ chỉ của riêng bạn.',
            'Đổi giao diện sáng hoặc tối.',
          ]}
        />
      </Section>

      <Section title="Đang phát triển">
        <p>Hai tính năng sau chưa có trên bản hiện tại:</p>
        <Bullets
          items={[
            'Chatbot hỏi đáp: trả lời dựa trên nội dung bài học và ghi rõ trích dẫn từ bài nào.',
            'Trợ lý AI gợi ý bài nên học tiếp, chỉ đọc dữ liệu bài học chứ không sửa gì.',
          ]}
        />
      </Section>

      <Section title="Góp ý và báo lỗi">
        <p>
          Thấy lỗi hoặc có ý kiến, hãy tạo một mục mới tại{' '}
          <a href={FEEDBACK_URL} target="_blank" rel="noopener noreferrer" className="link">
            trang Issues trên GitHub
          </a>
          . Xem thêm{' '}
          <Link to="/chinh-sach-bao-mat" className="link">
            Chính sách bảo mật
          </Link>{' '}
          để biết dữ liệu nào được lưu.
        </p>
      </Section>
    </article>
  )
}
