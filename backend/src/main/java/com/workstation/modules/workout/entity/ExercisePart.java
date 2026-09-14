package com.workstation.modules.workout.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("exercise_part")
public class ExercisePart extends BaseEntity {

    private String code;

    private String name;

    private Integer sortOrder;

    /** 有氧没有「重量 × 次数」，记录时长与距离 */
    public boolean isCardio() {
        return "CARDIO".equals(code);
    }
}
