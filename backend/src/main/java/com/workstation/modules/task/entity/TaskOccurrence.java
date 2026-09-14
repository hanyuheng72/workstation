package com.workstation.modules.task.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("task_occurrence")
public class TaskOccurrence extends BaseEntity {

    private Long taskId;

    private LocalDate occurDate;

    private TaskStatus status;

    /**
     * 取消完成时要把这个字段置回 null。MyBatis-Plus 的 updateById 默认跳过 null 字段，
     * 不加 ALWAYS 就会留下一个「未完成但仍有完成时间」的脏值。
     * （旧版本叫 IGNORED，3.5 后期已移除，只能用 ALWAYS。）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime completedAt;
}
