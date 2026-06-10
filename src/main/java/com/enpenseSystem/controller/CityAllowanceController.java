package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.service.ReimbursementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/city-allowances")
public class CityAllowanceController {

    private final ReimbursementService reimbursementService;

    public CityAllowanceController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @GetMapping
    public Result list() {
        return Result.ok(reimbursementService.listCityAllowances());
    }
}
