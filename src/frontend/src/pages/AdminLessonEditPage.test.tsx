import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createLesson,
  deleteImage,
  setImageCover,
  clearImageCover,
  fetchAdminCourses,
  fetchAdminLesson,
  fetchLessonImages,
  setLessonStatus,
  updateLesson,
  uploadLessonImage,
  type AdminCourse,
  type AdminLesson,
  type LessonImage,
} from '../adminApi'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser } from '../authApi'
import { ApiError } from '../client'
import AdminLessonEditPage from './AdminLessonEditPage'

vi.mock('../adminApi', () => ({
  fetchAdminCourses: vi.fn(),
  fetchAdminLesson: vi.fn(),
  createLesson: vi.fn(),
  updateLesson: vi.fn(),
  setLessonStatus: vi.fn(),
  fetchLessonImages: vi.fn(),
  uploadLessonImage: vi.fn(),
  deleteImage: vi.fn(),
  setImageCover: vi.fn(),
  clearImageCover: vi.fn(),
}))
vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchAdminCoursesMock = vi.mocked(fetchAdminCourses)
const fetchAdminLessonMock = vi.mocked(fetchAdminLesson)
const createLessonMock = vi.mocked(createLesson)
const updateLessonMock = vi.mocked(updateLesson)
const setLessonStatusMock = vi.mocked(setLessonStatus)
const fetchLessonImagesMock = vi.mocked(fetchLessonImages)
const uploadLessonImageMock = vi.mocked(uploadLessonImage)
const deleteImageMock = vi.mocked(deleteImage)
const setImageCoverMock = vi.mocked(setImageCover)
const clearImageCoverMock = vi.mocked(clearImageCover)
const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)

const ADMIN_ME = { displayName: 'Quản trị viên', email: 'admin@example.com', role: 'ADMIN' as const }
const LEARNER_ME = { displayName: 'Học sinh', email: 'hs@example.com', role: 'LEARNER' as const }

const KHOA: AdminCourse[] = [
  { id: 5, slug: 'tin-hoc-10', title: 'Tin học 10', description: null, sortOrder: 1 },
  { id: 6, slug: 'tin-hoc-11', title: 'Tin học 11', description: null, sortOrder: 2 },
]
const BAI: AdminLesson = {
  id: 3,
  courseId: 5,
  slug: 'may-tinh-la-gi',
  title: 'Máy tính là gì',
  summary: 'Tóm tắt bài',
  status: 'DRAFT',
  sortOrder: 2,
  contentMd: '# Máy tính\n\nNội dung',
}
const ANH: LessonImage = { id: 7, fileName: 'so-do.png', contentType: 'image/png', sizeBytes: 2048, cover: false, url: '/api/images/7' }

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

