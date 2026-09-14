package com.workstation.modules.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workstation.modules.profile.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
