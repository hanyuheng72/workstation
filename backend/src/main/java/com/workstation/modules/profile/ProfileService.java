package com.workstation.modules.profile;

import com.workstation.common.util.BmiUtil;
import com.workstation.modules.profile.dto.ProfileUpdateRequest;
import com.workstation.modules.profile.dto.ProfileVO;
import com.workstation.modules.profile.entity.UserProfile;
import com.workstation.modules.profile.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProfileService {

    /** 单行表固定用这个 id */
    private static final long PROFILE_ID = 1L;

    private final UserProfileMapper profileMapper;

    public ProfileService(UserProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    public UserProfile getEntity() {
        UserProfile profile = profileMapper.selectById(PROFILE_ID);
        if (profile == null) {
            // data.sql 没跑过时会走到这里；补一行默认档案，免得整个 BMI 功能挂掉
            profile = createDefault();
        }
        return profile;
    }

    public ProfileVO get() {
        return toVO(getEntity());
    }

    public BigDecimal heightCm() {
        BigDecimal height = getEntity().getHeightCm();
        return height == null ? BmiUtil.DEFAULT_HEIGHT_CM : height;
    }

    @Transactional
    public ProfileVO update(ProfileUpdateRequest request) {
        UserProfile profile = getEntity();
        if (request.nickname() != null && !request.nickname().isBlank()) {
            profile.setNickname(request.nickname().trim());
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
        }
        if (request.aiEnabled() != null) {
            profile.setAiEnabled(request.aiEnabled());
        }
        if (request.aiModel() != null && !request.aiModel().isBlank()) {
            profile.setAiModel(request.aiModel().trim());
        }
        profileMapper.updateById(profile);
        return toVO(profile);
    }

    private UserProfile createDefault() {
        UserProfile profile = new UserProfile();
        profile.setId(PROFILE_ID);
        profile.setNickname("我");
        profile.setHeightCm(BmiUtil.DEFAULT_HEIGHT_CM);
        profile.setAiEnabled(true);
        profile.setAiModel("deepseek-chat");
        profileMapper.insert(profile);
        return profile;
    }

    private static ProfileVO toVO(UserProfile profile) {
        return new ProfileVO(profile.getId(), profile.getNickname(), profile.getHeightCm(),
                profile.getAiEnabled(), profile.getAiModel());
    }
}
