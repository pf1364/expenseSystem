package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销单主表实体，对应数据库表 fk_reim_main。
 *
 * <p>保存单据基础信息、状态和金额汇总。行程、每日补助和费用分摊
 * 分别存放在子表中，通过本实体的 id 关联。</p>
 */
@Data
@TableName("fk_reim_main")
public class FkReimMain {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID
    private String reimNo; // 报销单号，唯一
    private String billStatus; // 单据状态编码
    private String billStatusName; // 单据状态名称
    private String billType; // 单据类型编码
    private String billTypeName; // 单据类型名称
    private String reimburserId; // 报销人ID
    private String reimburserNo; // 报销人工号
    private String reimburserName; // 报销人姓名
    private String reimDepartmentId; // 报销部门ID
    private String reimDepartmentNo; // 报销部门编号
    private String reimDepartmentName; // 报销部门名称
    private String reimCompanyNames; // 费用归属公司名称汇总，来自分摊表公司类型归属方
    private String businessTypeId; // 业务类型ID
    private String businessTypeNo; // 业务类型编号
    private String businessTypeName; // 业务类型名称
    private String title; // 报销标题
    private String reason; // 报销事由
    private BigDecimal allowanceAmount; // 补助总金额
    private BigDecimal mealAmount; // 餐费补助合计
    private BigDecimal trafficAmount; // 交通补助合计
    private BigDecimal communicationAmount; // 通讯补助合计
    private String remark; // 备注信息
    private LocalDateTime submittedAt; // 提交时间
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
