import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HealthStatus from './HealthStatus'
import { fetchHealth } from './api'

vi.mock('./api', () => ({ fetchHealth: vi.fn() }))

const fetchHealthMock = vi.mocked(fetchHealth)

beforeEach(() => {
  fetchHealthMock.mockReset()
})

describe('HealthStatus', () => {
  it('hiện "đang kiểm tra" trong lúc chờ máy chủ trả lời', () => {
    fetchHealthMock.mockReturnValue(new Promise(() => {}))

    render(<HealthStatus />)

    expect(screen.getByRole('status').textContent).toContain('Đang kiểm tra hệ thống...')
  })

  it('máy chủ trả UP thì báo hệ thống hoạt động bình thường', async () => {
    fetchHealthMock.mockResolvedValue('UP')

    render(<HealthStatus />)

    expect(await screen.findByText('Hệ thống hoạt động bình thường')).not.toBeNull()
    expect(screen.queryByText('Đang kiểm tra hệ thống...')).toBeNull()
  })

  it('máy chủ trả trạng thái khác UP thì hiện đúng trạng thái đó', async () => {
    fetchHealthMock.mockResolvedValue('DOWN')

    render(<HealthStatus />)

    expect(await screen.findByText('Trạng thái hệ thống: DOWN')).not.toBeNull()
    expect(screen.queryByText('Hệ thống hoạt động bình thường')).toBeNull()
  })

  it('không gọi được máy chủ thì báo lỗi và có nút Thử lại', async () => {
    fetchHealthMock.mockRejectedValue(new Error('mất mạng'))

    render(<HealthStatus />)

    expect(await screen.findByText('Không kết nối được tới máy chủ.')).not.toBeNull()
    expect(screen.getByRole('button', { name: 'Thử lại' })).not.toBeNull()
  })

  it('bấm Thử lại thì kiểm tra lại và hiện kết quả mới', async () => {
    fetchHealthMock.mockRejectedValueOnce(new Error('mất mạng')).mockResolvedValueOnce('UP')

    render(<HealthStatus />)
    fireEvent.click(await screen.findByRole('button', { name: 'Thử lại' }))

    expect(screen.getByText('Đang kiểm tra hệ thống...')).not.toBeNull()
    expect(await screen.findByText('Hệ thống hoạt động bình thường')).not.toBeNull()
    expect(fetchHealthMock).toHaveBeenCalledTimes(2)
  })
})
