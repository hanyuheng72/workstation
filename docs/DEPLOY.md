# 云部署指南

目标：后端跑在云上、数据库托管在云上，**电脑关机也能用**；手机端再打包成 APK。

```
手机 App (HBuilderX 套壳)
        │  https
        ▼
Render.com  ── Docker 跑 Spring Boot ──▶ Aiven Cloud (MySQL 8, SSL)
        ▲
        │  每 5 分钟 ping 一次，防止免费档休眠
   UptimeRobot
```

本地电脑在这套结构里**只负责写代码和打包**，跑不跑都不影响线上。

---

## 第 0 步：先把它放进 Git 仓库

Render 是从 Git 仓库拉代码构建的，所以这一步绕不过去。**这个项目目前还没有 git**，先初始化并推到 GitHub / GitLab（私有仓库即可）。

```bash
cd D:/Claude/Workstation
git init
```

推之前确认 `.gitignore` 里已经排除了 `backend/.env`（项目里已经有了，别删）。**密钥绝不能进仓库**，后面会在 Render 的环境变量里单独配。

---

## 第 1 步：Aiven 建 MySQL

1. 注册 <https://aiven.io>，新建一个 **MySQL** 服务（免费档够用）
2. 建好后在服务的 **Overview** 里拿到这些：Host、Port、User、Password、Database name
3. 在 **Databases** 页签把库名改成 `workstation`（或直接用默认库名，改下面的连接串）

### 在本机把表建好

Aiven 的库是空的，建表脚本要跑一次。**用本机的 mysql 客户端连过去执行**：

```bash
cd D:/Claude/Workstation/backend/src/main/resources/db
mysql -h <Aiven主机> -P <端口> -u <用户> -p --ssl-mode=REQUIRED --default-character-set=utf8mb4 < schema.sql
mysql -h <Aiven主机> -P <端口> -u <用户> -p --ssl-mode=REQUIRED --default-character-set=utf8mb4 < data.sql
```

两个脚本都可以重复执行，不会覆盖已有数据。

> `schema.sql` 开头有 `CREATE DATABASE workstation`，如果 Aiven 不允许建库，
> 把它注释掉，然后在命令里加上 `-D workstation` 指定库名。

---

## 第 2 步：Render 部署后端

1. 注册 <https://render.com>，**New → Web Service**，连上刚才那个仓库
2. 关键设置：
   - **Language**：`Docker`
   - **Root Directory**：`backend` ← 必填，Dockerfile 在这个目录下
   - **Dockerfile Path**：`./Dockerfile`
   - **Instance Type**：Free
3. 环境变量（**Environment** 页签）：

| 变量 | 值 |
|---|---|
| `DB_URL` | `jdbc:mysql://<Aiven主机>:<端口>/workstation?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&sslMode=REQUIRED` |
| `DB_USERNAME` | Aiven 的用户 |
| `DB_PASSWORD` | Aiven 的密码 |
| `APP_AUTH_ENABLED` | **`true`** ← 公网部署务必打开 |
| `APP_PASSWORD` | 你的登录口令（建议 12 位以上随机串） |
| `JWT_SECRET` | 64 位随机串 |
| `DEEPSEEK_API_KEY` | 你的 Key |
| `CORS_ALLOWED_ORIGINS` | `null,https://<你的服务>.onrender.com` |

> `CORS_ALLOWED_ORIGINS` 里的 `null` 是给打包后的 App 用的：WebView 里页面从本地加载，
> 发出的请求来源是 `null`，不放行的话会被跨域策略挡掉。

**不需要配 `SERVER_PORT`** —— Render 会注入 `PORT`，后端已经兼容。

4. 部署完成后 Render 给你一个地址，形如 `https://xxx.onrender.com`。

### 验证

```bash
curl https://xxx.onrender.com/actuator/health
```

应返回 `{"status":"UP"}`。再拿本机的前端指过去跑一遍完整测试：

