package com.workstation.modules.weight;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.workstation.common.exception.BusinessException;
import com.workstation.common.util.BmiUtil;
import com.workstation.modules.profile.ProfileService;
import com.workstation.modules.weight.dto.BmiVO;
import com.workstation.modules.weight.dto.TrendPointVO;
import com.workstation.modules.weight.dto.WeightStatsVO;
import com.workstation.modules.weight.dto.WeightUpsertRequest;
import com.workstation.modules.weight.dto.WeightVO;
import com.workstation.modules.weight.entity.WeightRecord;
import com.workstation.modules.weight.mapper.WeightRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class WeightService {

    private final WeightRecordMapper weightRecordMapper;
    private final ProfileService profileService;

    public WeightService(WeightRecordMapper weightRecordMapper, ProfileService profileService) {
        this.weightRecordMapper = weightRecordMapper;
        this.profileService = profileService;
    }

    public List<WeightVO> list(LocalDate start, LocalDate end) {
        List<WeightRecord> records = weightRecordMapper.selectList(
                Wrappers.lambdaQuery(WeightRecord.class)
                        .ge(start != null, WeightRecord::getRecordDate, start)
                        .le(end != null, WeightRecord::getRecordDate, end)
                        .orderByDesc(WeightRecord::getRecordDate));
        return records.stream().map(WeightService::toVO).toList();
    }

    /** 当前体重 = 最新一条记录 */
    public WeightVO latest() {
        WeightRecord record = weightRecordMapper.selectOne(
                Wrappers.lambdaQuery(WeightRecord.class)
                        .orderByDesc(WeightRecord::getRecordDate)
                        .last("LIMIT 1"));
        return record == null ? null : toVO(record);
    }

    /**
     * 一天只保留一条记录，同一天再次提交即为修正，不新增。
     */
    @Transactional
    public WeightVO upsert(WeightUpsertRequest request) {
        LocalDate date = request.recordDate() == null ? LocalDate.now() : request.recordDate();
        WeightRecord existing = findByDate(date);

        if (existing == null) {
            WeightRecord record = new WeightRecord();
            record.setRecordDate(date);
            record.setWeightKg(request.weightKg());
            record.setNote(request.note());
            weightRecordMapper.insert(record);
            return toVO(record);
        }

        existing.setWeightKg(request.weightKg());
        existing.setNote(request.note());
        weightRecordMapper.updateById(existing);
        return toVO(existing);
    }

    @Transactional
    public WeightVO update(Long id, WeightUpsertRequest request) {
        WeightRecord record = requireById(id);
        LocalDate date = request.recordDate() == null ? record.getRecordDate() : request.recordDate();

        if (!date.equals(record.getRecordDate())) {
            WeightRecord conflict = findByDate(date);
            if (conflict != null && !conflict.getId().equals(id)) {
                throw BusinessException.conflict("这一天已经有体重记录了，请直接修改那一条");
            }
            record.setRecordDate(date);
        }

        record.setWeightKg(request.weightKg());
        record.setNote(request.note());
        weightRecordMapper.updateById(record);
        return toVO(record);
    }

    @Transactional
    public void delete(Long id) {
        requireById(id);
        weightRecordMapper.deleteById(id);
    }

    // ---------------- 统计 ----------------

    /** 没有任何记录时返回全 null 的统计对象，前端据此显示空状态 */
    public WeightStatsVO stats() {
        List<WeightRecord> records = weightRecordMapper.selectList(
                Wrappers.lambdaQuery(WeightRecord.class).orderByDesc(WeightRecord::getRecordDate));
        if (records.isEmpty()) {
            return WeightStatsVO.empty();
        }

        BigDecimal max = null;
        BigDecimal min = null;
        BigDecimal sum = BigDecimal.ZERO;
        for (WeightRecord record : records) {
            BigDecimal weight = record.getWeightKg();
            sum = sum.add(weight);
            if (max == null || weight.compareTo(max) > 0) {
                max = weight;
            }
            if (min == null || weight.compareTo(min) < 0) {
                min = weight;
            }
        }

        WeightRecord latest = records.get(0);
        BigDecimal average = sum.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
        BigDecimal delta = records.size() > 1
                ? latest.getWeightKg().subtract(records.get(1).getWeightKg())
                : null;

        return new WeightStatsVO(latest.getWeightKg(), latest.getRecordDate(), max, min,
                average, delta, records.size());
    }

    /**
     * 趋势数据。粒度决定回溯窗口与聚合方式：
     * 日看最近 30 天、月看最近 12 个月（取当月均值）、年看最近 5 年（取当年均值）。
     */
    public List<TrendPointVO> trend(TrendGranularity granularity) {
        LocalDate today = LocalDate.now();
        String format;
        LocalDate from;

        switch (granularity) {
            case MONTH -> {
                format = "%Y-%m";
                from = today.withDayOfMonth(1).minusMonths(11);
            }
            case YEAR -> {
                format = "%Y";
                from = today.withDayOfYear(1).minusYears(4);
            }
            default -> {
                format = "%Y-%m-%d";
                from = today.minusDays(29);
            }
        }

        return weightRecordMapper.selectTrend(format, from).stream()
                .map(row -> new TrendPointVO(row.label(), labelOf(row.label(), granularity), row.value()))
                .toList();
    }

    /** BMI 基于档案里的身高与最新一条体重 */
    public BmiVO bmi() {
        BigDecimal height = profileService.heightCm();
        WeightRecord latest = weightRecordMapper.selectOne(
                Wrappers.lambdaQuery(WeightRecord.class)
                        .orderByDesc(WeightRecord::getRecordDate)
                        .last("LIMIT 1"));

        if (latest == null) {
            return BmiVO.empty(height);
        }

        BigDecimal bmi = BmiUtil.calculate(latest.getWeightKg(), height);
        return new BmiVO(bmi, BmiUtil.category(bmi), height, latest.getWeightKg(),
                BmiUtil.SCALE_MIN, BmiUtil.SCALE_MAX,
                BmiUtil.healthyWeightMin(height), BmiUtil.healthyWeightMax(height),
                BmiUtil.bands());
    }

    private static String labelOf(String key, TrendGranularity granularity) {
        String[] parts = key.split("-");
        return switch (granularity) {
            case MONTH -> Integer.parseInt(parts[1]) + "月";
            case YEAR -> key + "年";
            default -> Integer.parseInt(parts[1]) + "/" + Integer.parseInt(parts[2]);
        };
    }

    private WeightRecord findByDate(LocalDate date) {
        return weightRecordMapper.selectOne(
                Wrappers.lambdaQuery(WeightRecord.class).eq(WeightRecord::getRecordDate, date));
    }

    private WeightRecord requireById(Long id) {
        WeightRecord record = weightRecordMapper.selectById(id);
        if (record == null) {
            throw BusinessException.notFound("找不到这条体重记录");
        }
        return record;
    }

    private static WeightVO toVO(WeightRecord record) {
        return new WeightVO(record.getId(), record.getRecordDate(), record.getWeightKg(), record.getNote());
    }
}
