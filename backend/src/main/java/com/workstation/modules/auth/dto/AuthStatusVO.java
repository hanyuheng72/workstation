package com.workstation.modules.auth.dto;

/** 前端启动时问一次：这个部署要不要登录 */
public record AuthStatusVO(boolean authRequired) {
}
