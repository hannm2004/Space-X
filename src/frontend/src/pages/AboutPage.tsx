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
        Trung tâm Tin học SPACE-X là nền tảng tự học Tin học trực tuyến bằng tiếng Việt. Đây là một trung tâm hư cấu;
        Space-X chỉ là tên dự án, không liên quan đến vũ trụ.
      </p>
      <p className="mt-4">
        <Link to="/#khoa-hoc" className="btn btn-primary">
          Xem các khóa học
        </Link>
      </p>

      <Section title="Space-X dành cho ai">
        <p>
          Người muốn tự học Tin học theo tốc độ của mình, trên máy tính hoặc điện thoại. Bạn chọn khóa rồi học lúc nào
          thuận tiện, không phải theo lịch cố định.
        </p>
        <p>
          Đây là một đồ án chuyên ngành về công nghệ thông tin, chạy ở môi trường thử nghiệm và không thu tiền thật. Vì
          vậy trang còn đang được bổ sung dần.
        </p>
      </Section>

      <Section title="Nội dung học">
        <p>
          Khóa học được xếp theo nhóm chương trình, mỗi khóa gồm nhiều bài học. Bài học được soạn bằng lời mới, không sao
          chép sách giáo khoa hay trang web khác.
        </p>
      </Section>

      <Section title="Bạn làm được gì">
        <Bullets
          items={[
            'Xem danh sách khóa học và lọc theo chương trình, trình độ, thời lượng, học phí.',
            'Tạo tài khoản bằng email hoặc đăng nhập bằng tài khoản Google.',
            'Đọc bài học có bảng, ảnh minh họa và tìm bài theo từ khóa sau khi đăng nhập.',
            'Đánh dấu bài đã hoàn thành và xem phần trăm hoàn thành của từng khóa; tiến độ chỉ của riêng bạn.',
            'Đổi giao diện sáng hoặc tối.',
          ]}
        />
      </Section>

      <Section title="Đang phát triển">
        <p>Các tính năng sau chưa có trên bản hiện tại:</p>
        <Bullets
          items={[
            'Mua khóa trả phí (chỉ thanh toán thử nghiệm) và mục "Khóa học của tôi".',
            'Chatbot hỏi đáp: trả lời dựa trên thông tin khóa học và bài học bạn được xem, ghi rõ trích dẫn.',
            'Trợ lý AI gợi ý khóa và bài phù hợp, chỉ đọc dữ liệu chứ không sửa gì.',
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
