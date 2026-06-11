package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.FkReimAllocation;
import com.enpenseSystem.mapper.FkReimAllocationMapper;
import com.enpenseSystem.service.FkReimAllocationService;
import org.springframework.stereotype.Service;

/** 费用分摊通用数据服务实现，标准 CRUD 由 MyBatis-Plus ServiceImpl 提供。 */
@Service
public class FkReimAllocationServiceImpl extends ServiceImpl<FkReimAllocationMapper, FkReimAllocation> implements FkReimAllocationService {
}
