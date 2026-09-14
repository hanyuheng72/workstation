package com.workstation.modules.workout.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("workout_record")
public class WorkoutRecord extends BaseEntity {

    private LocalDate recordDate;

    private Long partId;

    private Long exerciseId;

    private String note;
}
