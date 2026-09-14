package com.workstation.modules.weight.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@TableName("weight_record")
public class WeightRecord extends BaseEntity {

    private LocalDate recordDate;

    private BigDecimal weightKg;

    private String note;
}
