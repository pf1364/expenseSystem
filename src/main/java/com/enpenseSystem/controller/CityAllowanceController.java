package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.service.ReimbursementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 城市补助标准查询接口。
 *
 * <p>城市标准是每日补助生成和后端金额上限校验的权威来源。</p>
 */
@RestController
@RequestMapping("/api/city-allowances")
public class CityAllowanceController {

    private final ReimbursementService reimbursementService;

    public CityAllowanceController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @GetMapping
    public Result list() {
        // 返回全部城市标准，实际排序和数据库查询由业务服务处理。
        return Result.ok(reimbursementService.listCityAllowances());
    }
}
