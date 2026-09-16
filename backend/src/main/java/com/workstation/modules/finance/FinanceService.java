package com.workstation.modules.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workstation.common.exception.BusinessException;
import com.workstation.common.result.PageResult;
import com.workstation.modules.finance.dto.CategoryVO;
import com.workstation.modules.finance.dto.FinanceOverviewVO;
import com.workstation.modules.finance.dto.TransactionRequest;
import com.workstation.modules.finance.dto.TransactionVO;
import com.workstation.modules.finance.entity.Category;
import com.workstation.modules.finance.entity.TransactionRecord;
import com.workstation.modules.finance.entity.TransactionType;
import com.workstation.modules.finance.mapper.CategoryMapper;
import com.workstation.modules.finance.mapper.TransactionRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private final CategoryMapper categoryMapper;
    private final TransactionRecordMapper transactionMapper;

    public FinanceService(CategoryMapper categoryMapper, TransactionRecordMapper transactionMapper) {
        this.categoryMapper = categoryMapper;
        this.transactionMapper = transactionMapper;
    }

    // ---------------- 分类 ----------------

    public List<CategoryVO> listCategories(TransactionType type) {
        return categoryMapper.selectList(
                        Wrappers.lambdaQuery(Category.class)
                                .eq(type != null, Category::getType, type)
                                .orderByAsc(Category::getType)
                                .orderByAsc(Category::getSortOrder))
                .stream().map(FinanceService::toCategoryVO).toList();
    }

    @Transactional
    public CategoryVO createCategory(String name, TransactionType type, String icon) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw BusinessException.badRequest("请填写分类名称");
        }
        if (trimmed.length() > 20) {
            throw BusinessException.badRequest("分类名称最多 20 字");
        }

        Category existing = categoryMapper.selectOne(
                Wrappers.lambdaQuery(Category.class).eq(Category::getName, trimmed).eq(Category::getType, type));
        if (existing != null) {
            throw BusinessException.conflict("这个分类已经存在了");
        }

        Category category = new Category();
        category.setCode("CUSTOM_" + type.name() + "_" + System.currentTimeMillis());
        category.setName(trimmed);
        category.setType(type);
        category.setIcon(icon);
        category.setSortOrder(999);
        category.setIsSystem(false);
        categoryMapper.insert(category);
        return toCategoryVO(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("找不到这个分类");
        }
        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw BusinessException.badRequest("内置分类不能删除");
        }
        Long used = transactionMapper.selectCount(
                Wrappers.lambdaQuery(TransactionRecord.class).eq(TransactionRecord::getCategoryId, id));
        if (used != null && used > 0) {
            throw BusinessException.badRequest("这个分类下还有 " + used + " 笔记录，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 按分类编码找分类，供 AI 解析结果使用。
     * 编码不认识时退化到该方向的「其他」，保证一笔账总能记下去——
     * 宁可分类粗一点，也不要因为分类没认出来就把用户的话丢掉。
     */
    public CategoryVO findCategoryByCode(String code, TransactionType type) {
        Category category = null;
        if (code != null && !code.isBlank()) {
            category = categoryMapper.selectOne(
                    Wrappers.lambdaQuery(Category.class)
                            .eq(Category::getCode, code.trim().toUpperCase())
                            .last("LIMIT 1"));
        }
        if (category == null) {
            String fallback = type == TransactionType.EXPENSE ? "OTHER_EXPENSE" : "OTHER_INCOME";
            category = categoryMapper.selectOne(
                    Wrappers.lambdaQuery(Category.class)
                            .eq(Category::getCode, fallback)
                            .last("LIMIT 1"));
        }
        if (category == null) {
            throw BusinessException.notFound("找不到可用的分类，请先在记账页确认分类字典");
        }
        return toCategoryVO(category);
    }

    // ---------------- 流水 ----------------

    public PageResult<TransactionVO> listTransactions(LocalDate start, LocalDate end, TransactionType type,
                                                      Long categoryId, long page, long size) {
        IPage<TransactionRecord> result = transactionMapper.selectPage(
                Page.of(page, size),
                Wrappers.lambdaQuery(TransactionRecord.class)
                        .ge(start != null, TransactionRecord::getOccurDate, start)
                        .le(end != null, TransactionRecord::getOccurDate, end)
                        .eq(type != null, TransactionRecord::getType, type)
                        .eq(categoryId != null, TransactionRecord::getCategoryId, categoryId)
                        .orderByDesc(TransactionRecord::getOccurDate)
                        .orderByDesc(TransactionRecord::getOccurTime)
                        .orderByDesc(TransactionRecord::getId));

        Map<Long, Category> categories = categoryIndex();
        List<TransactionVO> list = result.getRecords().stream()
                .map(record -> toTransactionVO(record, categories.get(record.getCategoryId())))
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public TransactionVO create(TransactionRequest request) {
        Category category = requireMatchingCategory(request.categoryId(), request.type());

        TransactionRecord record = new TransactionRecord();
        applyRequest(record, request);
        transactionMapper.insert(record);
        return toTransactionVO(record, category);
    }

    @Transactional
    public TransactionVO update(Long id, TransactionRequest request) {
        TransactionRecord record = requireTransaction(id);
        Category category = requireMatchingCategory(request.categoryId(), request.type());
        applyRequest(record, request);
        transactionMapper.updateById(record);
        return toTransactionVO(record, category);
    }

    @Transactional
    public void delete(Long id) {
        requireTransaction(id);
        transactionMapper.deleteById(id);
    }

    // ---------------- 概览 ----------------

    public FinanceOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays((today.getDayOfWeek().getValue() + 6) % 7);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal todayExpense = sum(TransactionType.EXPENSE, today, today);
        BigDecimal weekExpense = sum(TransactionType.EXPENSE, weekStart, today);
        BigDecimal monthIncome = sum(TransactionType.INCOME, monthStart, monthEnd);
        BigDecimal monthExpense = sum(TransactionType.EXPENSE, monthStart, monthEnd);

        return new FinanceOverviewVO(today, todayExpense, weekExpense, monthIncome, monthExpense,
                monthIncome.subtract(monthExpense));
    }

    private BigDecimal sum(TransactionType type, LocalDate from, LocalDate to) {
        BigDecimal total = transactionMapper.sumAmount(type.name(), from, to);
        return total == null ? BigDecimal.ZERO : total;
    }

    /**
     * 指定月份的收入与支出合计。
     * 给 AI 助手用——它需要上个月的数字才能回答「比上个月多花多少」这类问题。
     */
    public MonthAmounts monthAmounts(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        return new MonthAmounts(month.toString(),
                sum(TransactionType.INCOME, from, to),
                sum(TransactionType.EXPENSE, from, to));
    }

    public record MonthAmounts(String month, BigDecimal income, BigDecimal expense) {
    }

    // ---------------- 内部 ----------------

    private void applyRequest(TransactionRecord record, TransactionRequest request) {
        record.setType(request.type());
        record.setAmount(request.amount());
        record.setCategoryId(request.categoryId());
        record.setOccurDate(request.occurDate() == null ? LocalDate.now() : request.occurDate());
        record.setOccurTime(request.occurTime() == null ? LocalTime.now().withNano(0) : request.occurTime());
        record.setNote(request.note());
    }

    /** 分类必须存在，且收支方向要和记录一致——否则会出现「支出」记到「工资」上的脏数据 */
    private Category requireMatchingCategory(Long categoryId, TransactionType type) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.notFound("找不到这个分类");
        }
        if (category.getType() != type) {
            throw BusinessException.badRequest("「" + category.getName() + "」不属于"
                    + (type == TransactionType.EXPENSE ? "支出" : "收入") + "分类");
        }
        return category;
    }

    private TransactionRecord requireTransaction(Long id) {
        TransactionRecord record = transactionMapper.selectById(id);
        if (record == null) {
            throw BusinessException.notFound("找不到这笔记录");
        }
        return record;
    }

    private Map<Long, Category> categoryIndex() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private static CategoryVO toCategoryVO(Category category) {
        return new CategoryVO(category.getId(), category.getCode(), category.getName(), category.getType(),
                category.getIcon(), category.getSortOrder(), category.getIsSystem());
    }

    private static TransactionVO toTransactionVO(TransactionRecord record, Category category) {
        return new TransactionVO(
                record.getId(),
                record.getType(),
                record.getAmount(),
                record.getCategoryId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                record.getOccurDate(),
                record.getOccurTime(),
                record.getNote());
    }
}
