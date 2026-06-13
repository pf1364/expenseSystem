package com.enpenseSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应体，返回当前登录用户的基本信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String username;
    private String displayName;
}
