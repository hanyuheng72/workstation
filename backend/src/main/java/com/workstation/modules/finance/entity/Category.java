package com.workstation.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.workstation.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("category")
public class Category extends BaseEntity {

    private String code;

    private String name;

    private TransactionType type;

    private String icon;

    private Integer sortOrder;

    /** 内置分类不允许删除 */
    private Boolean isSystem;
}
