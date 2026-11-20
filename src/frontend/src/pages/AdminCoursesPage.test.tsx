import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createCourse,
  deleteCourse,
  deleteCourseCover,
  fetchAdminCourses,
  updateCourse,
  uploadCourseCover,
  type AdminCourse,
} from '../adminApi'
import { AuthProvider } from '../AuthContext'
import { fetchCurrentUser } from '../authApi'
import { ApiError } from '../client'
import AdminCoursesPage from './AdminCoursesPage'

vi.mock('../adminApi', () => ({
  fetchAdminCourses: vi.fn(),
  createCourse: vi.fn(),
  updateCourse: vi.fn(),
  deleteCourse: vi.fn(),
  uploadCourseCover: vi.fn(),
  deleteCourseCover: vi.fn(),
}))
vi.mock('../authApi', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
}))

const fetchAdminCoursesMock = vi.mocked(fetchAdminCourses)
const createCourseMock = vi.mocked(createCourse)
const updateCourseMock = vi.mocked(updateCourse)
const deleteCourseMock = vi.mocked(deleteCourse)
const uploadCourseCoverMock = vi.mocked(uploadCourseCover)
const deleteCourseCoverMock = vi.mocked(deleteCourseCover)
const fetchCurrentUserMock = vi.mocked(fetchCurrentUser)

const ADMIN_ME = { displayName: 'Quản trị viên', email: 'admin@example.com', role: 'ADMIN' as const }
const LEARNER_ME = { displayName: 'Học sinh', email: 'hs@example.com', role: 'LEARNER' as const }

const KHOA_10: AdminCourse = {
  id: 5,
  slug: 'tin-hoc-10',
  title: 'Tin học 10',
  description: 'Chương trình lớp 10',
  sortOrder: 1,
}
const KHOA_11: AdminCourse = { id: 6, slug: 'tin-hoc-11', title: 'Tin học 11', description: null, sortOrder: 2 }

