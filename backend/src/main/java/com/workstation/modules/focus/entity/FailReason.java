package com.workstation.modules.focus.entity;

/**
 * 专注失败的原因。只有 FAILED 的场次才有值。
 */
public enum FailReason {

    /** 那一次暂停已经用掉，再按暂停就整场判失败 */
    PAUSE_EXHAUSTED,

    /** 自己按了放弃 */
    ABANDONED
}
