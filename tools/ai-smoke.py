"""AI 链路的冒烟测试。

验证的是**架构约束**而不只是「接口能通」：
  1. 解析阶段绝不写业务表（用户确认之前，数据库必须纹丝不动）
  2. 确认之后才落库，且落进去的数据与手工录入的走同一套校验
  3. 不配 Key 时给出明确提示，而不是崩掉

会真实调用 DeepSeek，因此每次执行会消耗少量额度。

用法（需后端已启动）：

    python tools/ai-smoke.py

与其他测试一样，造的数据一律用 1999 年的日期并自行清理，
不会碰你的真实数据。
"""

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

BASE = os.environ.get("WORKSTATION_API", "http://localhost:8080/api")
ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / "backend" / ".env"

TEST_DATE = "1999-12-29"

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


def call(method: str, path: str, body=None, token: str | None = None, timeout=120):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(BASE + path, data=data, method=method)
    request.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8")
        try:
            return error.code, json.loads(raw)
        except json.JSONDecodeError:
            return error.code, {"raw": raw}


def data_of(method: str, path: str, body=None, token: str | None = None, timeout=120):
    status, payload = call(method, path, body, token, timeout)
    if payload.get("code") != 0:
        raise SystemExit(f"{method} {path} 失败: {payload}")
    return payload["data"]


def txns_on(date_str: str, token: str) -> list:
    page = data_of("GET", f"/finance/transactions?start={date_str}&end={date_str}", token=token)
    return page["list"]


def weights_on(date_str: str, token: str) -> list:
    return data_of("GET", f"/weights?start={date_str}&end={date_str}", token=token)


