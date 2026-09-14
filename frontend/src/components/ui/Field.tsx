import type { InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'
import { cn } from '@/utils/cn'

export function Field({
  label,
  hint,
  error,
  children,
}: {
  label?: string
  hint?: ReactNode
  error?: string | null
  children: ReactNode
}) {
  return (
    <label className="flex flex-col gap-2">
      {label ? <span className="text-xs font-medium text-fg-muted">{label}</span> : null}
      {children}
      {error ? (
        <span className="text-xs text-danger">{error}</span>
      ) : hint ? (
        <span className="text-xs text-fg-subtle">{hint}</span>
      ) : null}
    </label>
  )
}

/*
 * 输入框做成「凹槽」：拟物体系里，能按进去的东西天然就是凹陷的。
 * 底色不另设，只靠内阴影。
 */
const CONTROL =
  'w-full rounded-tile bg-surface px-4 py-3.5 text-[15px] shadow-inset outline-none ' +
  'placeholder:text-fg-subtle transition-shadow ' +
  'focus:shadow-inset-deep focus:outline-2 focus:outline-offset-0 focus:outline-primary ' +
  'disabled:opacity-60'

export function Input({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(CONTROL, 'tabular', className)} {...rest} />
}

export function TextArea({ className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn(CONTROL, 'min-h-24 resize-none', className)} {...rest} />
}
