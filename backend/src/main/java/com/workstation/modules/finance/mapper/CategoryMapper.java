package com.workstation.modules.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.finance.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