def main() -> None:
    token = data_of("POST", "/auth/login", {"password": read_password()})["token"]
    conversations: list[int] = []
    created_txns: list[int] = []
    created_weights: list[int] = []
    created_workouts: list[int] = []

    try:
        # ---------------- 配置状态 ----------------
        print("== 配置 ==")
        status = data_of("GET", "/ai/status", token=token)
        check("已配置 DeepSeek Key", status["configured"] is True, status)
        check("回报了模型名", bool(status.get("model")), status)
        check("状态接口不回传 Key 本身",
              "key" not in json.dumps(status).lower() or "apiKey" not in status, status)

        if not status["configured"]:
            print("\n未配置 Key，跳过后续需要调用模型的用例")
            return

        # ---------------- 记账：解析 → 草稿 → 确认 ----------------
        print("\n== 语音记账链路 ==")
        before = txns_on(TEST_DATE, token)

        result = data_of("POST", "/ai/chat",
                         {"message": f"{TEST_DATE} 吃饭花了35元"}, token)
        conversations.append(result["conversationId"])
        draft = result["assistantMessage"]

        check("识别出记账意图", draft["intent"] == "LOG_EXPENSE", draft)
        check("动作处于待确认状态", draft["actionStatus"] == "PENDING", draft["actionStatus"])
        check("给出了可读的草稿预览", bool(draft.get("draftPreview")), draft)
        check("模型给了回应文案", bool(draft["content"].strip()), draft["content"])
        # 此刻还没落库，回复里不能说「已记录」，否则会误导用户以为已经存好了
        claim = draft["content"]
        check("★ 确认前不会声称「已记录」",
              not any(word in claim for word in ("已记录", "已经记录", "已保存", "已经保存")), claim)

        # 核心约束：确认之前，业务表必须没有任何变化
        after_parse = txns_on(TEST_DATE, token)
        check("★ 确认之前没有写入任何记账数据",
              len(after_parse) == len(before), (len(before), len(after_parse)))

        confirmed = data_of("POST", f"/ai/actions/{draft['id']}/confirm", token=token)
        check("确认后状态变为已执行", confirmed["actionStatus"] == "EXECUTED", confirmed["actionStatus"])
        check("回执说明了实际记了什么", "已记录" in confirmed["content"], confirmed["content"])

        after_confirm = txns_on(TEST_DATE, token)
        check("★ 确认之后才真正落库", len(after_confirm) == len(before) + 1,
              (len(before), len(after_confirm)))
        if after_confirm:
            saved = next((t for t in after_confirm if t["occurDate"] == TEST_DATE), None)
            check("落库金额为 35", saved and abs(float(saved["amount"]) - 35) < 0.01, saved)
            check("落库分类为餐饮", saved and saved["categoryName"] == "餐饮", saved)
            created_txns.extend(t["id"] for t in after_confirm)

        # ---------------- 拒绝 ----------------
        print("\n== 拒绝草稿 ==")
        before_reject = len(txns_on(TEST_DATE, token))
        result = data_of("POST", "/ai/chat", {"message": f"{TEST_DATE} 打车花了 18 元"}, token)
        conversations.append(result["conversationId"])
        draft = result["assistantMessage"]
        check("识别出记账意图", draft["intent"] == "LOG_EXPENSE", draft["intent"])

        rejected = data_of("POST", f"/ai/actions/{draft['id']}/reject", token=token)
        check("拒绝后状态为 REJECTED", rejected["actionStatus"] == "REJECTED", rejected["actionStatus"])
        check("★ 拒绝之后数据库没有变化",
              len(txns_on(TEST_DATE, token)) == before_reject, before_reject)

        # ---------------- 体重 ----------------
        print("\n== 语音记体重 ==")
        before_weight = len(weights_on(TEST_DATE, token))
        result = data_of("POST", "/ai/chat",
                         {"message": f"{TEST_DATE} 体重 65.3 公斤"}, token)
        conversations.append(result["conversationId"])
        draft = result["assistantMessage"]
        check("识别出体重意图", draft["intent"] == "LOG_WEIGHT", draft)
        check("确认前没有写入体重", len(weights_on(TEST_DATE, token)) == before_weight, before_weight)

        confirmed = data_of("POST", f"/ai/actions/{draft['id']}/confirm", token=token)
        check("确认后执行成功", confirmed["actionStatus"] == "EXECUTED", confirmed["actionStatus"])
        weights = weights_on(TEST_DATE, token)
        check("体重已落库且数值正确",
              len(weights) == before_weight + 1 and abs(float(weights[0]["weightKg"]) - 65.3) < 0.01,
              weights)
        created_weights.extend(w["id"] for w in weights)

        # ---------------- 训练 ----------------
        print("\n== 语音记训练 ==")
        result = data_of("POST", "/ai/chat",
                         {"message": f"{TEST_DATE} 做了卧推，60公斤，4组，每组10次"}, token)
        conversations.append(result["conversationId"])
        draft = result["assistantMessage"]
        check("识别出训练意图", draft["intent"] == "LOG_WORKOUT", draft)

        confirmed = data_of("POST", f"/ai/actions/{draft['id']}/confirm", token=token)
        check("训练草稿执行成功", confirmed["actionStatus"] == "EXECUTED", confirmed["content"])
        records = data_of("GET", f"/workout/records?start={TEST_DATE}&end={TEST_DATE}", token=token)
        check("训练已落库", len(records) > 0, records)
        if records:
            created_workouts.extend(r["id"] for r in records)
            record = records[0]
            check("组数为 4", len(record["sets"]) == 4, record["sets"])
            check("每组 60kg × 10",
                  all(abs(float(s["weightKg"]) - 60) < 0.01 and s["reps"] == 10
                      for s in record["sets"]), record["sets"])
            check("容量 = 60×10×4 = 2400",
                  abs(float(record["totalVolume"]) - 2400) < 1, record["totalVolume"])

        # ---------------- 非动作消息 ----------------
        print("\n== 普通对话不产生草稿 ==")
        result = data_of("POST", "/ai/chat", {"message": "你好，你是谁？"}, token)
        conversations.append(result["conversationId"])
        draft = result["assistantMessage"]
        check("闲聊不产生待确认动作",
              draft["actionStatus"] == "NONE" and draft["intent"] is None, draft)
        check("但有正常的回应", bool(draft["content"].strip()), draft["content"])

        # ---------------- 意图解析的健壮性 ----------------
        print("\n== 解析健壮性 ==")
        result = data_of("POST", "/ai/chat",
                         {"message": f"{TEST_DATE} 买了个键盘花了 499，另外还买了本书 59"}, token)
        conversations.append(result["conversationId"])
        draft = result["assistantMessage"]
        check("一句话里多笔消费时不崩，且给出待确认草稿或明确回应",
              draft["actionStatus"] in {"PENDING", "NONE"}, draft)
        if draft["actionStatus"] == "PENDING":
            data_of("POST", f"/ai/actions/{draft['id']}/reject", token=token)

        # ---------------- 对话历史 ----------------
        print("\n== 对话历史 ==")
        listed = data_of("GET", "/ai/conversations", token=token)
        check("会话列表非空", len(listed) > 0, len(listed))
        messages = data_of("GET", f"/ai/conversations/{conversations[0]}/messages", token=token)
        check("会话里能取到消息", len(messages) >= 2, len(messages))
        check("消息按时间正序",
              all(messages[i]["id"] <= messages[i + 1]["id"] for i in range(len(messages) - 1)))

        # ---------------- 每日总结 ----------------
        print("\n== 每日总结 ==")
        # 先清掉今天的缓存，才能验证 GET 确实不会顺带生成
        sql("DELETE FROM ai_summary WHERE summary_date = CURDATE();")

        empty = data_of("GET", "/ai/summary/today", token=token)
        check("★ 无缓存时 GET 返回空且不调用模型", not empty["content"], empty)
        check("★ 无缓存时 GET 也不会写入 ai_summary", summary_rows() == 0, summary_rows())

        summary = data_of("POST", "/ai/summary/today", token=token, timeout=180)
        check("POST 生成了总结正文", bool(summary["content"]) and len(summary["content"]) > 10,
              str(summary["content"])[:60])
        check("POST 之后缓存里有一条记录", summary_rows() == 1, summary_rows())

        again = data_of("GET", "/ai/summary/today", token=token)
        check("★ 再次 GET 读到刚生成的缓存，不重复调用模型", again["cached"] is True, again)
        check("缓存内容与生成时一致", again["content"] == summary["content"])

    finally:
        # ---------------- 清理 ----------------
        print("\n== 清理 ==")
        for record_id in created_workouts:
            call("DELETE", f"/workout/records/{record_id}", token=token)
        for txn_id in created_txns:
            call("DELETE", f"/finance/transactions/{txn_id}", token=token)
        for weight_id in created_weights:
            call("DELETE", f"/weights/{weight_id}", token=token)

        # 会话与消息没有删除接口，直接清掉本次新建的
        removed = cleanup_conversations(conversations)
        check(f"清理了 {removed} 个测试会话", True)
        check("清理后测试日期无残留记账", len(txns_on(TEST_DATE, token)) == 0)
        check("清理后测试日期无残留体重", len(weights_on(TEST_DATE, token)) == 0)

    print(f"\n通过 {passed} 项，失败 {len(failures)} 项")
    if failures:
        for item in failures:
            print(f"  x {item}")
        sys.exit(1)
    print("全部通过")


