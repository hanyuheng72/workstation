package com.workstation.modules.workout.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("exercise")
public class Exercise extends BaseEntity {

    private Long partId;

    private String name;

    private Boolean isDefault;

    /** 逻辑删除：动作被删掉后，历史训练记录仍然能引用到它 */
    @TableLogic
    private Boolean deleted;
}
