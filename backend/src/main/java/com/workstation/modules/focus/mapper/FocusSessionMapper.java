package com.workstation.modules.focus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.focus.dto.DailyFocusRow;
import com.workstation.modules.focus.dto.FocusTotalsRow;
import com.workstation.modules.focus.entity.FocusSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FocusSessionMapper extends BaseMapper<FocusSession> {

    /**
     * 每日专注情况。只返回区间内真的有记录的日期，缺的日子由服务层补 0——
     * 这样图上空的那几天读作「那天真的没学」，而不是数据缺失。
     */
    @Select("""
            SELECT session_date AS sessionDate,
                   COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN actual_seconds ELSE 0 END), 0) AS successSeconds,
                   COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END)                                AS successCount,
                   COUNT(CASE WHEN status = 'FAILED'  THEN 1 END)                                AS failCount
            FROM focus_session
            WHERE session_date BETWEEN #{from} AND #{to}
            GROUP BY session_date
            ORDER BY session_date
            """)
    List<DailyFocusRow> selectDaily(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * 全时段累计。只统计已经结束的场次，进行中那一场不该计入。
     *
     * 秒数直接求和、回到 Java 再换算成分钟：逐场取整再相加会积少成多。
     * 计数用 COUNT 而不是 SUM：零行时 SUM 返回的是 NULL，映射到 int 会直接 NPE，
     * COUNT 恒为 0。整表为空是这个功能最常见的起始状态，不能在这里炸。
     */
    @Select("""
            SELECT COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN actual_seconds ELSE 0 END), 0) AS successSeconds,
                   COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END)                                AS successCount,
                   COUNT(CASE WHEN status = 'FAILED'  THEN 1 END)                                AS failCount
            FROM focus_session
            WHERE status IN ('SUCCESS', 'FAILED')
            """)
    FocusTotalsRow selectTotals();
}
