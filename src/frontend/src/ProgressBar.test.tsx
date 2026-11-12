import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ProgressBar from './ProgressBar'

describe('ProgressBar', () => {
  it('là thanh tiến độ có tên và giá trị cho trình đọc màn hình', () => {
    render(<ProgressBar percent={60} label="Tiến độ Tin học 10" />)

    const bar = screen.getByRole('progressbar', { name: 'Tiến độ Tin học 10' })
    expect(bar.getAttribute('aria-valuemin')).toBe('0')
    expect(bar.getAttribute('aria-valuemax')).toBe('100')
    expect(bar.getAttribute('aria-valuenow')).toBe('60')
  })

  it('phần tô màu rộng đúng bằng phần trăm', () => {
    const { container } = render(<ProgressBar percent={33} label="x" />)

    const fill = container.querySelector('[data-testid="progress-fill"]') as HTMLElement
    expect(fill.style.width).toBe('33%')
  })

  it('giá trị ngoài khoảng hoặc không phải số được đưa về 0–100 và làm tròn', () => {
    const { rerender } = render(<ProgressBar percent={-5} label="x" />)
    expect(screen.getByRole('progressbar').getAttribute('aria-valuenow')).toBe('0')

    rerender(<ProgressBar percent={140} label="x" />)
    expect(screen.getByRole('progressbar').getAttribute('aria-valuenow')).toBe('100')

    rerender(<ProgressBar percent={33.6} label="x" />)
    expect(screen.getByRole('progressbar').getAttribute('aria-valuenow')).toBe('34')

    rerender(<ProgressBar percent={Number.NaN} label="x" />)
    expect(screen.getByRole('progressbar').getAttribute('aria-valuenow')).toBe('0')
  })
})
