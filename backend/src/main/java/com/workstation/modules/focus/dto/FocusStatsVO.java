package com.workstation.modules.focus.dto;

import java.util.List;

/**
 * 自习室的统计。todayMinutes 只累加今天成功的场次；
 * totalMinutes / successRate 是全时段的，不受 30 天窗口限制。
 *
 * 所有 minutes 都是**真正专注的时长**（actual_seconds 换算而来），
 * 不是当初设定的时长——提前结束的场次按实际学了多久记账。
 *
 * daily 覆盖最近 30 天，中间没学的那几天是 0 而不是缺行——
 * 图上断档会被误读成没数据。完全没有记录时 daily 为空数组，前端据此显示空状态。
 */
public record FocusStatsVO(
        int todayMinutes,
        int todaySuccessCount,
        int todayFailCount,
        int totalMinutes,
        int totalSuccessCount,
        int totalFailCount,
        /** 已结束的场次里成功占的百分比，四舍五入；一场都没结束时为 0 */
        int successRate,
        List<DailyFocusVO> daily) {
}
