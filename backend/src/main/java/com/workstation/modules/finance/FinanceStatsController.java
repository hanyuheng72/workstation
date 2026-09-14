package com.workstation.modules.finance;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.finance.dto.CategoryStatVO;
import com.workstation.modules.finance.dto.FinanceOverviewVO;
import com.workstation.modules.finance.dto.PeriodAmountVO;
import com.workstation.modules.finance.entity.TransactionType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance/stats")
public class FinanceStatsController {

    private final FinanceService financeService;
    private final FinanceStatsService statsService;

    public FinanceStatsController(FinanceService financeService, FinanceStatsService statsService) {
        this.financeService = financeService;
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public ApiResponse<FinanceOverviewVO> overview() {
        return ApiResponse.ok(financeService.overview());
    }

    /** month 省略时取当前月 */
    @GetMapping("/category")
    public ApiResponse<List<CategoryStatVO>> category(
            @RequestParam(defaultValue = "EXPENSE") TransactionType type,
            @RequestParam(required = false) String month) {
        return ApiResponse.ok(statsService.categoryStats(type, FinanceStatsService.parseMonth(month)));
    }

    @GetMapping("/daily-trend")
    public ApiResponse<List<PeriodAmountVO>> dailyTrend(@RequestParam(required = false) String month) {
        return ApiResponse.ok(statsService.dailyTrend(FinanceStatsService.parseMonth(month)));
    }

    @GetMapping("/monthly-trend")
    public ApiResponse<List<PeriodAmountVO>> monthlyTrend(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok(statsService.monthlyTrend(year));
    }
}
