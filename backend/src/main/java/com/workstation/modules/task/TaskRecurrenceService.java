package com.workstation.modules.task;

import com.workstation.modules.task.entity.RecurrenceType;
import com.workstation.modules.task.entity.Task;
import com.workstation.modules.task.entity.TaskOccurrence;
import com.workstation.modules.task.mapper.TaskOccurrenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 重复任务的展开。
 *
 * 设计取舍：不为每个重复任务预先铺满未来所有日期，也不引入定时任务，
 * 而是在任何按日期区间读取任务的接口里，对区间内缺失的实例做一次幂等补齐。
 * 好处是没有调度器、重启安全、改规则不用追历史数据；
 * 代价是读接口带写副作用——这是有意的选择。
 */
@Service
public class TaskRecurrenceService {

    private final TaskOccurrenceMapper occurrenceMapper;

    public TaskRecurrenceService(TaskOccurrenceMapper occurrenceMapper) {
        this.occurrenceMapper = occurrenceMapper;
    }

    /** 这条任务在 [from, to] 区间内应该出现在哪几天 */
    public List<LocalDate> occurrenceDates(Task task, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return List.of();
        }
        LocalDate anchor = task.anchorDate();
        if (anchor == null) {
            return List.of();
        }

        if (!task.isRecurring()) {
            return (anchor.isBefore(from) || anchor.isAfter(to)) ? List.of() : List.of(anchor);
        }

        LocalDate end = task.getRecurrenceEndDate();
        LocalDate limit = (end != null && end.isBefore(to)) ? end : to;
        if (limit.isBefore(from)) {
            return List.of();
        }

        long step = task.getRecurrenceType() == RecurrenceType.WEEKLY
                ? 7L * Math.max(1, task.getRecurrenceInterval())
                : Math.max(1, task.getRecurrenceInterval());

        // 从 anchor 直接跳到区间内的第一个发生日，避免 anchor 在很久以前时逐日空转
        LocalDate cursor = anchor;
        if (cursor.isBefore(from)) {
            long gap = ChronoUnit.DAYS.between(cursor, from);
            cursor = cursor.plusDays(((gap + step - 1) / step) * step);
        }

        List<LocalDate> dates = new ArrayList<>();
        while (!cursor.isAfter(limit)) {
            dates.add(cursor);
            cursor = cursor.plusDays(step);
        }
        return dates;
    }

    /** 幂等补齐：靠 uk_task_date 唯一键去重，重复调用不会产生重复实例 */
    @Transactional
    public void materialize(List<Task> tasks, LocalDate from, LocalDate to) {
        List<TaskOccurrence> pending = new ArrayList<>();
        for (Task task : tasks) {
            for (LocalDate date : occurrenceDates(task, from, to)) {
                TaskOccurrence occurrence = new TaskOccurrence();
                occurrence.setTaskId(task.getId());
                occurrence.setOccurDate(date);
                pending.add(occurrence);
            }
        }
        if (!pending.isEmpty()) {
            occurrenceMapper.insertIgnoreBatch(pending);
        }
    }
}