function renderPage(entry: string) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <AuthProvider>
        <Routes>
          <Route path="/quan-tri/bai-hoc/moi" element={<AdminLessonEditPage />} />
          <Route path="/quan-tri/bai-hoc/:lessonId/sua" element={<AdminLessonEditPage />} />
          <Route path="/dang-nhap" element={<LoginStub />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

const EDIT = '/quan-tri/bai-hoc/3/sua'
const CREATE = '/quan-tri/bai-hoc/moi'

function field(label: string) {
  return screen.getByLabelText(label) as HTMLInputElement
}

function change(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

async function openEdit() {
  renderPage(EDIT)
  await screen.findByRole('heading', { level: 1, name: 'Sửa bài học' })
}

async function openCreate() {
  renderPage(CREATE)
  await screen.findByRole('heading', { level: 1, name: 'Thêm bài học' })
}

function pngFile(size = 10, name = 'a.png', type = 'image/png') {
  return new File([new Uint8Array(size)], name, { type })
}

function chooseFile(file: File) {
  fireEvent.change(screen.getByLabelText(/Tải ảnh lên/), { target: { files: [file] } })
}

beforeEach(() => {
  for (const mock of [
    fetchAdminCoursesMock,
    fetchAdminLessonMock,
    createLessonMock,
    updateLessonMock,
    setLessonStatusMock,
    fetchLessonImagesMock,
    uploadLessonImageMock,
    deleteImageMock,
    fetchCurrentUserMock,
  ]) {
    mock.mockReset()
  }
  fetchCurrentUserMock.mockResolvedValue(ADMIN_ME)
  fetchAdminCoursesMock.mockResolvedValue(KHOA)
  fetchAdminLessonMock.mockResolvedValue(BAI)
  fetchLessonImagesMock.mockResolvedValue([ANH])
})

describe('AdminLessonEditPage — quyền truy cập', () => {
  it('chưa đăng nhập thì chuyển sang trang đăng nhập, nhớ để quay lại và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(null)

    renderPage(EDIT)

    expect(await screen.findByText('Trang đăng nhập, quay lại /quan-tri/bai-hoc/3/sua')).not.toBeNull()
    expect(fetchAdminLessonMock).not.toHaveBeenCalled()
  })

  it('học sinh đăng nhập thì báo không có quyền và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(LEARNER_ME)

    renderPage(EDIT)

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(fetchAdminLessonMock).not.toHaveBeenCalled()
  })

  it('máy chủ trả 403 khi tải bài thì hiện câu không có quyền', async () => {
    fetchAdminLessonMock.mockRejectedValue(new ApiError(403, 'Bạn không có quyền thực hiện thao tác này'))

    renderPage(EDIT)

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
  })
})

describe('AdminLessonEditPage — thêm bài mới', () => {
  it('form trống, có danh sách khóa, và chưa cho thêm ảnh (cần lưu bài trước)', async () => {
    await openCreate()

    expect(field('Slug').value).toBe('')
    expect(screen.getByRole('option', { name: 'Tin học 10' })).not.toBeNull()
    expect(screen.getByRole('option', { name: 'Tin học 11' })).not.toBeNull()
    expect(screen.getByText(/Lưu bài trước/)).not.toBeNull()
    expect(screen.queryByLabelText(/Tải ảnh lên/)).toBeNull()
    expect(fetchAdminLessonMock).not.toHaveBeenCalled()
  })

  it('lưu thì gửi đúng dữ liệu (không cắt khoảng trắng của nội dung), chuyển sang trang sửa và báo đã tạo', async () => {
    createLessonMock.mockResolvedValue({ ...BAI, id: 9 })
    fetchAdminLessonMock.mockResolvedValue({ ...BAI, id: 9 })
    await openCreate()

    change('Khóa học', '5')
    change('Slug', ' may-tinh-la-gi ')
    change('Tên bài học', ' Máy tính là gì ')
    change('Tóm tắt (không bắt buộc)', '')
    change('Thứ tự hiển thị', '2')
    change('Nội dung (Markdown)', '  # Mở đầu\n')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bài' }))

    await waitFor(() =>
      expect(createLessonMock).toHaveBeenCalledWith({
        courseId: 5,
        slug: 'may-tinh-la-gi',
        title: 'Máy tính là gì',
        summary: '',
        contentMd: '  # Mở đầu\n',
        sortOrder: 2,
      }),
    )
    expect(await screen.findByRole('heading', { level: 1, name: 'Sửa bài học' })).not.toBeNull()
    expect(fetchAdminLessonMock).toHaveBeenCalledWith(9)
    expect((await screen.findByRole('status')).textContent).toContain('Đã tạo bài học')
  })

  it('thiếu khóa, slug hoặc tên thì báo lỗi và không gọi API', async () => {
    await openCreate()

    change('Thứ tự hiển thị', '1')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bài' }))

    const alert = await screen.findByRole('alert')
    expect(alert.textContent).toContain('khóa học')
    expect(alert.textContent).toContain('slug')
    expect(createLessonMock).not.toHaveBeenCalled()
  })

  it('thứ tự không phải số nguyên không âm thì báo lỗi và không gọi API', async () => {
    await openCreate()

    change('Khóa học', '5')
    change('Slug', 'bai-moi')
    change('Tên bài học', 'Bài mới')
    for (const bad of ['abc', '-1', '1.5', '']) {
      change('Thứ tự hiển thị', bad)
      fireEvent.click(screen.getByRole('button', { name: 'Lưu bài' }))
      expect((await screen.findByRole('alert')).textContent).toContain('Thứ tự')
    }
    expect(createLessonMock).not.toHaveBeenCalled()
  })

  it('slug trùng (409) thì hiện đúng câu của máy chủ và giữ nguyên chữ đã nhập', async () => {
    createLessonMock.mockRejectedValue(
      new ApiError(409, 'Slug bài học đã tồn tại', { detail: 'Slug bài học đã tồn tại' }),
    )
    await openCreate()

    change('Khóa học', '5')
    change('Slug', 'bai-moi')
    change('Tên bài học', 'Bài mới')
    change('Thứ tự hiển thị', '1')
    change('Nội dung (Markdown)', 'Viết dở')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bài' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Slug bài học đã tồn tại')
    expect(field('Nội dung (Markdown)').value).toBe('Viết dở')
  })
})

