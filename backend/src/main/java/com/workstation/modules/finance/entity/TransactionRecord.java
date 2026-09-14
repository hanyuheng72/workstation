package com.workstation.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@TableName("transaction_record")
public class TransactionRecord extends BaseEntity {

    private TransactionType type;

    private BigDecimal amount;

    private Long categoryId;

    private LocalDate occurDate;

    private LocalTime occurTime;

    private String note;
}
