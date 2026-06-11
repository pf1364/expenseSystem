package com.enpenseSystem.service;

import com.enpenseSystem.common.PageData;
import com.enpenseSystem.dto.AllowanceGenerateRequest;
import com.enpenseSystem.dto.PersonalStatisticsVO;
import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.enpenseSystem.dto.ReimbursementPageQuery;
import com.enpenseSystem.dto.ReimbursementPageVO;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.dto.ReimbursementSaveResponse;
import com.enpenseSystem.entity.FkCityAllowance;

import java.util.List;

/**
 * 差旅报销单核心业务服务。
 *
 * <p>该接口统一定义报销单查询、保存、提交、复制、删除、作废、
 * 补助生成和个人统计等业务能力。Controller 只负责接收 HTTP 请求，
 * 具体的数据校验、事务控制、子表同步和金额计算均由实现类完成。</p>
 */
public interface ReimbursementService {

    /**
     * 按查询条件分页获取报销单列表。
     *
     * @param query 分页信息及报销单号、报销人、状态等筛选条件
     * @return 当前页数据、总记录数和分页信息
     */
    PageData<ReimbursementPageVO> page(ReimbursementPageQuery query);

    /**
     * 查询一张报销单的完整详情。
     *
     * <p>详情包括主表基础信息、行程、每条行程对应的每日补助，
     * 以及费用归属和分摊信息。</p>
     *
     * @param reimNo 报销单号
     * @return 报销单完整详情
     */
    ReimbursementDetailVO detail(String reimNo);

    /**
     * 创建草稿报销单。
     *
     * <p>草稿允许信息不完整，但已经填写的每日补助金额仍会接受
     * 后端安全校验，防止通过接口写入负数或超过标准的金额。</p>
     *
     * @param request 前端填写的报销单数据
     * @return 新报销单号及草稿状态
     */
    ReimbursementSaveResponse createDraft(ReimbursementSaveRequest request);

    /**
     * 创建一张完整报销单并立即提交。
     *
     * @param request 完整的基础信息、行程、每日补助和分摊信息
     * @return 新报销单号及已提交状态
     */
    ReimbursementSaveResponse createAndSubmit(ReimbursementSaveRequest request);

    /**
     * 更新指定报销单及其所有子表数据。
     *
     * @param reimNo 要更新的报销单号
     * @param request 更新后的完整表单数据
     * @return 报销单号及更新后的状态
     */
    ReimbursementSaveResponse update(String reimNo, ReimbursementSaveRequest request);

    /**
     * 将数据库中已有的草稿校验后提交。
     *
     * @param reimNo 草稿报销单号
     * @return 报销单号及已提交状态
     */
    ReimbursementSaveResponse submitDraft(String reimNo);

    /**
     * 深度复制一张报销单为新的草稿。
     *
     * <p>主表、行程、每日补助和费用分摊都会生成新的数据库主键，
     * 原报销单不会被修改。</p>
     *
     * @param reimNo 源报销单号
     * @return 新报销单号及草稿状态
     */
    ReimbursementSaveResponse copy(String reimNo);

    /**
     * 物理删除草稿及其所有子表数据。
     *
     * @param reimNo 草稿报销单号
     */
    void deleteDraft(String reimNo);

    /**
     * 将非草稿单据标记为已作废，数据仍保留在数据库中。
     *
     * @param reimNo 要作废的报销单号
     */
    void voidBill(String reimNo);

    /**
     * 根据行程日期和目的地城市标准生成每日补助明细。
     *
     * @param request 出发地、目的地和日期范围
     * @return 从开始日期到结束日期的逐日补助数据
     */
    List<ReimbursementSaveRequest.AllowanceDayRequest> generateAllowanceDays(AllowanceGenerateRequest request);

    /**
     * 查询全部城市补助标准，供前端选择城市和预览补助使用。
     *
     * @return 按城市等级和名称排序的城市标准列表
     */
    List<FkCityAllowance> listCityAllowances();

    /**
     * 统计指定报销人的单据数量、金额趋势和公司分摊结构。
     *
     * @param reimburserName 报销人姓名，可与工号二选一
     * @param reimburserNo 报销人工号，可与姓名二选一
     * @return 个人报销统计结果
     */
    PersonalStatisticsVO personalStatistics(String reimburserName, String reimburserNo);
}
