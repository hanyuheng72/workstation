package com.workstation.modules.finance;

import com.workstation.common.exception.BusinessException;
import com.workstation.modules.finance.dto.CategoryStatRow;
import com.workstation.modules.finance.dto.CategoryStatVO;
import com.workstation.modules.finance.dto.PeriodAmountVO;
import com.workstation.modules.finance.dto.TypeAmountRow;
import com.workstation.modules.finance.entity.Category;
import com.workstation.modules.finance.entity.TransactionType;
import com.workstation.modules.finance.mapper.CategoryMapper;
import com.workstation.modules.finance.mapper.TransactionRecordMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinanceStatsService {

    private final TransactionRecordMapper transactionMapper;
    private final CategoryMapper categoryMapper;

    public FinanceStatsService(TransactionRecordMapper transactionMapper, CategoryMapper categoryMapper) {
        this.transactionMapper = transactionMapper;
        this.categoryMapper = categoryMapper;
    }

    /** 分类占比。没有任何记录时返回空列表，前端显示空状态而不是空饼图 */
    public List<CategoryStatVO> categoryStats(TransactionType type, YearMonth month) {
        YearMonth target = month == null ? YearMonth.now() : month;
        List<CategoryStatRow> rows = transactionMapper.selectCategoryStats(
                type.name(), target.atDay(1), target.atEndOfMonth());
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, Category> categories = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        BigDecimal total = rows.stream()
                .map(CategoryStatRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream().map(row -> {
            Category category = categories.get(row.categoryId());
            BigDecimal percent = total.signum() == 0
                    ? BigDecimal.ZERO
                    : row.amount().multiply(BigDecimal.valueOf(100))
                            .divide(total, 1, RoundingMode.HALF_UP);
            return new CategoryStatVO(
                    row.categoryId(),
                    category == null ? null : category.getCode(),
                    category == null ? "已删除的分类" : category.getName(),
                    category == null ? null : category.getIcon(),
                    row.amount(),
                    percent);
        }).toList();
    }

    /** 每日收支趋势。没有记录的日期补 0，否则折线会跳过空白日期，看趋势会失真 */
    public List<PeriodAmountVO> dailyTrend(YearMonth month) {
        YearMonth target = month == null ? YearMonth.now() : month;
        Map<String, TypeAmountRow> index = indexByLabel(
                transactionMapper.selectAmountByPeriod("%Y-%m-%d", target.atDay(1), target.atEndOfMonth()));

        List<PeriodAmountVO> points = new ArrayList<>();
        for (int day = 1; day <= target.lengthOfMonth(); day++) {
            LocalDate date = target.atDay(day);
            String key = date.toString();
            points.add(new PeriodAmountVO(key, String.valueOf(day),
                    amountOf(index, key, TransactionType.INCOME),
                    amountOf(index, key, TransactionType.EXPENSE)));
        }
        return points;
    }

    /** 每月收支趋势，整年 12 个点，没有记录的月份补 0 */
    public List<PeriodAmountVO> monthlyTrend(Integer year) {
        int target = year == null ? LocalDate.now().getYear() : year;
        Map<String, TypeAmountRow> index = indexByLabel(
                transactionMapper.selectAmountByPeriod("%Y-%m",
                        LocalDate.of(target, 1, 1), LocalDate.of(target, 12, 31)));

        List<PeriodAmountVO> points = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            String key = String.format("%d-%02d", target, month);
            points.add(new PeriodAmountVO(key, month + "月",
                    amountOf(index, key, TransactionType.INCOME),
                    amountOf(index, key, TransactionType.EXPENSE)));
        }
        return points;
    }

    /** 解析 yyyy-MM，空值返回 null（由调用方取当前月） */
    public static YearMonth parseMonth(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw BusinessException.badRequest("month 格式应为 yyyy-MM，例如 2026-09");
        }
    }

    private static Map<String, TypeAmountRow> indexByLabel(List<TypeAmountRow> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> row.label() + "|" + row.type(),
                Function.identity(),
                (first, second) -> first));
    }

    private static BigDecimal amountOf(Map<String, TypeAmountRow> index, String label, TransactionType type) {
        TypeAmountRow row = index.get(label + "|" + type.name());
        return row == null ? BigDecimal.ZERO : row.amount();
    }
}