function LoginStub() {
  const state = useLocation().state as { from?: string } | null
  return <p>Trang đăng nhập, quay lại {state?.from}</p>
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/quan-tri/khoa-hoc']}>
      <AuthProvider>
        <Routes>
          <Route path="/quan-tri/khoa-hoc" element={<AdminCoursesPage />} />
          <Route path="/dang-nhap" element={<LoginStub />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function rowOf(title: string) {
  const cell = await screen.findByText(title)
  return within(cell.closest('li') as HTMLElement)
}

function addForm() {
  return within(screen.getByRole('region', { name: 'Thêm khóa học' }))
}

function type(scope: ReturnType<typeof within>, label: string, value: string) {
  fireEvent.change(scope.getByLabelText(label), { target: { value } })
}

function fillAddForm(slug: string, title: string, description: string, sortOrder: string) {
  const form = addForm()
  type(form, 'Slug', slug)
  type(form, 'Tên khóa học', title)
  type(form, 'Mô tả (không bắt buộc)', description)
  type(form, 'Thứ tự hiển thị', sortOrder)
  return form
}

beforeEach(() => {
  fetchAdminCoursesMock.mockReset()
  createCourseMock.mockReset()
  updateCourseMock.mockReset()
  deleteCourseMock.mockReset()
  uploadCourseCoverMock.mockReset()
  deleteCourseCoverMock.mockReset()
  fetchCurrentUserMock.mockReset()
  fetchCurrentUserMock.mockResolvedValue(ADMIN_ME)
  fetchAdminCoursesMock.mockResolvedValue([KHOA_10, KHOA_11])
})

const KHOA_10_BIA: AdminCourse = { ...KHOA_10, coverImageUrl: '/api/courses/tin-hoc-10/cover?v=1' }

function pngFile(name = 'bia.png') {
  return new File(['x'], name, { type: 'image/png' })
}

describe('AdminCoursesPage — ảnh bìa khóa học', () => {
  it('khóa có ảnh hiện ảnh thật cùng nút Đổi và Bỏ; khóa chưa có hiện hình mặc định cùng nút Tải', async () => {
    fetchAdminCoursesMock.mockResolvedValue([KHOA_10_BIA, KHOA_11])

    renderPage()

    const co = await rowOf('Tin học 10')
    expect(co.getByTestId('course-cover').querySelector('img')?.getAttribute('src')).toBe(
      '/api/courses/tin-hoc-10/cover?v=1',
    )
    expect(co.getByRole('button', { name: 'Đổi ảnh bìa Tin học 10' })).not.toBeNull()
    expect(co.getByRole('button', { name: 'Bỏ ảnh bìa Tin học 10' })).not.toBeNull()
    const chua = await rowOf('Tin học 11')
    expect(chua.getByTestId('course-cover').getAttribute('data-kind')).toBe('generated')
    expect(chua.getByRole('button', { name: 'Tải ảnh bìa Tin học 11' })).not.toBeNull()
    expect(chua.queryByRole('button', { name: 'Bỏ ảnh bìa Tin học 11' })).toBeNull()
  })

  it('chọn ảnh thì tải lên đúng khóa, báo đã đặt và hiện ảnh mới sau khi tải lại danh sách', async () => {
    uploadCourseCoverMock.mockResolvedValue({ coverImageUrl: '/api/courses/tin-hoc-11/cover?v=2' })
    fetchAdminCoursesMock
      .mockResolvedValueOnce([KHOA_10, KHOA_11])
      .mockResolvedValue([KHOA_10, { ...KHOA_11, coverImageUrl: '/api/courses/tin-hoc-11/cover?v=2' }])
    renderPage()
    const file = pngFile()

    const row = await rowOf('Tin học 11')
    fireEvent.change(row.getByLabelText(/Tệp ảnh bìa Tin học 11/), { target: { files: [file] } })

    await waitFor(() => expect(uploadCourseCoverMock).toHaveBeenCalledWith(6, file))
    expect((await screen.findByRole('status')).textContent).toContain('Đã đặt ảnh bìa')
    const sau = await rowOf('Tin học 11')
    await waitFor(() =>
      expect(sau.getByTestId('course-cover').querySelector('img')?.getAttribute('src')).toBe(
        '/api/courses/tin-hoc-11/cover?v=2',
      ),
    )
  })

  it('tệp không phải ảnh hoặc lớn hơn 1 MB thì báo ngay và không gọi máy chủ', async () => {
    renderPage()
    const row = await rowOf('Tin học 10')
    const input = row.getByLabelText(/Tệp ảnh bìa Tin học 10/)

    fireEvent.change(input, { target: { files: [new File(['x'], 'a.txt', { type: 'text/plain' })] } })
    expect((await row.findByRole('alert')).textContent).toContain('Chỉ nhận ảnh PNG, JPEG hoặc WebP.')

    const lon = new File([new Uint8Array(1024 * 1024 + 1)], 'lon.png', { type: 'image/png' })
    fireEvent.change(input, { target: { files: [lon] } })
    await waitFor(() => expect(row.getByRole('alert').textContent).toContain('Ảnh lớn hơn 1 MB'))

    expect(uploadCourseCoverMock).not.toHaveBeenCalled()
  })

  it('tải ảnh lỗi thì báo lỗi ở đúng khóa và không báo thành công', async () => {
    uploadCourseCoverMock.mockRejectedValue(new Error('hỏng'))
    renderPage()

    const row = await rowOf('Tin học 10')
    fireEvent.change(row.getByLabelText(/Tệp ảnh bìa Tin học 10/), { target: { files: [pngFile()] } })

    expect((await row.findByRole('alert')).textContent).toContain('Không đặt được ảnh bìa')
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('Bỏ ảnh bìa phải xác nhận: bấm lần đầu chỉ hỏi lại, chưa gọi máy chủ', async () => {
    fetchAdminCoursesMock.mockResolvedValue([KHOA_10_BIA, KHOA_11])
    renderPage()

    const row = await rowOf('Tin học 10')
    fireEvent.click(row.getByRole('button', { name: 'Bỏ ảnh bìa Tin học 10' }))

    expect(row.getByText(/Bỏ ảnh bìa của khóa "Tin học 10"\?/)).not.toBeNull()
    expect(row.getByRole('button', { name: 'Xác nhận bỏ ảnh bìa' })).not.toBeNull()
    expect(deleteCourseCoverMock).not.toHaveBeenCalled()
  })

  it('Hủy ở bước xác nhận thì giữ nguyên ảnh bìa và không gọi máy chủ', async () => {
    fetchAdminCoursesMock.mockResolvedValue([KHOA_10_BIA, KHOA_11])
    renderPage()

    const row = await rowOf('Tin học 10')
    fireEvent.click(row.getByRole('button', { name: 'Bỏ ảnh bìa Tin học 10' }))
    fireEvent.click(row.getByRole('button', { name: 'Hủy' }))

    expect(row.queryByRole('button', { name: 'Xác nhận bỏ ảnh bìa' })).toBeNull()
    expect(row.getByRole('button', { name: 'Bỏ ảnh bìa Tin học 10' })).not.toBeNull()
    expect(row.getByTestId('course-cover').getAttribute('data-kind')).toBe('image')
    expect(deleteCourseCoverMock).not.toHaveBeenCalled()
  })

  it('Xác nhận bỏ ảnh bìa gọi máy chủ đúng khóa, báo đã bỏ và tải lại danh sách', async () => {
    deleteCourseCoverMock.mockResolvedValue(undefined)
    fetchAdminCoursesMock.mockResolvedValueOnce([KHOA_10_BIA, KHOA_11]).mockResolvedValue([KHOA_10, KHOA_11])
    renderPage()

    const row = await rowOf('Tin học 10')
    fireEvent.click(row.getByRole('button', { name: 'Bỏ ảnh bìa Tin học 10' }))
    fireEvent.click(row.getByRole('button', { name: 'Xác nhận bỏ ảnh bìa' }))

    await waitFor(() => expect(deleteCourseCoverMock).toHaveBeenCalledWith(5))
    expect((await screen.findByRole('status')).textContent).toContain('Đã bỏ ảnh bìa')
    const sau = await rowOf('Tin học 10')
    await waitFor(() => expect(sau.getByTestId('course-cover').getAttribute('data-kind')).toBe('generated'))
  })

  it('bỏ ảnh bìa lỗi thì báo lỗi và giữ nguyên ảnh', async () => {
    deleteCourseCoverMock.mockRejectedValue(new Error('hỏng'))
    fetchAdminCoursesMock.mockResolvedValue([KHOA_10_BIA, KHOA_11])
    renderPage()

    const row = await rowOf('Tin học 10')
    fireEvent.click(row.getByRole('button', { name: 'Bỏ ảnh bìa Tin học 10' }))
    fireEvent.click(row.getByRole('button', { name: 'Xác nhận bỏ ảnh bìa' }))

    expect((await row.findByRole('alert')).textContent).toContain('Không bỏ được ảnh bìa')
    expect(row.getByTestId('course-cover').getAttribute('data-kind')).toBe('image')
  })
})

describe('AdminCoursesPage — quyền truy cập', () => {
  it('chưa đăng nhập thì chuyển sang trang đăng nhập, nhớ để quay lại và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(null)

    renderPage()

    expect(await screen.findByText('Trang đăng nhập, quay lại /quan-tri/khoa-hoc')).not.toBeNull()
    expect(fetchAdminCoursesMock).not.toHaveBeenCalled()
  })

  it('học sinh đăng nhập thì báo không có quyền và không gọi API', async () => {
    fetchCurrentUserMock.mockResolvedValue(LEARNER_ME)

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(screen.queryByRole('heading', { level: 1, name: 'Quản lý khóa học' })).toBeNull()
    expect(fetchAdminCoursesMock).not.toHaveBeenCalled()
  })

  it('máy chủ trả 403 thì hiện câu không có quyền thay vì nút Thử lại', async () => {
    fetchAdminCoursesMock.mockRejectedValue(new ApiError(403, 'Bạn không có quyền thực hiện thao tác này'))

    renderPage()

    expect((await screen.findByRole('alert')).textContent).toContain('không có quyền')
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })
})

describe('AdminCoursesPage — danh sách', () => {
  it('Admin thấy tiêu đề và mỗi khóa có tên, slug, mô tả, thứ tự', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { level: 1, name: 'Quản lý khóa học' })).not.toBeNull()
    const khoa10 = await rowOf('Tin học 10')
    expect(khoa10.getByText('tin-hoc-10')).not.toBeNull()
    expect(khoa10.getByText('Chương trình lớp 10')).not.toBeNull()
    expect(khoa10.getByText('Thứ tự 1')).not.toBeNull()
    const khoa11 = await rowOf('Tin học 11')
    expect(khoa11.getByText('Thứ tự 2')).not.toBeNull()
    expect(khoa11.queryByText('null')).toBeNull()
  })

  it('chưa có khóa nào thì báo rõ và vẫn có form thêm khóa', async () => {
    fetchAdminCoursesMock.mockResolvedValue([])

    renderPage()

    expect(await screen.findByText('Chưa có khóa học nào.')).not.toBeNull()
    expect(screen.getByRole('region', { name: 'Thêm khóa học' })).not.toBeNull()
  })

  it('lỗi tải thì có nút Thử lại và bấm thì tải lại', async () => {
    fetchAdminCoursesMock.mockRejectedValueOnce(new Error('mạng'))

    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Tin học 10')).not.toBeNull()
    expect(fetchAdminCoursesMock).toHaveBeenCalledTimes(2)
  })
})

