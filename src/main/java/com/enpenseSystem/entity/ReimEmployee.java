package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("reim_employee")
public class ReimEmployee {

    @TableId
    private String reimburserId;
    private String reimburserNo;
    private String reimburserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getReimburserId() {
        return reimburserId;
    }

    public void setReimburserId(String reimburserId) {
        this.reimburserId = reimburserId;
    }

    public String getReimburserNo() {
        return reimburserNo;
    }

    public void setReimburserNo(String reimburserNo) {
        this.reimburserNo = reimburserNo;
    }

    public String getReimburserName() {
        return reimburserName;
    }

    public void setReimburserName(String reimburserName) {
        this.reimburserName = reimburserName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
