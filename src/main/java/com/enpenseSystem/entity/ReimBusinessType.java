package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@TableName("reim_business_type")
public class ReimBusinessType {

    @TableId
    private String businessTypeId;
    private String businessTypeNo;
    private String businessTypeName;
    private Integer hasSubordinateNode;
    private String parentBusinessTypeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<ReimBusinessType> children = new ArrayList<>();

    public String getBusinessTypeId() {
        return businessTypeId;
    }

    public void setBusinessTypeId(String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    public String getBusinessTypeNo() {
        return businessTypeNo;
    }

    public void setBusinessTypeNo(String businessTypeNo) {
        this.businessTypeNo = businessTypeNo;
    }

    public String getBusinessTypeName() {
        return businessTypeName;
    }

    public void setBusinessTypeName(String businessTypeName) {
        this.businessTypeName = businessTypeName;
    }

    public Integer getHasSubordinateNode() {
        return hasSubordinateNode;
    }

    public void setHasSubordinateNode(Integer hasSubordinateNode) {
        this.hasSubordinateNode = hasSubordinateNode;
    }

    public String getParentBusinessTypeId() {
        return parentBusinessTypeId;
    }

    public void setParentBusinessTypeId(String parentBusinessTypeId) {
        this.parentBusinessTypeId = parentBusinessTypeId;
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

    public List<ReimBusinessType> getChildren() {
        return children;
    }

    public void setChildren(List<ReimBusinessType> children) {
        this.children = children;
    }
}
