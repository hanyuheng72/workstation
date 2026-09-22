package com.workstation.modules.focus.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("focus_session")
public class FocusSession extends BaseEntity {

    /** 开始那天。创建时定死，跨天的场次仍记在开始的那一天 */
    private LocalDate sessionDate;

    /** 这一场要学的东西 */
    private String subject;

    private Integer plannedMinutes;

    private FocusStatus status;

    /** 仅 FAILED 时有值 */
    private FailReason failReason;

    /** 倒计时的基准时刻。由服务端落库，前端改不了 */
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /** 那一次暂停是否已经用掉 */
    private Boolean pauseUsed;

    /** 当前这轮暂停的起始时刻，续跑时清空 */
    private LocalDateTime pausedAt;

    /** 累计暂停秒数。剩余时间 = planned - (now - startedAt - pausedSeconds) */
    private Integer pausedSeconds;

    /** 提前结束：学完了但没走满设定时长。它和走满一样算成功 */
    private Boolean endedEarly;

    /**
     * 真正专注的秒数，收尾时算好。已扣掉暂停，且以设定时长为上限——
     * 「每日专注时长」统计的是它，不是 planned_minutes。
     */
    private Integer actualSeconds;
}
