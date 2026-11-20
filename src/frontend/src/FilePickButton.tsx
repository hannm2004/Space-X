import { useRef, type ChangeEvent } from 'react'
import { PRIMARY } from './buttons'

type Props = {
  /** Chữ trên nút, ví dụ "Chọn ảnh từ máy". */
  label: string
  /** Tên của ô nhập tệp cho trình đọc màn hình (người thấy chỉ thấy nút). */
  inputLabel: string
  /** Tên đầy đủ của nút khi nhiều nút cùng nhãn nằm trên một trang (ví dụ "Tải ảnh bìa Tin học 10"). */
  ariaLabel?: string
  /** Loại tệp cho phép, cùng dạng với thuộc tính accept của <input type="file">. */
  accept: string
  disabled?: boolean
  onFile: (file: File) => void
}

// Ô chọn tệp mặc định của trình duyệt ("Choose File / No file chosen") không theo được bộ nút của trang và khó thấy.
// Ở đây ô nhập bị ẩn đi, người dùng bấm một nút bình thường để mở hộp chọn tệp.
export default function FilePickButton({ label, inputLabel, ariaLabel, accept, disabled = false, onFile }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const input = event.target
    const file = input.files?.[0]
    // Xóa lựa chọn để lần sau chọn lại đúng tệp này vẫn kích hoạt sự kiện change.
    input.value = ''
    if (file) onFile(file)
  }

  return (
    <>
      <button
        type="button"
        aria-label={ariaLabel}
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        className={`${PRIMARY} btn-sm`}
      >
        {label}
      </button>
      {/* sr-only: ẩn khỏi mắt nhưng vẫn là ô nhập thật; tabIndex -1 để Tab dừng ở nút chứ không dừng hai lần. */}
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        aria-label={inputLabel}
        tabIndex={-1}
        disabled={disabled}
        onChange={handleChange}
        className="sr-only"
      />
    </>
  )
}
