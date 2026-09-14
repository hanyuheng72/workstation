# 个人工作台 Workstation

把**任务 / 体重 / 健身 / 记账**四类日常数据收进一个入口，再由 DeepSeek 汇总成「今日总结 + 建议 + 行动」的个人 App。
移动端优先，可安装到手机主屏。

```
任务 + 体重 + 健身 + 财务
        ↓  写入同一数据库
      仪表盘（今日控制中心）
        ↓  聚合快照喂给 DeepSeek
   总结 + 建议 + 行动
```

技术栈：React + TypeScript + Vite + Tailwind CSS（PWA） / Java 17 + Spring Boot 3.5 + MyBatis-Plus / MySQL 8 / DeepSeek API。

---

## 目录

```
Workstation/
├─ backend/    Spring Boot 服务（业务逻辑、REST API、数据库、DeepSeek 调用）
└─ frontend/   React 单页应用（PWA）
```

---

## 一、准备数据库

需要一个正在运行的 MySQL 8。**本项目只使用 `workstation` 库，不会触碰你已有的其它数据库。**

```bash
cd backend/src/main/resources/db
mysql -u root -p --default-character-set=utf8mb4 < 00-create-database.sql
mysql -u root -p --default-character-set=utf8mb4 -D workstation < schema.sql
mysql -u root -p --default-character-set=utf8mb4 -D workstation < data.sql
```

三个脚本都可以重复执行：建库用 `IF NOT EXISTS`、建表用 `CREATE TABLE IF NOT EXISTS`、种子数据用 `INSERT IGNORE`，不会覆盖你已有的数据。

> 云数据库一般不给应用账号建库权限。那种情况跳过第一个脚本，在服务商控制台里建好 `workstation` 库，再用 `-D workstation` 跑后两个。

跑完后确认：

```bash
mysql -u root -p -e "USE workstation; SHOW TABLES;"
```

应列出 13 张表。建表脚本会同时创建 `workstation` 库本身，无需手动建库。

`data.sql` 只播种**字典表**（身体部位、训练动作、收支分类）与一行用户档案。
体重、训练、记账、任务等业务表保持为空——没有真实数据时界面显示空状态，不画假图。

---

## 二、配置环境变量

```bash
cd backend
cp .env.example .env
```

然后编辑 `backend/.env` 填入真实值：

| 变量 | 说明 |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接信息 |
| `APP_PASSWORD` | App 打开后要输入的登录口令 |
| `JWT_SECRET` | 令牌签名密钥，随便一段 32 字符以上的随机串 |
| `JWT_EXPIRE_DAYS` | 登录保持天数，默认 30 |
| `DEEPSEEK_API_KEY` | DeepSeek 的 Key |
| `DEEPSEEK_BASE_URL` | 默认 `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | 默认 `deepseek-chat` |

> **`.env` 不进版本库**（已在 `.gitignore` 中）。
> 其中 `DEEPSEEK_API_KEY` 只在后端读取，任何接口都不会把它返回给前端，日志里也不会打印。
> 未配置 Key 时，除 AI 外的全部功能都能正常使用。

---

## 三、启动后端

```bash
cd backend
./mvnw spring-boot:run
```

Windows 下用 `mvnw.cmd spring-boot:run`。首次运行会自动下载 Maven 与依赖。

启动后：
- 健康检查 <http://localhost:8080/actuator/health> 应返回 `{"status":"UP"}`
- 未带令牌访问 <http://localhost:8080/api/auth/me> 应返回 401 的统一 JSON

配置从 `backend/.env` 读取；脚本在项目根目录或 `backend/` 目录启动都能找到它。

---

## 四、启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 <http://localhost:5173>，输入 `APP_PASSWORD` 里设置的口令即可进入。

Vite 开发服务器会把 `/api` 代理到 `localhost:8080`，所以前后端要同时运行。

---

## 五、手机上使用

有两条路，选一条即可：

**A. 云部署（推荐，电脑关机也能用）**
后端与数据库都放到云上，手机随时随地可访问。完整步骤见 **[docs/DEPLOY.md](docs/DEPLOY.md)**。

**B. 局域网（仅电脑开着时可用）**
前端 `npm run build` 后把 `dist/` 拷进 `backend/src/main/resources/static/`，
用 `prod` profile 启动后端，手机连同一 WiFi 访问。

Service Worker 只预缓存静态资源，接口请求一律走网络，所以离网时页面能开但数据是空的 ——
这是刻意的，避免看到过期数字。

---

## 六、常用命令

```bash
# 后端
"$MAVEN_HOME/bin/mvn" -f backend/pom.xml spring-boot:run     # 开发运行
"$MAVEN_HOME/bin/mvn" -f backend/pom.xml -q compile          # 只编译，快速查错

# 前端
npm run dev         # 开发服务器
npm run build       # 类型检查 + 打包（含 PWA 资源生成）
npm run typecheck   # 只做类型检查
```

---

## 七、开发辅助脚本

`tools/` 下两个脚本，都需要后端已启动、且 `backend/.env` 里有 `APP_PASSWORD`。
默认打 8080，可用环境变量指向别的实例：`WORKSTATION_API=http://localhost:8081/api`。

```bash
python tools/api-smoke.py            # 接口冒烟测试：58 项断言，跑完自动清理造的数据
python tools/seed-demo.py            # 生成演示数据，便于空库时查看图表效果
python tools/seed-demo.py --clean    # 清空所有业务数据（保留字典）
```

`api-smoke.py` 可以在任意时刻重复执行——它会把造出来的数据删干净。
改过后端接口之后跑一遍，比手点页面快得多。

> 注意：`backend/target/` 是 Maven 的构建输出目录，`mvn clean` 会整个清空，
> 所以脚本放在 `tools/` 而不是 `target/`。

---

## 八、AI 助手

配置 `DEEPSEEK_API_KEY` 后，AI 助手提供两件事：

**1. 用一句话记录。** 在「AI 助手」页直接说人话：

```
我今天体重 65.3 公斤
今天做了卧推，60 公斤，4 组，每组 10 次
今天吃饭花了 35 元
提醒我每天背单词
```

模型把它解析成草稿，**你在界面上点确认之后才会真正写进数据库**。
确认前数据库不会有任何变化，点「不用了」也不会留下痕迹。

**2. 分析与总结。** 「分析本月消费」「分析近期训练」按需调用；
首页的「AI 今日总结」读的是后端缓存，只有点重新生成才会重新调用模型，
所以刷新首页不会反复消耗额度。

实现上有两条硬约束：

- **模型不碰业务表。** 解析结果只落到 `ai_message` 当草稿，
  真正写库由 `ActionExecutor` 完成，而它复用各模块已有的 Service，
  因此 AI 录入的数据与手工录入的走完全相同的校验。
- **Key 不出后端。** `/api/ai/status` 只回 `configured: true|false`，
  连前后几位都不回传；日志里也不打印。

---

## 九、约定

- **数据全部落 MySQL**，前端只保存登录令牌，不存业务数据。
- **AI 不直接改数据**：自然语言先解析成草稿，用户在界面上确认后才写库。
- **空库不画图**：没有真实数据时显示空状态，不生成占位图表或假数字。
- 后端按功能分包（`auth / profile / weight / workout / finance / task / dashboard / export / ai`），
  单表增删改查走 MyBatis-Plus，聚合统计 SQL 放在 `resources/mapper/*.xml`。
