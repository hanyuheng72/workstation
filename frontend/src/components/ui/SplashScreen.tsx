export function SplashScreen() {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-3 bg-bg">
      <div className="size-8 animate-spin rounded-full border-2 border-line border-t-primary" />
      <p className="text-sm text-fg-subtle">正在载入</p>
    </div>
  )
}
