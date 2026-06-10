package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.service.ReimbursementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return Result.ok(reimbursementService.personalStatistics(reimburserName, reimburserNo));
    }
}