describe('AdminCoursesPage — thêm khóa', () => {
  it('gửi đúng dữ liệu (thứ tự là số), tải lại danh sách, xóa form và báo đã thêm', async () => {
    createCourseMock.mockResolvedValue({ id: 7, slug: 'tin-hoc-12', title: 'Tin học 12', description: null, sortOrder: 3 })
    renderPage()
    await screen.findByText('Tin học 10')

    const form = fillAddForm(' tin-hoc-12 ', ' Tin học 12 ', '', '3')
    fireEvent.click(form.getByRole('button', { name: 'Thêm khóa học' }))

    await waitFor(() =>
      expect(createCourseMock).toHaveBeenCalledWith({
        slug: 'tin-hoc-12',
        title: 'Tin học 12',
        description: '',
        sortOrder: 3,
      }),
    )
    expect((await screen.findByRole('status')).textContent).toContain('Đã thêm khóa học')
    await waitFor(() => expect(fetchAdminCoursesMock).toHaveBeenCalledTimes(2))
    expect((form.getByLabelText('Slug') as HTMLInputElement).value).toBe('')
    expect((form.getByLabelText('Tên khóa học') as HTMLInputElement).value).toBe('')
  })

  it('để trống slug hoặc tên thì báo lỗi và không gọi API', async () => {
    renderPage()
    await screen.findByText('Tin học 10')

    const form = fillAddForm('', '', '', '1')
    fireEvent.click(form.getByRole('button', { name: 'Thêm khóa học' }))

    expect((await screen.findByRole('alert')).textContent).toContain('slug')
    expect(createCourseMock).not.toHaveBeenCalled()
  })

  it('thứ tự không phải số nguyên không âm thì báo lỗi và không gọi API', async () => {
    renderPage()
    await screen.findByText('Tin học 10')

    for (const bad of ['abc', '-1', '1.5', '']) {
      const form = fillAddForm('tin-hoc-12', 'Tin học 12', '', bad)
      fireEvent.click(form.getByRole('button', { name: 'Thêm khóa học' }))
      expect((await screen.findByRole('alert')).textContent).toContain('Thứ tự')
    }
    expect(createCourseMock).not.toHaveBeenCalled()
  })

  it('máy chủ báo slug trùng (409) thì hiện đúng câu và giữ nguyên chữ đã nhập', async () => {
    createCourseMock.mockRejectedValue(
      new ApiError(409, 'Slug khóa học đã tồn tại', { detail: 'Slug khóa học đã tồn tại' }),
    )
    renderPage()
    await screen.findByText('Tin học 10')

    const form = fillAddForm('tin-hoc-10', 'Tin học 10 bản 2', '', '1')
    fireEvent.click(form.getByRole('button', { name: 'Thêm khóa học' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Slug khóa học đã tồn tại')
    expect((form.getByLabelText('Tên khóa học') as HTMLInputElement).value).toBe('Tin học 10 bản 2')
    expect(fetchAdminCoursesMock).toHaveBeenCalledTimes(1)
  })

  it('lỗi 400 theo trường thì hiện câu của máy chủ', async () => {
    createCourseMock.mockRejectedValue(
      new ApiError(400, 'Dữ liệu không hợp lệ', {
        fieldErrors: [{ field: 'slug', message: 'Slug chỉ gồm chữ thường, số và dấu gạch ngang' }],
      }),
    )
    renderPage()
    await screen.findByText('Tin học 10')

    const form = fillAddForm('Tin_Hoc', 'Tin học 12', '', '1')
    fireEvent.click(form.getByRole('button', { name: 'Thêm khóa học' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Slug chỉ gồm chữ thường')
  })
})

describe('AdminCoursesPage — sửa khóa', () => {
  it('bấm Sửa thì form có sẵn giá trị hiện tại (mô tả null thành rỗng)', async () => {
    renderPage()

    const khoa11 = await rowOf('Tin học 11')
    fireEvent.click(khoa11.getByRole('button', { name: 'Sửa Tin học 11' }))

    expect((khoa11.getByLabelText('Slug') as HTMLInputElement).value).toBe('tin-hoc-11')
    expect((khoa11.getByLabelText('Tên khóa học') as HTMLInputElement).value).toBe('Tin học 11')
    expect((khoa11.getByLabelText('Mô tả (không bắt buộc)') as HTMLInputElement).value).toBe('')
    expect((khoa11.getByLabelText('Thứ tự hiển thị') as HTMLInputElement).value).toBe('2')
  })

  it('lưu thì gọi updateCourse đúng id và dữ liệu, tải lại danh sách và báo đã lưu', async () => {
    updateCourseMock.mockResolvedValue({ ...KHOA_10, title: 'Tin học 10 (mới)' })
    renderPage()

    const khoa10 = await rowOf('Tin học 10')
    fireEvent.click(khoa10.getByRole('button', { name: 'Sửa Tin học 10' }))
    type(khoa10, 'Tên khóa học', 'Tin học 10 (mới)')
    fireEvent.click(khoa10.getByRole('button', { name: 'Lưu thay đổi' }))

    await waitFor(() =>
      expect(updateCourseMock).toHaveBeenCalledWith(5, {
        slug: 'tin-hoc-10',
        title: 'Tin học 10 (mới)',
        description: 'Chương trình lớp 10',
        sortOrder: 1,
      }),
    )
    expect((await screen.findByRole('status')).textContent).toContain('Đã lưu khóa học')
    await waitFor(() => expect(fetchAdminCoursesMock).toHaveBeenCalledTimes(2))
  })

  it('lỗi từ máy chủ thì hiện câu lỗi, form sửa vẫn mở và giữ chữ đã nhập', async () => {
    updateCourseMock.mockRejectedValue(
      new ApiError(409, 'Slug khóa học đã tồn tại', { detail: 'Slug khóa học đã tồn tại' }),
    )
    renderPage()

    const khoa10 = await rowOf('Tin học 10')
    fireEvent.click(khoa10.getByRole('button', { name: 'Sửa Tin học 10' }))
    type(khoa10, 'Slug', 'tin-hoc-11')
    fireEvent.click(khoa10.getByRole('button', { name: 'Lưu thay đổi' }))

    expect((await khoa10.findByRole('alert')).textContent).toContain('Slug khóa học đã tồn tại')
    expect((khoa10.getByLabelText('Slug') as HTMLInputElement).value).toBe('tin-hoc-11')
  })

  it('Hủy thì đóng form và không gọi API', async () => {
    renderPage()

    const khoa10 = await rowOf('Tin học 10')
    fireEvent.click(khoa10.getByRole('button', { name: 'Sửa Tin học 10' }))
    fireEvent.click(khoa10.getByRole('button', { name: 'Hủy' }))

    expect(khoa10.queryByLabelText('Slug')).toBeNull()
    expect(updateCourseMock).not.toHaveBeenCalled()
  })
})

describe('AdminCoursesPage — xóa khóa', () => {
  it('phải xác nhận mới xóa: bấm Xóa chỉ hỏi, bấm Xác nhận xóa mới gọi API rồi tải lại và báo đã xóa', async () => {
    deleteCourseMock.mockResolvedValue(undefined)
    renderPage()

    const khoa11 = await rowOf('Tin học 11')
    fireEvent.click(khoa11.getByRole('button', { name: 'Xóa Tin học 11' }))
    expect(khoa11.getByText(/Xóa khóa học "Tin học 11"\?/)).not.toBeNull()
    expect(deleteCourseMock).not.toHaveBeenCalled()

    fireEvent.click(khoa11.getByRole('button', { name: 'Xác nhận xóa' }))

    await waitFor(() => expect(deleteCourseMock).toHaveBeenCalledWith(6))
    expect((await screen.findByRole('status')).textContent).toContain('Đã xóa khóa học')
    await waitFor(() => expect(fetchAdminCoursesMock).toHaveBeenCalledTimes(2))
  })

  it('Hủy ở bước xác nhận thì không xóa', async () => {
    renderPage()

    const khoa11 = await rowOf('Tin học 11')
    fireEvent.click(khoa11.getByRole('button', { name: 'Xóa Tin học 11' }))
    fireEvent.click(khoa11.getByRole('button', { name: 'Hủy' }))

    expect(deleteCourseMock).not.toHaveBeenCalled()
    expect(khoa11.queryByRole('button', { name: 'Xác nhận xóa' })).toBeNull()
  })

  it('khóa còn bài (409) thì hiện câu của máy chủ và khóa vẫn còn trong danh sách', async () => {
    deleteCourseMock.mockRejectedValue(
      new ApiError(409, 'Không xóa được khóa học còn bài học', { detail: 'Không xóa được khóa học còn bài học' }),
    )
    renderPage()

    const khoa10 = await rowOf('Tin học 10')
    fireEvent.click(khoa10.getByRole('button', { name: 'Xóa Tin học 10' }))
    fireEvent.click(khoa10.getByRole('button', { name: 'Xác nhận xóa' }))

    expect((await khoa10.findByRole('alert')).textContent).toContain('Không xóa được khóa học còn bài học')
    expect(screen.getByText('Tin học 10')).not.toBeNull()
    expect(fetchAdminCoursesMock).toHaveBeenCalledTimes(1)
  })
})
