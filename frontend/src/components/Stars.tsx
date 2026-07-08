import { useState } from 'react'

export default function Stars({ value, max = 5, size = 'sm', interactive = false, onChange }: {
  value: number
  max?: number
  size?: 'sm' | 'lg'
  interactive?: boolean
  onChange?: (v: number) => void
}) {
  const [hover, setHover] = useState(0)
  const sz = size === 'lg' ? 'text-2xl' : 'text-base'
  return (
    <span className={`flex gap-0.5 ${sz}`}>
      {Array.from({ length: max }, (_, i) => i + 1).map(i => (
        <span
          key={i}
          className={`${interactive ? 'cursor-pointer' : ''} transition-colors leading-none`}
          style={{ color: i <= (interactive ? hover || value : value) ? '#f59e0b' : '#d1d5db' }}
          onMouseEnter={() => interactive && setHover(i)}
          onMouseLeave={() => interactive && setHover(0)}
          onClick={() => interactive && onChange?.(i)}
        >
          ★
        </span>
      ))}
    </span>
  )
}
