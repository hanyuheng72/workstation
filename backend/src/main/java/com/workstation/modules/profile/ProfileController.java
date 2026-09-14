package com.workstation.modules.profile;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.profile.dto.ProfileUpdateRequest;
import com.workstation.modules.profile.dto.ProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileVO> get() {
        return ApiResponse.ok(profileService.get());
    }

    @PutMapping
    public ApiResponse<ProfileVO> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(profileService.update(request));
    }
}
