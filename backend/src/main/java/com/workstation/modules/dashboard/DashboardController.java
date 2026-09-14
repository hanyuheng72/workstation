package com.workstation.modules.dashboard;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.dashboard.dto.DashboardOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 首页一次拉全：避免四张卡片各自到达造成页面零散跳动 */
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview() {
        return ApiResponse.ok(dashboardService.overview());
    }
}
