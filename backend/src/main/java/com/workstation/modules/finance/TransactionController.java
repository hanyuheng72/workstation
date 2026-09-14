package com.workstation.modules.finance;

import com.workstation.common.result.ApiResponse;
import com.workstation.common.result.PageResult;
import com.workstation.modules.finance.dto.TransactionRequest;
import com.workstation.modules.finance.dto.TransactionVO;
import com.workstation.modules.finance.entity.TransactionType;
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

@RestController
@RequestMapping("/api/finance/transactions")
public class TransactionController {

    private final FinanceService financeService;

    public TransactionController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    public ApiResponse<PageResult<TransactionVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(financeService.listTransactions(start, end, type, categoryId, page, size));
    }

    @PostMapping
    public ApiResponse<TransactionVO> create(@Valid @RequestBody TransactionRequest request) {
        return ApiResponse.ok(financeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionVO> update(@PathVariable Long id,
                                             @Valid @RequestBody TransactionRequest request) {
        return ApiResponse.ok(financeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        financeService.delete(id);
        return ApiResponse.ok();
    }
}
