package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.common.PageData;
import com.enpenseSystem.dto.ReimbursementPageQuery;
import com.enpenseSystem.dto.ReimbursementPageVO;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.dto.ReimbursementSaveResponse;
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

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_DRAFT_NAME = "草稿";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_SUBMITTED_NAME = "已提交";

    @GetMapping("/page")
    public Result page(@ModelAttribute ReimbursementPageQuery query) {
        // TODO 实现报销单分页查询：
        // 1. fk_reim_main 作为主表。
        // 2. reimNo/title/reason 使用模糊查询。
        // 3. 报销部门、报销人、业务类型使用精确查询。
        // 4. 费用归属公司从 fk_reim_allocation 查询，限定 allocation_owner_type = COMPANY。
        return Result.ok(PageData.<ReimbursementPageVO>empty(query.getPageNum(), query.getPageSize()));
    }

    @PostMapping("/submit")
    public Result createAndSubmit(@RequestBody ReimbursementSaveRequest request) {
        // TODO 创建完整报销单数据，写入主表、行程表、每日补助明细表、分摊表，并设置状态为已提交。
        // TODO 创建时 reimNo 可为空，后端生成唯一报销单号。
        return Result.ok(new ReimbursementSaveResponse(request.getReimNo(), STATUS_SUBMITTED, STATUS_SUBMITTED_NAME));
    }

    @PostMapping("/draft")
    public Result createDraft(@RequestBody ReimbursementSaveRequest request) {
        // TODO 创建草稿，写入用户已经填写的单据信息，并设置状态为草稿。
        // TODO 创建时 reimNo 可为空，后端生成唯一报销单号。
        return Result.ok(new ReimbursementSaveResponse(request.getReimNo(), STATUS_DRAFT, STATUS_DRAFT_NAME));
    }

    @PutMapping("/{reimNo}")
    public Result update(@PathVariable String reimNo, @RequestBody ReimbursementSaveRequest request) {
        // TODO 根据 reimNo 更新已有草稿单据。
        // TODO 可根据 request 中的目标状态决定继续保存草稿，还是修改后提交。
        request.setReimNo(reimNo);
        return Result.ok(new ReimbursementSaveResponse(reimNo, STATUS_DRAFT, STATUS_DRAFT_NAME));
    }
}
