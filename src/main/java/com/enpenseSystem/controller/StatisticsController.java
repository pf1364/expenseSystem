package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.service.ReimbursementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报销统计接口。
 *
 * <p>当前提供个人维度统计，供前端柱状图、公司占比饼图和最近单据列表使用。</p>
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final ReimbursementService reimbursementService;

    public StatisticsController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @GetMapping("/personal")
    public Result personal(@RequestParam(required = false) String reimburserName,
                           @RequestParam(required = false) String reimburserNo) {
        // 姓名或工号至少传一个；具体校验与统计口径由 Service 负责。
        return Result.ok(reimbursementService.personalStatistics(reimburserName, reimburserNo));
    }
}
