package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.common.PageData;
import com.enpenseSystem.dto.ReimbursementPageQuery;
import com.enpenseSystem.dto.ReimbursementPageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reimbursements")
public class TravelReimbursementController {

    @GetMapping("/page")
    public Result page(@ModelAttribute ReimbursementPageQuery query) {
        // TODO 实现报销单分页查询：
        // 1. fk_reim_main 作为主表。
        // 2. reimNo/title/reason 使用模糊查询。
        // 3. 报销部门、报销人、业务类型使用精确查询。
        // 4. 费用归属公司从 fk_reim_allocation 查询，限定 allocation_owner_type = COMPANY。
        return Result.ok(PageData.<ReimbursementPageVO>empty(query.getPageNum(), query.getPageSize()));
    }
}
