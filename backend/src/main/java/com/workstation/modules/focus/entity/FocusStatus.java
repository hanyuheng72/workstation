package com.workstation.modules.focus.entity;

/**
 * 一场专注的状态。RUNNING / PAUSED 都算「进行中」，同时只允许存在一场。
 */
public enum FocusStatus {

    /** 倒计时进行中 */
    RUNNING,

    /** 暂停中，倒计时不走；暂停时长的尾巴由 pausedSeconds 记账 */
    PAUSED,

    /** 走满了设定时长 */
    SUCCESS,

    /** 用尽暂停机会还想再暂停，或自己放弃 */
    FAILED;

    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }
}
