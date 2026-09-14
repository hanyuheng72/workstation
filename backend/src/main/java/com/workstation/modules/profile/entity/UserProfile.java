package com.workstation.modules.profile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 单行表，固定 id = 1 */
@Getter
@Setter
@TableName("user_profile")
public class UserProfile extends BaseEntity {

    private String nickname;

    private BigDecimal heightCm;

    private Boolean aiEnabled;

    private String aiModel;
}
