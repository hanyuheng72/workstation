package com.workstation.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 仪表盘一次性返回的聚合数据。
 *
 * 首页要同时展示四个模块的摘要，若让前端并发打四个接口，
 * 会出现四个卡片各自先后到达、页面零散跳动的观感。
 * 这里在服务端聚合成一次响应，前端一次渲染到位。
 */
public record DashboardOverviewVO(
        LocalDate date,
        TaskCardVO tasks,
        WeightCardVO weight,
        WorkoutCardVO workout,
        FinanceCardVO finance) {

    /** 今日任务：完成率 + 还没做的几条（带 id，首页可以直接勾选完成） */
    public record TaskCardVO(
            int total,
            int done,
            int completionRate,
            List<PendingTaskVO> pending) {
    }

    public record PendingTaskVO(Long taskId, String title, LocalDate occurDate) {
    }

    /** 今日体重：没有记录时除 bmiCategory 外均为 null */
    public record WeightCardVO(
            LocalDate recordDate,
            BigDecimal weightKg,
            /** 与上一条记录的差值，只有一条记录时为 null */
            BigDecimal delta,
            BigDecimal bmi,
            String bmiCategory) {
    }

    /** 今日训练：练了哪些动作、总容量 */
    public record WorkoutCardVO(
            int exerciseCount,
            List<String> exerciseNames,
            BigDecimal totalVolume) {
    }

    /** 今日支出、本周支出与本月收支 */
    public record FinanceCardVO(
            BigDecimal todayExpense,
            BigDecimal weekExpense,
            BigDecimal monthIncome,
            BigDecimal monthExpense,
            BigDecimal monthBalance) {
    }
}
