"""个人工作台 —— 接口冒烟测试。

设计原则：**可以在任何时刻重复执行，不管库里有没有真实数据。**

为了做到这一点：
  * 造数据一律用 1999 年这类遥远的日期，然后用日期区间查询做精确断言，
    与真实数据天然隔离；
  * 全局统计（max/min/avg/PR 之类）只断言不变量与跨接口一致性，
    不断言绝对数值；
  * 计数类断言一律相对基线取增量；
  * 绝不写今天或近期的日期——那会覆盖掉真实记录。

跑完会删掉自己造的数据，不影响你的真实数据。

用法（需后端已启动，且 backend/.env 里有 APP_PASSWORD）：

    python tools/api-smoke.py

用 Python 而不是 curl，是为了避开 Git Bash 的中文编码问题。
可用 WORKSTATION_API 指向别的实例，例如 http://localhost:8081/api
"""

import json
import os
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta
from pathlib import Path

BASE = os.environ.get("WORKSTATION_API", "http://localhost:8080/api")
ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / "backend" / ".env"

# 测试数据专用的历史日期，远离真实数据
T1, T2, T3 = date(1999, 12, 29), date(1999, 12, 30), date(1999, 12, 31)
TEST_MONTH = "1999-12"
TEST_YEAR = 1999

failures: list[str] = []
passed = 0


def check(label: str, condition: bool, detail: object = "") -> None:
    global passed
    if condition:
        passed += 1
        print(f"  [ok]   {label}")
    else:
        failures.append(label)
        print(f"  [FAIL] {label}  {detail}")


