package com.workstation.common.util;

import com.workstation.modules.weight.dto.BmiBandVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * BMI 相关计算与分级。
 *
 * 分级采用中国标准（WS/T 428-2013），和 WHO 国际标准不同：
 * 国内超重线是 24、肥胖线是 28，比 WHO 的 25 / 30 更严。
 */
public final class BmiUtil {

    public static final BigDecimal DEFAULT_HEIGHT_CM = new BigDecimal("173");

    /** 刻度尺两端。低于 15 或高于 35 都挤在端点显示，不做无限延伸 */
    public static final BigDecimal SCALE_MIN = new BigDecimal("15");
    public static final BigDecimal SCALE_MAX = new BigDecimal("35");

    private static final BigDecimal THIN_MAX = new BigDecimal("18.5");
    private static final BigDecimal NORMAL_MAX = new BigDecimal("24");
    private static final BigDecimal OVERWEIGHT_MAX = new BigDecimal("28");

    /**
     * 换算「健康体重范围」时用的上下界。
     * 上界取 23.9 而不是 24：BMI 恰好 24.0 会被判成「超重」，
     * 若健康上限按 24 算，推荐范围里就会包含一个被标为超重的体重，自相矛盾。
     */
    private static final BigDecimal HEALTHY_MIN_BMI = new BigDecimal("18.5");
    private static final BigDecimal HEALTHY_MAX_BMI = new BigDecimal("23.9");

    private BmiUtil() {
    }

    /** BMI = 体重(kg) / 身高(m)² */
    public static BigDecimal calculate(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null || heightCm.signum() <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    public static String category(BigDecimal bmi) {
        if (bmi == null) {
            return null;
        }
        if (bmi.compareTo(THIN_MAX) < 0) {
            return "偏瘦";
        }
        if (bmi.compareTo(NORMAL_MAX) < 0) {
            return "正常";
        }
        if (bmi.compareTo(OVERWEIGHT_MAX) < 0) {
            return "超重";
        }
        return "肥胖";
    }

    /** 刻度尺上的分区，前端按顺序渲染成一条彩色长条 */
    public static List<BmiBandVO> bands() {
        return List.of(
                new BmiBandVO("THIN", "偏瘦", SCALE_MIN, THIN_MAX),
                new BmiBandVO("NORMAL", "正常", THIN_MAX, NORMAL_MAX),
                new BmiBandVO("OVERWEIGHT", "超重", NORMAL_MAX, OVERWEIGHT_MAX),
                new BmiBandVO("OBESE", "肥胖", OVERWEIGHT_MAX, SCALE_MAX));
    }

    /** 对应身高下 BMI 处在正常区间所对应的体重范围 */
    public static BigDecimal healthyWeightMin(BigDecimal heightCm) {
        return weightFor(heightCm, HEALTHY_MIN_BMI);
    }

    public static BigDecimal healthyWeightMax(BigDecimal heightCm) {
        return weightFor(heightCm, HEALTHY_MAX_BMI);
    }

    private static BigDecimal weightFor(BigDecimal heightCm, BigDecimal bmi) {
        if (heightCm == null || heightCm.signum() <= 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return bmi.multiply(heightM.multiply(heightM)).setScale(1, RoundingMode.HALF_UP);
    }
}
