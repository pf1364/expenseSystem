package com.enpenseSystem.utils;

/**
 * 报销单状态和类型常量。
 *
 * <p>统一常量可避免 Controller、Service 中散落魔法字符串。</p>
 */
public class ReimbursementConstants {
    public static final String STATUS_DRAFT = "DRAFT"; // 草稿状态编码
    public static final String STATUS_DRAFT_NAME = "草稿"; // 草稿状态名称
    public static final String STATUS_SUBMITTED = "SUBMITTED"; // 已提交状态编码
    public static final String STATUS_SUBMITTED_NAME = "已提交"; // 已提交状态名称
    public static final String STATUS_VOIDED = "VOIDED"; // 已作废状态编码
    public static final String STATUS_VOIDED_NAME = "已作废"; // 已作废状态名称
    public static final String BILL_TYPE = "TRAVEL_REIMBURSEMENT"; // 差旅报销单类型编码
    public static final String BILL_TYPE_NAME = "差旅费用报销单"; // 差旅报销单类型名称
}