```bash
cd D:/Claude/Workstation
WORKSTATION_API=https://xxx.onrender.com/api PYTHONIOENCODING=utf-8 python tools/api-smoke.py
```

**这一步别跳过** —— 它会验证云端数据库连接、SSL、登录开关是否都配对了。

---

## 第 3 步：UptimeRobot 保活

Render 免费档 15 分钟没人访问就会休眠，下次访问要等 30–60 秒冷启动。

1. 注册 <https://uptimerobot.com>
2. 新建 Monitor：类型 `HTTP(s)`，地址填 `https://xxx.onrender.com/actuator/health`
3. 间隔 5 分钟

健康检查接口是放行的（不需要登录），所以 UptimeRobot 能正常探活。

---

## 第 4 步：前端打包成 APK

### 4.1 用云端地址构建

```bash
cd D:/Claude/Workstation/frontend
VITE_API_BASE=https://xxx.onrender.com/api npm run build
```

Windows 的 PowerShell 里用：

```powershell
$env:VITE_API_BASE="https://xxx.onrender.com/api"; npm run build
```

**不设这个变量的话，打出来的包里请求地址还是 `/api`** —— 装到手机上会全部请求失败，
因为 App 里的页面是从本地加载的，相对路径没有意义。

产物在 `frontend/dist/`。

### 4.2 HBuilderX 套壳

1. HBuilderX 新建 **5+App** 项目（不是 uni-app）
2. 把 `dist/` 里的文件全部拷进项目的根目录
3. `manifest.json` 里设置应用名、图标、启动图
4. **发行 → 原生App-云打包**，选 Android，用 DCloud 的公共测试证书即可

> iOS 想装成能长期用的 App，需要苹果开发者账号（一年 $99）；
> 不买的话证书 7 天就过期，得反复重装。自用建议先只做 Android。

### 4.3 必须先验证再打包

打包前**先用手机浏览器打开 `https://xxx.onrender.com`** 确认能用：

- 页面能打开
- 数据能加载（点几个页面看看）
- 能写入（记一笔账试试）

浏览器里通了再打包。打包出问题通常是 HBuilderX 的配置问题，
和代码无关，分开排查省时间。

---

## 上公网前的安全检查

| 项 | 说明 |
|---|---|
| `APP_AUTH_ENABLED=true` | **最重要的一条。** 关掉的话，拿到网址的人就能看和改你的全部数据 |
| 口令长度 | 公网可达后，6 位数字几小时就能跑完。换成 12 位以上随机串 |
| 网址别外传 | 一旦泄露收不回来，只能换服务名 |
| Aiven 白名单 | Aiven 默认允许任意来源连接。Render 免费档没有固定出口 IP，所以只能靠强密码 + SSL |
| 定期备份 | `tools/` 下有导出功能可以取全量数据；Aiven 免费档也带基础备份 |

---

## 常见问题

**构建失败，日志里有 `COPY failed`**
检查 Render 的 Root Directory 是否设成了 `backend`。

**部署成功但健康检查 502 / 超时**
看 Render 的 Logs。多半是 `DB_URL` 不对 —— Aiven 强制 SSL，漏了 `sslMode=REQUIRED` 会连不上。

**接口全部 401**
`APP_AUTH_ENABLED=true` 时接口需要令牌。先调用 `/api/auth/status` 确认开关状态，再调 `/api/auth/login` 拿令牌。

**App 里请求全部失败，浏览器里却正常**
十有八九是打包时没设 `VITE_API_BASE`。检查 `dist/assets/*.js` 里有没有你的云端域名：

```bash
grep -o "onrender.com/api" frontend/dist/assets/*.js
```

**时间差 8 小时**
`DB_URL` 里漏了 `serverTimezone=Asia/Shanghai`。

**界面是旧版**
Service Worker 缓存了静态资源。浏览器里 Ctrl+Shift+R 强刷；App 里重新打包安装。
