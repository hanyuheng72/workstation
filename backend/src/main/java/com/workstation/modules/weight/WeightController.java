package com.workstation.modules.weight;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.weight.dto.BmiVO;
import com.workstation.modules.weight.dto.TrendPointVO;
import com.workstation.modules.weight.dto.WeightStatsVO;
import com.workstation.modules.weight.dto.WeightUpsertRequest;
import com.workstation.modules.weight.dto.WeightVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weights")
public class WeightController {

    private final WeightService weightService;

    public WeightController(WeightService weightService) {
        this.weightService = weightService;
    }

    @GetMapping
    public ApiResponse<List<WeightVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.ok(weightService.list(start, end));
    }

    @GetMapping("/latest")
    public ApiResponse<WeightVO> latest() {
        return ApiResponse.ok(weightService.latest());
    }

    @GetMapping("/stats")
    public ApiResponse<WeightStatsVO> stats() {
        return ApiResponse.ok(weightService.stats());
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendPointVO>> trend(
            @RequestParam(defaultValue = "day") String granularity) {
        return ApiResponse.ok(weightService.trend(TrendGranularity.parse(granularity)));
    }

    @GetMapping("/bmi")
    public ApiResponse<BmiVO> bmi() {
        return ApiResponse.ok(weightService.bmi());
    }

    @PostMapping
    public ApiResponse<WeightVO> upsert(@Valid @RequestBody WeightUpsertRequest request) {
        return ApiResponse.ok(weightService.upsert(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WeightVO> update(@PathVariable Long id,
                                        @Valid @RequestBody WeightUpsertRequest request) {
        return ApiResponse.ok(weightService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        weightService.delete(id);
        return ApiResponse.ok();
    }
}