def read_password() -> str:
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        if line.startswith("APP_PASSWORD="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("backend/.env 里没有 APP_PASSWORD")


def call(method: str, path: str, body=None, token: str | None = None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(BASE + path, data=data, method=method)
    request.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8")
        try:
            return error.code, json.loads(raw)
        except json.JSONDecodeError:
            return error.code, {"raw": raw}


def data_of(method: str, path: str, body=None, token: str | None = None):
    """只要 data，顺带断言 code == 0"""
    status, payload = call(method, path, body, token)
    if payload.get("code") != 0:
        raise SystemExit(f"{method} {path} 失败: {payload}")
    return payload["data"]


def close_to(actual, expected, tolerance=0.011) -> bool:
    return actual is not None and abs(float(actual) - float(expected)) <= tolerance


def cleanup(token: str, created: list[tuple[str, str]]) -> None:
    endpoints = {
        "weight": "weights",
        "workout": "workout/records",
        "txn": "finance/transactions",
        "task": "tasks",
    }
    for kind, ident in created:
        call("DELETE", f"/{endpoints[kind]}/{ident}", token=token)


def main() -> None:
    # 先问这个部署要不要登录：关掉登录时根本没有令牌可拿
    auth_status = data_of("GET", "/auth/status")
    token = None
    if auth_status["authRequired"]:
        token = data_of("POST", "/auth/login", {"password": read_password()})["token"]

    today = date.today()
    created: list[tuple[str, str]] = []

    try:
        run(token, today, created)
    finally:
        # 无论中途是否失败，都要把造出来的数据收干净
        cleanup(token, created)

    print(f"\n通过 {passed} 项，失败 {len(failures)} 项")
    if failures:
        for item in failures:
            print(f"  x {item}")
        sys.exit(1)
    print("全部通过")


def run(token: str, today: date, created: list[tuple[str, str]]) -> None:
    # ---------------- 认证 ----------------
    print("== 认证 ==")
    # 这个部署到底要不要登录，由后端开关决定，测试按实际情况断言
    auth_status = data_of("GET", "/auth/status", token=token)
    auth_required = auth_status["authRequired"]

    if auth_required:
        status, _ = call("POST", "/auth/login", {"password": "000000"})
        check("错误口令返回 401", status == 401, status)
        status, _ = call("GET", "/weights")
        check("未带令牌返回 401", status == 401, status)
        status, _ = call("GET", "/weights", token="not-a-real-token")
        check("伪造令牌返回 401", status == 401, status)
    else:
        check("部署已关闭登录", auth_status["authRequired"] is False, auth_status)
        status, _ = call("GET", "/weights")
        check("关闭登录时不带令牌也能读", status == 200, status)
        status, _ = call("GET", "/dashboard/overview")
        check("关闭登录时仪表盘可直接访问", status == 200, status)
        # 关闭登录时签发令牌已无意义，接口要明确拒绝，而不是因为密钥为空崩掉
        status, payload = call("POST", "/auth/login", {"password": "whatever"})
        check("关闭登录时登录接口明确拒绝而非 500",
              payload.get("code") == 400 and status == 400, (status, payload))

    # ---------------- 档案 ----------------
    print("\n== 档案 ==")
    profile = data_of("GET", "/profile", token=token)
    height = float(profile["heightCm"])
    check("档案可读且身高合理", 100 <= height <= 250, height)

    # ---------------- 体重 ----------------
    print("\n== 体重 ==")
    for day, weight in ((T1, 70.0), (T2, 71.0), (T3, 72.0)):
        created.append(("weight", data_of("POST", "/weights",
                                          {"recordDate": day.isoformat(), "weightKg": weight},
                                          token)["id"]))

    # 日期区间查询把真实数据排除在外，可以放心断言绝对值
    scoped = data_of("GET", f"/weights?start={T1}&end={T3}", token=token)
    check("区间内有 3 条记录", len(scoped) == 3, len(scoped))
    check("按日期倒序返回", [r["recordDate"] for r in scoped] == [str(T3), str(T2), str(T1)], scoped)
    check("最新一条为 72.0", close_to(scoped[0]["weightKg"], 72.0), scoped[0])

    # 同一天重复提交是修正而非新增
    again = data_of("POST", "/weights", {"recordDate": T3.isoformat(), "weightKg": 72.5}, token)
    created.append(("weight", again["id"]))
    scoped = data_of("GET", f"/weights?start={T1}&end={T3}", token=token)
    check("同一天再次提交仍是 3 条（覆盖而非新增）", len(scoped) == 3, len(scoped))
    check("覆盖后取到新值 72.5", close_to(scoped[0]["weightKg"], 72.5), scoped[0])

    status, payload = call("POST", "/weights", {"weightKg": 999}, token)
    check("异常体重被校验拦下", payload.get("code") == 4000, payload)

    # 全局统计只断言不变量，不断言具体数值
    stats = data_of("GET", "/weights/stats", token=token)
    check("统计：最高 ≥ 当前 ≥ 最低",
          float(stats["max"]) >= float(stats["current"]) >= float(stats["min"]), stats)
    check("统计：平均落在最低与最高之间",
          float(stats["min"]) <= float(stats["average"]) <= float(stats["max"]), stats)
    check("统计：记录了测试写入的 72.5", float(stats["max"]) >= 72.5, stats["max"])

    check("日趋势可查", isinstance(data_of("GET", "/weights/trend?granularity=day", token=token), list))
    check("月趋势可查", isinstance(data_of("GET", "/weights/trend?granularity=month", token=token), list))
    check("年趋势可查", isinstance(data_of("GET", "/weights/trend?granularity=year", token=token), list))
    status, payload = call("GET", "/weights/trend?granularity=hour", token=token)
    check("非法粒度被拒绝", payload.get("code") != 0, payload)

    # BMI 验证公式本身，而不是某个具体记录
    bmi = data_of("GET", "/weights/bmi", token=token)
    if bmi["bmi"] is None:
        check("BMI 在无记录时为 null 而非 0", bmi["bmi"] is None, bmi)
    else:
        expected = round(float(bmi["weightKg"]) / ((float(bmi["heightCm"]) / 100) ** 2), 1)
        check("BMI 等于 体重 / 身高²（四舍五入到 1 位）",
              close_to(bmi["bmi"], expected, 0.05), (bmi["bmi"], expected))
        check("BMI 分级属于四个已知档位之一",
              bmi["category"] in {"偏瘦", "正常", "超重", "肥胖"}, bmi["category"])
        check("刻度尺四个分区且首尾衔接", len(bmi["bands"]) == 4, bmi["bands"])
        check("健康体重区间下界 < 上界",
              float(bmi["healthyMinKg"]) < float(bmi["healthyMaxKg"]), bmi)
        # 上界必须落在「正常」区间内，否则推荐范围会包含被标为超重的体重
        upper_bmi = float(bmi["healthyMaxKg"]) / ((float(bmi["heightCm"]) / 100) ** 2)
        check("健康体重上界对应的 BMI < 24（不落在超重区）", upper_bmi < 24.0, upper_bmi)

    # ---------------- 健身 ----------------
    print("\n== 健身 ==")
    parts = data_of("GET", "/workout/parts", token=token)
    check("部位字典有 7 项", len(parts) == 7, len(parts))
    chest = next(p for p in parts if p["code"] == "CHEST")
    cardio = next(p for p in parts if p["code"] == "CARDIO")
    check("有氧部位标记为 cardio", cardio["cardio"] is True, cardio)

    exercises = data_of("GET", f"/workout/parts/{chest['id']}/exercises", token=token)
    bench = next(e for e in exercises if e["name"] == "卧推")

    first = data_of("POST", "/workout/records",
                    {"recordDate": T2.isoformat(), "exerciseId": bench["id"],
                     "sets": [{"weightKg": 60, "reps": 10}]}, token)
    created.append(("workout", first["id"]))
    check("容量 = 60×10", close_to(first["totalVolume"], 600), first["totalVolume"])

    comparison = data_of("GET", f"/workout/records/{first['id']}/comparison", token=token)
    check("首次记录不产生对比文案", comparison["hasPrevious"] is False, comparison)

    second = data_of("POST", "/workout/records",
                     {"recordDate": T3.isoformat(), "exerciseId": bench["id"],
                      "sets": [{"weightKg": 62.5, "reps": 8}]}, token)
    created.append(("workout", second["id"]))
    comparison = data_of("GET", f"/workout/records/{second['id']}/comparison", token=token)
    check("对比文案：重量较上次提升 2.5kg",
          comparison["message"] == "重量较上次提升 2.5kg", comparison["message"])
    check("重量差值为 2.5", close_to(comparison["weightDelta"], 2.5), comparison)

    status, payload = call("POST", "/workout/records",
                           {"exerciseId": bench["id"], "sets": [{"weightKg": 60}]}, token)
    check("力量动作缺次数被拦下", payload.get("code") == 400, payload)

    running = data_of("GET", f"/workout/parts/{cardio['id']}/exercises", token=token)[0]
    cardio_record = data_of("POST", "/workout/records",
                            {"recordDate": T3.isoformat(), "exerciseId": running["id"],
                             "sets": [{"durationMin": 30, "distanceKm": 5}]}, token)
    created.append(("workout", cardio_record["id"]))
    check("有氧按时长/距离记录成功", cardio_record["id"] is not None)

    status, payload = call("POST", "/workout/records",
                           {"exerciseId": running["id"], "sets": [{"weightKg": 10, "reps": 5}]}, token)
    check("有氧缺时长与距离被拦下", payload.get("code") == 400, payload)

    # 训练量按日期区间隔离，可以断言绝对值
    volume = data_of("GET", f"/workout/stats/volume?start={T1}&end={T3}", token=token)
    check("区间训练量 = 1100（有氧不计入容量）",
          close_to(volume["totalVolume"], 1100), volume["totalVolume"])
    check("区间总组数 = 3", volume["totalSets"] == 3, volume["totalSets"])
    check("区间总次数 = 18（有氧无次数）", volume["totalReps"] == 18, volume["totalReps"])

    check("周频次可查", "sessions" in data_of("GET", "/workout/stats/frequency?granularity=week", token=token))
    check("月频次可查", "sessions" in data_of("GET", "/workout/stats/frequency?granularity=month", token=token))
    status, payload = call("GET", "/workout/stats/frequency?granularity=day", token=token)
    check("非法频次粒度被拒绝", payload.get("code") != 0, payload)

    # PR 是全局的，只断言不变量
    prs = data_of("GET", "/workout/stats/pr", token=token)
    bench_pr = next((p for p in prs if p["exerciseId"] == bench["id"]), None)
    check("PR 列表含卧推", bench_pr is not None, prs)
    check("卧推 PR ≥ 62.5（本次测试写入的最大值）",
          bench_pr and float(bench_pr["maxWeight"]) >= 62.5, bench_pr)
    check("PR 按重量降序",
          all(float(prs[i]["maxWeight"]) >= float(prs[i + 1]["maxWeight"])
              for i in range(len(prs) - 1)), prs)

    # ---------------- 记账 ----------------
    print("\n== 记账 ==")
    expense_categories = data_of("GET", "/finance/categories?type=EXPENSE", token=token)
    income_categories = data_of("GET", "/finance/categories?type=INCOME", token=token)
    check("支出分类 7 项", len(expense_categories) == 7, len(expense_categories))
    check("收入分类 3 项", len(income_categories) == 3, len(income_categories))
    food = next(c for c in expense_categories if c["code"] == "FOOD")
    transport = next(c for c in expense_categories if c["code"] == "TRANSPORT")
    salary = next(c for c in income_categories if c["code"] == "SALARY")

    txn1 = data_of("POST", "/finance/transactions",
                   {"type": "EXPENSE", "amount": 35, "categoryId": food["id"],
                    "occurDate": T2.isoformat()}, token)
    created.append(("txn", txn1["id"]))
    check("返回带分类名", txn1["categoryName"] == "餐饮", txn1)

    txn2 = data_of("POST", "/finance/transactions",
                   {"type": "EXPENSE", "amount": 20, "categoryId": transport["id"],
                    "occurDate": T3.isoformat()}, token)
    created.append(("txn", txn2["id"]))

    status, payload = call("POST", "/finance/transactions",
                           {"type": "EXPENSE", "amount": 10, "categoryId": salary["id"]}, token)
    check("把支出记到收入分类被拦下", payload.get("code") == 400, payload)
    status, payload = call("POST", "/finance/transactions",
                           {"type": "EXPENSE", "amount": -5, "categoryId": food["id"]}, token)
    check("负数金额被拦下", payload.get("code") != 0, payload)

    scoped = data_of("GET", f"/finance/transactions?start={T1}&end={T3}", token=token)
    check("区间内 2 笔支出", scoped["total"] == 2, scoped)

    # 分类占比按月份隔离
    stats = data_of("GET", f"/finance/stats/category?type=EXPENSE&month={TEST_MONTH}", token=token)
    amounts = {c["categoryName"]: float(c["amount"]) for c in stats}
    check("分类占比含餐饮 35", close_to(amounts.get("餐饮"), 35), amounts)
    check("分类占比含交通 20", close_to(amounts.get("交通"), 20), amounts)
    percents = {c["categoryName"]: float(c["percent"]) for c in stats}
    check("餐饮占比 63.6%", abs(percents.get("餐饮", 0) - 63.6) <= 0.2, percents)
    check("占比合计 100%", abs(sum(percents.values()) - 100) <= 0.3, sum(percents.values()))
    check("每条都带 code（前端据此固定配色）",
          all(c.get("code") for c in stats), stats)

    daily = data_of("GET", f"/finance/stats/daily-trend?month={TEST_MONTH}", token=token)
    check("日趋势覆盖 12 月全部 31 天", len(daily) == 31, len(daily))
    day30 = next(p for p in daily if p["key"] == str(T2))
    day31 = next(p for p in daily if p["key"] == str(T3))
    check("12-30 支出 35", close_to(day30["expense"], 35), day30)
    check("12-31 支出 20", close_to(day31["expense"], 20), day31)
    check("无记录的日期补 0 而不是缺失",
          any(float(p["expense"]) == 0 and float(p["income"]) == 0 for p in daily), daily[:3])

    monthly = data_of("GET", f"/finance/stats/monthly-trend?year={TEST_YEAR}", token=token)
    check("月趋势固定 12 个点", len(monthly) == 12, len(monthly))
    december = next(p for p in monthly if p["key"] == TEST_MONTH)
    check("1999-12 支出合计 55", close_to(december["expense"], 55), december)

    status, payload = call("GET", "/finance/stats/category?month=1999-13", token=token)
    check("非法月份格式被拒绝", payload.get("code") != 0, payload)

    overview = data_of("GET", "/finance/stats/overview", token=token)
    check("概览结余 = 本月收入 − 本月支出",
          close_to(overview["monthBalance"],
                   float(overview["monthIncome"]) - float(overview["monthExpense"])), overview)

    # ---------------- 任务 ----------------
    print("\n== 任务 ==")
    # 任务没有日期区间可用来隔离，一律用相对基线的增量断言
    base = data_of("GET", "/tasks/today", token=token)
    base_total, base_done = base["total"], base["done"]

    task = data_of("POST", "/tasks",
                   {"title": "冒烟测试任务", "taskType": "TODAY", "recurrenceType": "DAILY"}, token)
    created.append(("task", task["id"]))

    today_tasks = data_of("GET", "/tasks/today", token=token)
    check("今日任务增加 1 条", today_tasks["total"] == base_total + 1,
          (base_total, today_tasks["total"]))
    check("刚建的任务是未完成状态",
          any(i["taskId"] == task["id"] and i["status"] == "PENDING" for i in today_tasks["items"]),
          today_tasks["items"])

    data_of("POST", f"/tasks/{task['id']}/occurrences/{today.isoformat()}/complete", token=token)
    today_tasks = data_of("GET", "/tasks/today", token=token)
    completed = next(i for i in today_tasks["items"] if i["taskId"] == task["id"])
    check("勾选后已完成数 +1", today_tasks["done"] == base_done + 1,
          (base_done, today_tasks["done"]))
    check("完成后写入完成时间", completed["completedAt"] is not None, completed)

    data_of("DELETE", f"/tasks/{task['id']}/occurrences/{today.isoformat()}/complete", token=token)
    today_tasks = data_of("GET", "/tasks/today", token=token)
    reverted = next(i for i in today_tasks["items"] if i["taskId"] == task["id"])
    check("取消完成后状态回到 PENDING", reverted["status"] == "PENDING", reverted)
    # 这个字段若不清空，就会留下「未完成但仍有完成时间」的脏值
    check("取消完成后完成时间被清空", reverted["completedAt"] is None, reverted)

    # 再勾回来，方便后面的仪表盘检查
    data_of("POST", f"/tasks/{task['id']}/occurrences/{today.isoformat()}/complete", token=token)
    today_tasks = data_of("GET", "/tasks/today", token=token)

    calendar = data_of("GET", f"/tasks/calendar?year={today.year}&month={today.month}", token=token)
    check("日历有本月数据", len(calendar) > 0, calendar)
    check("日历按天聚合，每天最多一个点",
          len({c["date"] for c in calendar}) == len(calendar), calendar)

    stats = data_of("GET", f"/tasks/stats/completion?start={today}&end={today}", token=token)
    check("完成率统计与今日接口一致",
          stats["completionRate"] == today_tasks["completionRate"],
          (stats, today_tasks["completionRate"]))

    # ---------------- 长期任务 ----------------
    print("\n== 长期任务 ==")
    long_task = data_of("POST", "/tasks", {"title": "冒烟长期任务", "taskType": "LONG_TERM"}, token)
    created.append(("task", long_task["id"]))

    check("长期任务不占日历（anchorDate 为空）", long_task["anchorDate"] is None, long_task)
    check("长期任务创建时不生成实例",
          all(i["taskId"] != long_task["id"]
              for i in data_of("GET", f"/tasks/range?start={today}&end={today}", token=token)),
          "range 出现了长期任务")

    after_long = data_of("GET", "/tasks/today", token=token)
    check("★ 长期任务不会混进今日任务",
          all(i["taskId"] != long_task["id"] for i in after_long["items"]), after_long["items"])
    check("★ 建长期任务不改变今日任务数", after_long["total"] == today_tasks["total"],
          (today_tasks["total"], after_long["total"]))

    # 长期任务直接改自身状态，不走按日期勾选
    marked = data_of("POST", f"/tasks/{long_task['id']}/done", token=token)
    check("长期任务可标记完成", marked["status"] == "DONE", marked)
    unmarked = data_of("DELETE", f"/tasks/{long_task['id']}/done", token=token)
    check("长期任务可取消完成", unmarked["status"] == "PENDING", unmarked)

    # 两条路径必须互斥，否则会出现两套状态互相打架
    status, payload = call("POST", f"/tasks/{task['id']}/done", token=token)
    check("今日任务用 /done 被明确拒绝", payload.get("code") == 400, payload)

    # ---------------- 仪表盘 ----------------
    print("\n== 仪表盘 ==")
    dash = data_of("GET", "/dashboard/overview", token=token)
    check("日期为今天", dash["date"] == today.isoformat(), dash["date"])

    # 聚合视图的数字必须与各模块接口算出的完全一致，不能是第二套算法
    check("任务卡与 /tasks/today 一致",
          dash["tasks"]["total"] == today_tasks["total"]
          and dash["tasks"]["done"] == today_tasks["done"], dash["tasks"])
    check("任务卡完成率与 /tasks/today 一致",
          dash["tasks"]["completionRate"] == today_tasks["completionRate"], dash["tasks"])
    check("待办条目带 taskId（首页可直接勾选）",
          all("taskId" in p and "title" in p for p in dash["tasks"]["pending"]), dash["tasks"])

    weight_bmi = data_of("GET", "/weights/bmi", token=token)
    check("体重卡与 /weights/bmi 一致",
          dash["weight"]["bmi"] == weight_bmi["bmi"]
          and dash["weight"]["bmiCategory"] == weight_bmi["category"], dash["weight"])

    finance_overview = data_of("GET", "/finance/stats/overview", token=token)
    check("财务卡与 /finance/stats/overview 一致",
          close_to(dash["finance"]["todayExpense"], finance_overview["todayExpense"])
          and close_to(dash["finance"]["monthBalance"], finance_overview["monthBalance"]),
          dash["finance"])

    check("训练卡字段完整",
          isinstance(dash["workout"]["exerciseNames"], list)
          and isinstance(dash["workout"]["exerciseCount"], int), dash["workout"])

    check("仪表盘为只读：重复调用不改变任务数",
          data_of("GET", "/tasks/today", token=token)["total"] == today_tasks["total"])

    # ---------------- 删除 ----------------
    print("\n== 删除 ==")
    scoped = data_of("GET", f"/weights?start={T1}&end={T3}", token=token)
    data_of("DELETE", f"/weights/{scoped[0]['id']}", token=token)
    after = data_of("GET", f"/weights?start={T1}&end={T3}", token=token)
    check("删除后区间内少一条", len(after) == len(scoped) - 1, (len(scoped), len(after)))
    status, payload = call("DELETE", f"/weights/{scoped[0]['id']}", token=token)
    check("重复删除返回未找到", payload.get("code") == 404, payload)


if __name__ == "__main__":
    main()
