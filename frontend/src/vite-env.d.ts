/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * 接口基地址。留空则用相对路径 /api（开发时由 Vite 代理）。
   * 打包成 App 时必须设为云端绝对地址，例如 https://xxx.onrender.com/api
   */
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
