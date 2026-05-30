package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("reim_company")
public class ReimCompany {

    @TableId
    private String reimCompanyId;
    private String reimCompanyNo;
    private String reimCompanyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getReimCompanyId() {
        return reimCompanyId;
    }

    public void setReimCompanyId(String reimCompanyId) {
        this.reimCompanyId = reimCompanyId;
    }

    public String getReimCompanyNo() {
        return reimCompanyNo;
    }

    public void setReimCompanyNo(String reimCompanyNo) {
        this.reimCompanyNo = reimCompanyNo;
    }

    public String getReimCompanyName() {
        return reimCompanyName;
    }

    public void setReimCompanyName(String reimCompanyName) {
        this.reimCompanyName = reimCompanyName;
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
