package com.workstation.modules.focus.dto;

import com.workstation.modules.focus.entity.FailReason;
import com.workstation.modules.focus.entity.FocusStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 一场专注。
 *
 * remainingSeconds 由服务端算好，前端拿到后本地每秒递减即可——
 * 手机时钟可能不准，倒计时的权威必须在服务端；每次接口调用都会重新对齐一次。
 * 暂停中这个值保持不变（倒计时本来就不走）。
 */
public record FocusSessionVO(
        Long id,
        LocalDate sessionDate,
        String subject,
        int plannedMinutes,
        FocusStatus status,
        FailReason failReason,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        /** 那一次暂停机会是否已经用掉；用过之后前端要把暂停按钮置灰 */
        boolean pauseUsed,
        int pausedSeconds,
        /** 学完了但没走满设定时长。一样是成功，只是记录上分得出来 */
        boolean endedEarly,
        /** 真正专注的秒数。进行中时是 0，收尾之后才有值，上限是设定时长 */
        int actualSeconds,
        long remainingSeconds) {
}
