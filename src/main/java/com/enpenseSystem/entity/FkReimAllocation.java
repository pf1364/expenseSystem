package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fk_reim_allocation")
public class FkReimAllocation {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID
    private Long mainId; // 报销单主表ID
    private String allocationOwnerType; // 分摊归属方类型：COMPANY公司，DEPARTMENT部门
    private String allocationOwnerId; // 分摊归属方ID
    private String allocationOwnerNo; // 分摊归属方编号
    private String allocationOwnerName; // 分摊归属方名称
    private String businessId; // 分摊业务ID
    private String businessName; // 分摊业务名称
    private BigDecimal allocationRatio; // 分摊比例，数据库存0-1
    private BigDecimal allocationAmount; // 分摊金额
    private Integer sortNo; // 排序号
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
