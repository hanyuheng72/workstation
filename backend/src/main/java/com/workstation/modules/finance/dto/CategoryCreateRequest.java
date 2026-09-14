package com.workstation.modules.finance.dto;

import com.workstation.modules.finance.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "请填写分类名称")
        @Size(max = 20, message = "分类名称最多 20 字")
        String name,

        @NotNull(message = "请选择收入或支出")
        TransactionType type,

        @Size(max = 30, message = "图标标识过长")
        String icon) {
}
