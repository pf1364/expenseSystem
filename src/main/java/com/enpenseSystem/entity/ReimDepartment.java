package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("reim_department")
public class ReimDepartment {

    @TableId
    private String reimDepartmentId;
    private String reimDepartmentNo;
    private String reimDepartmentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getReimDepartmentId() {
        return reimDepartmentId;
    }

    public void setReimDepartmentId(String reimDepartmentId) {
        this.reimDepartmentId = reimDepartmentId;
    }

    public String getReimDepartmentNo() {
        return reimDepartmentNo;
    }

    public void setReimDepartmentNo(String reimDepartmentNo) {
        this.reimDepartmentNo = reimDepartmentNo;
    }

    public String getReimDepartmentName() {
        return reimDepartmentName;
    }

    public void setReimDepartmentName(String reimDepartmentName) {
        this.reimDepartmentName = reimDepartmentName;
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
