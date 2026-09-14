package com.workstation.modules.finance;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.finance.dto.CategoryCreateRequest;
import com.workstation.modules.finance.dto.CategoryVO;
import com.workstation.modules.finance.entity.TransactionType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance/categories")
public class CategoryController {

    private final FinanceService financeService;

    public CategoryController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    public ApiResponse<List<CategoryVO>> list(@RequestParam(required = false) TransactionType type) {
        return ApiResponse.ok(financeService.listCategories(type));
    }

    @PostMapping
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.ok(financeService.createCategory(request.name(), request.type(), request.icon()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        financeService.deleteCategory(id);
        return ApiResponse.ok();
    }
}
