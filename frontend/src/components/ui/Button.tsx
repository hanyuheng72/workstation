import type { ButtonHTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '@/utils/cn'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'md' | 'lg' | 'sm'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  block?: boolean
  loading?: boolean
}

/*
 * 拟物里的按钮分两类：
 *   次级按钮和底色同色，靠凸起阴影立起来；
 *   主按钮带颜色，此时凸起阴影会显脏，改用一层与自身同色的柔和投影。
 */
const VARIANTS: Record<Variant, string> = {
  primary:
    'bg-primary text-primary-fg shadow-[0_6px_16px_rgba(45,99,221,0.35)] active:shadow-[0_2px_8px_rgba(45,99,221,0.3)]',
  secondary: 'bg-surface text-fg shadow-raised-sm neu-pressable',
  ghost: 'text-fg-muted active:bg-surface-2',
  danger:
    'bg-danger text-white shadow-[0_6px_16px_rgba(208,43,57,0.32)] active:shadow-[0_2px_8px_rgba(208,43,57,0.28)]',
}

const SIZES: Record<Size, string> = {
  sm: 'h-9 px-3.5 text-sm rounded-tile',
  md: 'h-11 px-4 text-[15px] rounded-tile',
  lg: 'h-13 px-5 text-base rounded-panel',
}

export function Button({
  variant = 'primary',
  size = 'md',
  block,
  loading,
  className,
  children,
  disabled,
  ...rest
}: PropsWithChildren<ButtonProps>) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-2 font-medium transition-all',
        'disabled:cursor-not-allowed disabled:opacity-55 disabled:shadow-none',
        VARIANTS[variant],
        SIZES[size],
        block && 'w-full',
        className,
      )}
      disabled={disabled || loading}
      {...rest}
    >
      {loading ? '处理中…' : children}
    </button>
  )
}
