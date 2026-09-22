package com.workstation.modules.focus;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.focus.dto.FocusSessionVO;
import com.workstation.modules.focus.dto.FocusStartRequest;
import com.workstation.modules.focus.dto.FocusStatsVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 自习室。界面上只有两个地方会用到：
 *   首页右上角那个入口，和自习室自己的页面。
 *
 * 一场专注的生命周期全部走 POST /{id}/xxx，因为每一步都在改状态而不是读资源。
 * 回到页面时先 GET /active 把倒计时对齐——离开期间时间照走，续算是服务端算的。
 */
@RestController
@RequestMapping("/api/focus")
public class FocusController {

    private final FocusService focusService;

    public FocusController(FocusService focusService) {
        this.focusService = focusService;
    }

    /** 进行中的那一场，没有则 data 为 null */
    @GetMapping("/active")
    public ApiResponse<FocusSessionVO> active() {
        return ApiResponse.ok(focusService.active());
    }

    @GetMapping("/sessions")
    public ApiResponse<List<FocusSessionVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(focusService.list(from, to));
    }

    @GetMapping("/stats")
    public ApiResponse<FocusStatsVO> stats() {
        return ApiResponse.ok(focusService.stats());
    }

    @PostMapping("/sessions")
    public ApiResponse<FocusSessionVO> start(@Valid @RequestBody FocusStartRequest request) {
        return ApiResponse.ok(focusService.start(request));
    }

    /** 机会已经用掉时调这个接口，返回的是失败态而不是暂停态 */
    @PostMapping("/sessions/{id}/pause")
    public ApiResponse<FocusSessionVO> pause(@PathVariable Long id) {
        return ApiResponse.ok(focusService.pause(id));
    }

    @PostMapping("/sessions/{id}/resume")
    public ApiResponse<FocusSessionVO> resume(@PathVariable Long id) {
        return ApiResponse.ok(focusService.resume(id));
    }

    @PostMapping("/sessions/{id}/complete")
    public ApiResponse<FocusSessionVO> complete(@PathVariable Long id) {
        return ApiResponse.ok(focusService.complete(id));
    }

    /** 提前学完了：没到时间也能结束，一样记成功。不足 1 分钟会被拒 */
    @PostMapping("/sessions/{id}/finish-early")
    public ApiResponse<FocusSessionVO> finishEarly(@PathVariable Long id) {
        return ApiResponse.ok(focusService.finishEarly(id));
    }

    @PostMapping("/sessions/{id}/abandon")
    public ApiResponse<FocusSessionVO> abandon(@PathVariable Long id) {
        return ApiResponse.ok(focusService.abandon(id));
    }
}
