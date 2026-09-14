package com.workstation.modules.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("task")
public class Task extends BaseEntity {

    private String title;

    private String description;

    private TaskType taskType;

    private Integer priority;

    /** TODAY 任务的日期；重复任务的重复起始日 */
    private LocalDate planDate;

    /** LONG_TERM 的截止日，可为空 */
    private LocalDate dueDate;

    private RecurrenceType recurrenceType;

    private Integer recurrenceInterval;

    private LocalDate recurrenceEndDate;

    /** 非重复任务的完成状态；重复任务看各自的 task_occurrence */
    private TaskStatus status;

    public boolean isRecurring() {
        return recurrenceType != null && recurrenceType != RecurrenceType.NONE;
    }

    public boolean isLongTerm() {
        return taskType == TaskType.LONG_TERM;
    }

    /**
     * 这条任务在日历上落在哪一天；返回 null 表示它不占日历格子。
     *
     * 长期任务返回 null —— 它是一份待办清单，不是一个「某天要做的事」。
     * 之前给了它日期，结果长期任务混进了今日任务里。
     */
    public LocalDate anchorDate() {
        if (isLongTerm()) {
            return null;
        }
        return planDate;
    }
}
