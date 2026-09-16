"""生成一批演示数据，用来查看图表效果；也可以一键清空。

真实使用时不建议留着这些数据——它们只是为了让空库也能看到图表长什么样。

用法：

    python tools/seed-demo.py            # 造数据
    python tools/seed-demo.py --clean    # 清空所有业务数据（保留字典）

只动业务表（体重 / 训练 / 记账 / 任务），不会碰字典表和你已有的数据以外的库。
"""

import argparse
import json
import os
import random
import urllib.error
import urllib.request
from datetime import date, timedelta
from pathlib import Path

BASE = os.environ.get("WORKSTATION_API", "http://localhost:8080/api")
ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / "backend" / ".env"


def read_password() -> str:
    """口令优先取环境变量，取不到才读本机 .env"""
    from_env = os.environ.get("APP_PASSWORD")
    if from_env:
        return from_env
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        if line.startswith("APP_PASSWORD="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("没有拿到口令：请设置 APP_PASSWORD 环境变量，或在 backend/.env 里配置")


def login() -> str | None:
    """关掉登录的部署没有令牌可拿，此时返回 None"""
    auth = call("GET", "/auth/status")
    if not auth.get("data", {}).get("authRequired"):
        return None
    return fetch("POST", "/auth/login", {"password": read_password()})["token"]


def call(method: str, path: str, body=None, token: str | None = None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(BASE + path, data=data, method=method)
    request.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        raise SystemExit(f"{method} {path} -> {error.code} {error.read().decode('utf-8')}")


def fetch(method: str, path: str, body=None, token: str | None = None):
    payload = call(method, path, body, token)
    if payload.get("code") != 0:
        raise SystemExit(f"{method} {path} 失败: {payload}")
    return payload["data"]


def clean(token: str) -> None:
    for path in ("/weights", "/tasks"):
        for item in fetch("GET", path, token=token):
            call("DELETE", f"{path}/{item['id']}", token=token)
    for record in fetch("GET", "/workout/records", token=token):
        call("DELETE", f"/workout/records/{record['id']}", token=token)
    page = fetch("GET", "/finance/transactions?page=1&size=500", token=token)
    for txn in page["list"]:
        call("DELETE", f"/finance/transactions/{txn['id']}", token=token)
    print("已清空业务数据（字典保留）")


def seed(token: str) -> None:
    today = date.today()

    # ---- 体重：过去 60 天，缓慢下降并带一点噪声 ----
    weight = 68.5
    for offset in range(60, -1, -1):
        day = today - timedelta(days=offset)
        # 不是每天都称，隔天记一次更真实
        if offset % 2 == 0:
            weight -= random.uniform(0.02, 0.09)
            fetch("POST", "/weights",
                  {"recordDate": day.isoformat(), "weightKg": round(weight + random.uniform(-0.3, 0.3), 1)},
                  token)

    # ---- 健身：最近 8 周，每周 3~4 练 ----
    parts = fetch("GET", "/workout/parts", token=token)
    by_code = {p["code"]: p for p in parts}
    plan = [
        ("CHEST", "卧推", 60.0, 10),
        ("BACK", "高位下拉", 55.0, 12),
        ("LEG", "深蹲", 80.0, 8),
        ("SHOULDER", "坐姿哑铃推举", 20.0, 10),
        ("ARM", "杠铃弯举", 25.0, 12),
        ("ABS", "卷腹", 0.0, 20),
        ("CARDIO", "跑步", 0.0, 0),
    ]
    random.seed(7)
    for week in range(8):
        for day_offset, (code, exercise_name, base_weight, base_reps) in enumerate(random.sample(plan, 3)):
            target = today - timedelta(weeks=week, days=day_offset)
            if target > today:
                continue
            part = by_code.get(code)
            if part is None:
                continue
            exercises = fetch("GET", f"/workout/parts/{part['id']}/exercises", token=token)
            exercise = next((e for e in exercises if e["name"] == exercise_name), None)
            if exercise is None:
                continue

            # 越近的训练重量略微上涨，形成可见的进步趋势
            progress = (8 - week) * 1.25
            if code == "CARDIO":
                sets = [{"durationMin": 25 + week, "distanceKm": round(3.5 + week * 0.2, 2)}]
            elif code == "ABS":
                sets = [{"weightKg": 0, "reps": 20 + week}]
            else:
                weight_kg = round(base_weight + progress, 1)
                sets = [{"weightKg": weight_kg, "reps": base_reps} for _ in range(3)]
            fetch("POST", "/workout/records",
                  {"recordDate": target.isoformat(), "exerciseId": exercise["id"], "sets": sets}, token)

    # ---- 记账：最近 5 个月 ----
    expense_categories = [c for c in fetch("GET", "/finance/categories?type=EXPENSE", token=token)]
    income_categories = fetch("GET", "/finance/categories?type=INCOME", token=token)
    salary = next(c for c in income_categories if c["code"] == "SALARY")
    allowance = next(c for c in income_categories if c["code"] == "ALLOWANCE")

    random.seed(11)
    for month_back in range(5):
        anchor = today.replace(day=1) - timedelta(days=month_back * 30)
        days_in_month = 28
        for _ in range(random.randint(18, 26)):
            day = anchor.replace(day=random.randint(1, days_in_month))
            if day > today:
                continue
            category = random.choices(
                expense_categories,
                weights=[5 if c["code"] == "FOOD" else 2 for c in expense_categories],
            )[0]
            amount = round(random.uniform(8, 120), 2) if category["code"] != "HOUSING" else 1200.0
            fetch("POST", "/finance/transactions",
                  {"type": "EXPENSE", "amount": amount, "categoryId": category["id"],
                   "occurDate": day.isoformat()}, token)

        fetch("POST", "/finance/transactions",
              {"type": "INCOME", "amount": 3000, "categoryId": salary["id"],
               "occurDate": anchor.replace(day=5).isoformat()}, token)
        fetch("POST", "/finance/transactions",
              {"type": "INCOME", "amount": 1500, "categoryId": allowance["id"],
               "occurDate": anchor.replace(day=10).isoformat()}, token)

    # ---- 任务：今日任务 + 每日重复 + 长期 ----
    fetch("POST", "/tasks", {"title": "写周报", "taskType": "TODAY", "priority": 3}, token)
    fetch("POST", "/tasks", {"title": "整理实验数据", "taskType": "TODAY", "priority": 2}, token)
    fetch("POST", "/tasks", {"title": "背单词", "taskType": "TODAY", "recurrenceType": "DAILY"}, token)
    fetch("POST", "/tasks", {"title": "每周复盘", "taskType": "TODAY", "recurrenceType": "WEEKLY"}, token)
    fetch("POST", "/tasks",
          {"title": "读完《设计数据密集型应用》", "taskType": "LONG_TERM",
           "dueDate": (today + timedelta(days=30)).isoformat()}, token)

    print("演示数据已生成")
    print("  体重  ~31 条")
    print("  训练  ~24 次")
    print("  记账  ~120 笔（横跨 5 个月）")
    print("  任务  5 条")


def main() -> None:
    parser = argparse.ArgumentParser(description="生成或清空演示数据")
    parser.add_argument("--clean", action="store_true", help="清空所有业务数据")
    parser.add_argument("--yes", action="store_true", help="跳过清空前的确认")
    args = parser.parse_args()

    token = login()

    if args.clean:
        # 清空是不可逆的，而库里现在可能有真实数据，所以默认要确认一次
        if not args.yes:
            answer = input("这会删除全部体重、训练、记账与任务数据（字典保留），且不可恢复。\n"
                           "确认请输入 yes：").strip()
            if answer.lower() != "yes":
                print("已取消")
                return
        clean(token)
    else:
        seed(token)


if __name__ == "__main__":
    main()
