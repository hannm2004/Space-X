import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { fetchHealth } from './api'

vi.mock('./api', () => ({ fetchHealth: vi.fn() }))

beforeEach(() => {
  vi.mocked(fetchHealth).mockResolvedValue('UP')
  vi.stubGlobal(
    'matchMedia',
    vi.fn().mockReturnValue({ matches: false }),
  )
})

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App — định tuyến', () => {
  it('đường dẫn / hiện trang chủ', async () => {
    renderAt('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Học Tin học 10, 11, 12' })).not.toBeNull()
    expect(await screen.findByText('Hệ thống hoạt động bình thường')).not.toBeNull()
  })

  it('đường dẫn không tồn tại hiện trang "Không tìm thấy trang" kèm liên kết về trang chủ', () => {
    renderAt('/khong-co-trang-nay')

    expect(screen.getByRole('heading', { level: 1, name: 'Không tìm thấy trang' })).not.toBeNull()
    const home = screen.getByRole('link', { name: 'Về trang chủ' })
    expect(home.getAttribute('href')).toBe('/')
  })

  it('mọi trang đều có thanh đầu trang với liên kết Space-X về trang chủ và nút đổi giao diện', () => {
    renderAt('/khong-co-trang-nay')

    const brand = screen.getByRole('link', { name: 'Space-X' })
    expect(brand.getAttribute('href')).toBe('/')
    expect(screen.getByRole('button', { name: 'Chuyển sang giao diện tối' })).not.toBeNull()
  })
})