describe('AdminLessonEditPage — sửa bài', () => {
  it('tải bài thì form có sẵn dữ liệu và hiện trạng thái Nháp', async () => {
    await openEdit()

    expect(fetchAdminLessonMock).toHaveBeenCalledWith(3)
    expect(field('Khóa học').value).toBe('5')
    expect(field('Slug').value).toBe('may-tinh-la-gi')
    expect(field('Tên bài học').value).toBe('Máy tính là gì')
    expect(field('Tóm tắt (không bắt buộc)').value).toBe('Tóm tắt bài')
    expect(field('Thứ tự hiển thị').value).toBe('2')
    expect(field('Nội dung (Markdown)').value).toBe('# Máy tính\n\nNội dung')
    expect(screen.getByText('Nháp')).not.toBeNull()
  })

  it('lưu thì gọi updateLesson đúng id và báo đã lưu', async () => {
    updateLessonMock.mockResolvedValue(BAI)
    await openEdit()

    change('Tên bài học', 'Máy tính là gì (sửa)')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bài' }))

    await waitFor(() =>
      expect(updateLessonMock).toHaveBeenCalledWith(3, {
        courseId: 5,
        slug: 'may-tinh-la-gi',
        title: 'Máy tính là gì (sửa)',
        summary: 'Tóm tắt bài',
        contentMd: '# Máy tính\n\nNội dung',
        sortOrder: 2,
      }),
    )
    expect((await screen.findByRole('status')).textContent).toContain('Đã lưu bài học')
  })

  it('lỗi 400 theo trường thì hiện câu của máy chủ', async () => {
    updateLessonMock.mockRejectedValue(
      new ApiError(400, 'Dữ liệu không hợp lệ', {
        fieldErrors: [{ field: 'contentMd', message: 'Nội dung tối đa 100000 ký tự' }],
      }),
    )
    await openEdit()

    fireEvent.click(screen.getByRole('button', { name: 'Lưu bài' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Nội dung tối đa 100000 ký tự')
  })

  it('Đăng bài thì gọi API và trạng thái đổi sang Đã đăng với nút Ẩn bài', async () => {
    setLessonStatusMock.mockResolvedValue({ ...BAI, status: 'PUBLISHED' })
    await openEdit()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng bài' }))

    await waitFor(() => expect(setLessonStatusMock).toHaveBeenCalledWith(3, 'PUBLISHED'))
    expect(await screen.findByText('Đã đăng')).not.toBeNull()
    expect(screen.getByRole('button', { name: 'Ẩn bài' })).not.toBeNull()
  })

  it('bài không tồn tại (404) thì báo không tìm thấy', async () => {
    fetchAdminLessonMock.mockRejectedValue(new ApiError(404, 'Không tìm thấy bài học'))

    renderPage('/quan-tri/bai-hoc/99/sua')

    expect(await screen.findByText('Không tìm thấy bài học.')).not.toBeNull()
  })

  it('địa chỉ có id không phải số thì báo không tìm thấy và không gọi API', async () => {
    renderPage('/quan-tri/bai-hoc/abc/sua')

    expect(await screen.findByText('Không tìm thấy bài học.')).not.toBeNull()
    expect(fetchAdminLessonMock).not.toHaveBeenCalled()
  })
})

describe('AdminLessonEditPage — xem trước Markdown', () => {
  it('Xem trước hiện nội dung đã dựng và ẩn ô soạn; Soạn quay lại ô nhập', async () => {
    await openEdit()

    fireEvent.click(screen.getByRole('button', { name: 'Xem trước' }))
    expect(screen.getByRole('heading', { name: 'Máy tính' })).not.toBeNull()
    expect(screen.queryByLabelText('Nội dung (Markdown)')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Soạn' }))
    expect(field('Nội dung (Markdown)').value).toBe('# Máy tính\n\nNội dung')
  })
})

describe('AdminLessonEditPage — ảnh bài học', () => {
  it('liệt kê ảnh của bài kèm tên và dung lượng', async () => {
    await openEdit()

    expect(fetchLessonImagesMock).toHaveBeenCalledWith(3)
    const item = (await screen.findByText('so-do.png')).closest('li') as HTMLElement
    expect(within(item).getByText('2 KB')).not.toBeNull()
  })

  it('chưa có ảnh thì báo rõ', async () => {
    fetchLessonImagesMock.mockResolvedValue([])

    await openEdit()

    expect(await screen.findByText('Bài này chưa có ảnh.')).not.toBeNull()
  })

  it('Chèn vào bài thêm cú pháp ảnh Markdown vào cuối nội dung', async () => {
    await openEdit()

    fireEvent.click(await screen.findByRole('button', { name: 'Chèn ảnh so-do.png vào bài' }))

    expect(field('Nội dung (Markdown)').value).toBe('# Máy tính\n\nNội dung\n\n![so-do](/api/images/7)\n')
  })

  it('tải ảnh hợp lệ thì gọi API, tải lại danh sách và báo đã tải', async () => {
    uploadLessonImageMock.mockResolvedValue({ id: 8, url: '/api/images/8' })
    await openEdit()
    await screen.findByText('so-do.png')

    const file = pngFile()
    chooseFile(file)

    await waitFor(() => expect(uploadLessonImageMock).toHaveBeenCalledWith(3, file))
    await waitFor(() => expect(fetchLessonImagesMock).toHaveBeenCalledTimes(2))
    expect(await screen.findByText(/Đã tải ảnh/)).not.toBeNull()
  })

  it('tệp không phải PNG/JPEG/WebP thì báo lỗi và không gọi API', async () => {
    await openEdit()
    await screen.findByText('so-do.png')

    chooseFile(pngFile(10, 'a.gif', 'image/gif'))

    expect((await screen.findByText(/PNG, JPEG hoặc WebP/, { selector: '[role="alert"] *' })).textContent).toContain(
      'PNG, JPEG hoặc WebP',
    )
    expect(uploadLessonImageMock).not.toHaveBeenCalled()
  })

  it('tệp lớn hơn 1 MB thì báo lỗi và không gọi API', async () => {
    await openEdit()
    await screen.findByText('so-do.png')

    chooseFile(pngFile(1024 * 1024 + 1))

    expect((await screen.findByRole('alert')).textContent).toContain('1 MB')
    expect(uploadLessonImageMock).not.toHaveBeenCalled()
  })

  it('máy chủ từ chối ảnh (415) thì hiện câu của máy chủ', async () => {
    uploadLessonImageMock.mockRejectedValue(
      new ApiError(415, 'Tệp không phải ảnh hợp lệ', { detail: 'Tệp không phải ảnh hợp lệ' }),
    )
    await openEdit()
    await screen.findByText('so-do.png')

    chooseFile(pngFile())

    expect((await screen.findByRole('alert')).textContent).toContain('Tệp không phải ảnh hợp lệ')
  })

  it('xóa ảnh phải xác nhận: Xác nhận xóa ảnh mới gọi API rồi tải lại danh sách', async () => {
    deleteImageMock.mockResolvedValue(undefined)
    await openEdit()

    const item = within((await screen.findByText('so-do.png')).closest('li') as HTMLElement)
    fireEvent.click(item.getByRole('button', { name: 'Xóa ảnh so-do.png' }))
    expect(deleteImageMock).not.toHaveBeenCalled()
    fireEvent.click(item.getByRole('button', { name: 'Xác nhận xóa ảnh' }))

    await waitFor(() => expect(deleteImageMock).toHaveBeenCalledWith(7))
    await waitFor(() => expect(fetchLessonImagesMock).toHaveBeenCalledTimes(2))
  })

  it('Hủy ở bước xác nhận thì không xóa ảnh', async () => {
    await openEdit()

    const item = within((await screen.findByText('so-do.png')).closest('li') as HTMLElement)
    fireEvent.click(item.getByRole('button', { name: 'Xóa ảnh so-do.png' }))
    fireEvent.click(item.getByRole('button', { name: 'Hủy' }))

    expect(deleteImageMock).not.toHaveBeenCalled()
  })
})

describe('AdminLessonEditPage — ảnh bìa', () => {
  it('ảnh chưa là ảnh bìa có nút "Đặt làm ảnh bìa"; bấm thì gọi API rồi tải lại danh sách', async () => {
    setImageCoverMock.mockResolvedValue(undefined)
    await openEdit()

    const item = within((await screen.findByText('so-do.png')).closest('li') as HTMLElement)
    expect(item.queryByText('Ảnh bìa')).toBeNull()
    fireEvent.click(item.getByRole('button', { name: 'Đặt ảnh so-do.png làm ảnh bìa' }))

    await waitFor(() => expect(setImageCoverMock).toHaveBeenCalledWith(7))
    await waitFor(() => expect(fetchLessonImagesMock).toHaveBeenCalledTimes(2))
  })

  it('ảnh đang là ảnh bìa có nhãn "Ảnh bìa" và nút "Bỏ ảnh bìa"; bấm thì gọi API', async () => {
    fetchLessonImagesMock.mockResolvedValue([{ ...ANH, cover: true }])
    clearImageCoverMock.mockResolvedValue(undefined)
    await openEdit()

    const item = within((await screen.findByText('so-do.png')).closest('li') as HTMLElement)
    expect(item.getByText('Ảnh bìa')).not.toBeNull()
    expect(item.queryByRole('button', { name: 'Đặt ảnh so-do.png làm ảnh bìa' })).toBeNull()
    fireEvent.click(item.getByRole('button', { name: 'Bỏ ảnh bìa của so-do.png' }))

    await waitFor(() => expect(clearImageCoverMock).toHaveBeenCalledWith(7))
  })

  it('đặt ảnh bìa lỗi thì báo câu cố định và không tải lại danh sách', async () => {
    setImageCoverMock.mockRejectedValue(new Error('x'))
    await openEdit()

    const item = within((await screen.findByText('so-do.png')).closest('li') as HTMLElement)
    fireEvent.click(item.getByRole('button', { name: 'Đặt ảnh so-do.png làm ảnh bìa' }))

    expect((await item.findByRole('alert')).textContent).toContain('Không đặt được ảnh bìa')
    expect(fetchLessonImagesMock).toHaveBeenCalledTimes(1)
  })
})
