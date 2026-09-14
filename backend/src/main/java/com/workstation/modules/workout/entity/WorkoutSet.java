package com.workstation.modules.workout.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("workout_set")
public class WorkoutSet extends BaseEntity {

    private Long recordId;

    private Integer setIndex;

    private BigDecimal weightKg;

    private Integer reps;

    private BigDecimal durationMin;

    private BigDecimal distanceKm;
}
