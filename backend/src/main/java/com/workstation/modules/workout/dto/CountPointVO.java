package com.workstation.modules.workout.dto;

/** 计数型趋势点（训练次数），和体重那种带小数的趋势分开 */
public record CountPointVO(String key, String label, long value) {
}