def cleanup_conversations(ids: list[int]) -> int:
    """AI 会话没有对外暴露删除接口，测试收尾直接用 SQL 清掉自己造的几条。"""
    if not ids:
        return 0
    listing = ",".join(str(i) for i in ids)
    sql(f"DELETE FROM ai_message WHERE conversation_id IN ({listing}); "
        f"DELETE FROM ai_conversation WHERE id IN ({listing});")
    return len(ids)


def sql(statement: str) -> None:
    """测试需要直接查库来做断言（AI 相关表没有对外暴露删除接口）。"""
    import subprocess

    subprocess.run(["mysql", "-u", "root", "-p123456", "--default-character-set=utf8mb4",
                    "-e", f"USE workstation; {statement}"],
                   capture_output=True, timeout=30)


def summary_rows() -> int:
    import subprocess

    result = subprocess.run(
        ["mysql", "-u", "root", "-p123456", "-N", "-B", "--default-character-set=utf8mb4",
         "-e", "USE workstation; SELECT COUNT(*) FROM ai_summary WHERE summary_date = CURDATE();"],
        capture_output=True, timeout=30)
    try:
        return int(result.stdout.decode("utf-8", "ignore").strip() or 0)
    except ValueError:
        return -1


if __name__ == "__main__":
    main()
