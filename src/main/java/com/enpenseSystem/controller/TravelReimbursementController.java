package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.dto.AllowanceGenerateRequest;
import com.enpenseSystem.dto.ReimbursementPageQuery;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.service.ReimbursementService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reimbursements")
public class TravelReimbursementController {

    private final ReimbursementService reimbursementService;

    public TravelReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @GetMapping("/page")
    public Result page(@ModelAttribute ReimbursementPageQuery query) {
        return Result.ok(reimbursementService.page(query));
    }

    @GetMapping("/{reimNo}")
    public Result detail(@PathVariable String reimNo) {
        return Result.ok(reimbursementService.detail(reimNo));
    }

    @PostMapping("/draft")
    public Result createDraft(@RequestBody ReimbursementSaveRequest request) {
        return Result.ok(reimbursementService.createDraft(request));
    }

    @PostMapping("/submit")
    public Result createAndSubmit(@RequestBody ReimbursementSaveRequest request) {
        return Result.ok(reimbursementService.createAndSubmit(request));
    }

    @PutMapping("/{reimNo}")
    public Result update(@PathVariable String reimNo, @RequestBody ReimbursementSaveRequest request) {
        return Result.ok(reimbursementService.update(reimNo, request));
    }

    @PostMapping("/{reimNo}/submit")
    public Result submitDraft(@PathVariable String reimNo) {
        return Result.ok(reimbursementService.submitDraft(reimNo));
    }

    @PostMapping("/{reimNo}/copy")
    public Result copy(@PathVariable String reimNo) {
        return Result.ok(reimbursementService.copy(reimNo));
    }

    @DeleteMapping("/{reimNo}")
    public Result deleteDraft(@PathVariable String reimNo) {
        reimbursementService.deleteDraft(reimNo);
        return Result.ok();
    }

    @PostMapping("/{reimNo}/void")
    public Result voidBill(@PathVariable String reimNo) {
        reimbursementService.voidBill(reimNo);
        return Result.ok();
    }

    @PostMapping("/allowance-days/generate")
    public Result generateAllowanceDays(@RequestBody AllowanceGenerateRequest request) {
        return Result.ok(reimbursementService.generateAllowanceDays(request));
    }
}
