package com.enpenseSystem.controller;

import com.enpenseSystem.common.Result;
import com.enpenseSystem.dto.AllowanceGenerateRequest;
import com.enpenseSystem.dto.ReimbursementEditLockRequest;
import com.enpenseSystem.dto.ReimbursementPageQuery;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.dto.ReimbursementVersionRequest;
import com.enpenseSystem.dto.SubmitGroup;
import com.enpenseSystem.entity.SysUser;
import com.enpenseSystem.service.ReimbursementEditLockService;
import com.enpenseSystem.service.ReimbursementService;
import com.enpenseSystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 差旅报销单 HTTP 接口入口。
 *
 * <p>本类负责把 URL、查询参数和 JSON 请求体转换为 Java 对象，
 * 再调用 {@link ReimbursementService} 完成业务。所有响应统一包装为
 * {@link Result}，业务校验和数据库事务不放在 Controller 中。</p>
 */
@RestController
@RequestMapping("/api/reimbursements")
public class TravelReimbursementController {

    /** 报销单核心业务服务。 */
    private final ReimbursementService reimbursementService;
    /** 编辑锁服务，防止多人同时编辑冲突。 */
    private final ReimbursementEditLockService editLockService;
    /** 用户服务，获取当前登录用户信息。 */
    private final UserService userService;

    public TravelReimbursementController(ReimbursementService reimbursementService,
                                          ReimbursementEditLockService editLockService,
                                          UserService userService) {
        this.reimbursementService = reimbursementService;
        this.editLockService = editLockService;
        this.userService = userService;
    }

    @GetMapping("/page")
    public Result page(@ModelAttribute ReimbursementPageQuery query) {
        // GET 查询参数由 Spring 绑定为 ReimbursementPageQuery。
        return Result.ok(reimbursementService.page(query));
    }

    /** 查询一张报销单的主表、行程、每日补助和费用分摊。 */
    @GetMapping("/{reimNo}")
    public Result detail(@PathVariable String reimNo) {
        return Result.ok(reimbursementService.detail(reimNo));
    }

    /** 保存草稿；草稿可不完整，但已填写金额仍需通过后端校验。 */
    @PostMapping("/draft")
    public Result createDraft(@RequestBody ReimbursementSaveRequest request) {
        return Result.ok(reimbursementService.createDraft(request));
    }

    /** 创建完整报销单并直接提交。提交时触发 Bean Validation。 */
    @PostMapping("/submit")
    public Result createAndSubmit(@RequestBody @Validated(SubmitGroup.class) ReimbursementSaveRequest request) {
        return Result.ok(reimbursementService.createAndSubmit(request));
    }

    /** 用前端传来的完整表单状态同步指定报销单及其子表。 */
    @PutMapping("/{reimNo}")
    public Result update(@PathVariable String reimNo, @RequestBody ReimbursementSaveRequest request) {
        return Result.ok(reimbursementService.update(reimNo, request));
    }

    /** 对数据库中已经存在的草稿执行完整校验并提交。前端需回传详情接口给出的 version。 */
    @PostMapping("/{reimNo}/submit")
    public Result submitDraft(@PathVariable String reimNo,
                               @RequestBody(required = false) ReimbursementVersionRequest request) {
        Integer version = request != null ? request.getVersion() : null;
        return Result.ok(reimbursementService.submitDraft(reimNo, version));
    }

    /** 将任意现有报销单深度复制为一张新的草稿。 */
    @PostMapping("/{reimNo}/copy")
    public Result copy(@PathVariable String reimNo) {
        return Result.ok(reimbursementService.copy(reimNo));
    }

    /** 物理删除草稿及其行程、每日补助和费用分摊。 */
    @DeleteMapping("/{reimNo}")
    public Result deleteDraft(@PathVariable String reimNo) {
        reimbursementService.deleteDraft(reimNo);
        return Result.ok();
    }

    /** 将非草稿报销单改为已作废状态，保留历史数据。 */
    @PostMapping("/{reimNo}/void")
    public Result voidBill(@PathVariable String reimNo) {
        reimbursementService.voidBill(reimNo);
        return Result.ok();
    }

    /** 根据日期范围和目的地城市标准生成逐日补助。 */
    @PostMapping("/allowance-days/generate")
    public Result generateAllowanceDays(@RequestBody AllowanceGenerateRequest request) {
        return Result.ok(reimbursementService.generateAllowanceDays(request));
    }

    /** 查询全部城市补助标准。 */
    @GetMapping("/city-allowances")
    public Result listCityAllowances() {
        return Result.ok(reimbursementService.listCityAllowances());
    }

    /** 查询指定报销人的个人统计。 */
    @GetMapping("/personal-statistics")
    public Result personalStatistics(@RequestParam(required = false) String reimburserName,
                                      @RequestParam(required = false) String reimburserNo) {
        return Result.ok(reimbursementService.personalStatistics(reimburserName, reimburserNo));
    }

    // ======================== 编辑锁接口 ========================

    /** 尝试获得报销单编辑锁。只有草稿状态可以加锁。 */
    @PostMapping("/{reimNo}/lock")
    public Result tryLock(@PathVariable String reimNo, HttpServletRequest request) {
        SysUser user = userService.getCurrentUser(request);
        return Result.ok(editLockService.tryLock(reimNo, user));
    }

    /** 续期当前用户持有的编辑锁。 */
    @PutMapping("/{reimNo}/lock")
    public Result renewLock(@PathVariable String reimNo,
                             @RequestBody ReimbursementEditLockRequest lockRequest,
                             HttpServletRequest request) {
        SysUser user = userService.getCurrentUser(request);
        return Result.ok(editLockService.renew(reimNo, lockRequest.getLockToken(), user));
    }

    /** 释放当前用户持有的编辑锁。 */
    @DeleteMapping("/{reimNo}/lock")
    public Result unlock(@PathVariable String reimNo,
                          @RequestBody ReimbursementEditLockRequest lockRequest,
                          HttpServletRequest request) {
        SysUser user = userService.getCurrentUser(request);
        return Result.ok(editLockService.unlock(reimNo, lockRequest.getLockToken(), user));
    }

    /** 查询报销单当前编辑锁状态（持锁人信息等）。 */
    @GetMapping("/{reimNo}/lock")
    public Result getLockInfo(@PathVariable String reimNo) {
        return Result.ok(editLockService.getLockInfo(reimNo));
    }
}
