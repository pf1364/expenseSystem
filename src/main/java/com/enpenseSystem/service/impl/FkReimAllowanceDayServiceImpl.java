package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.FkReimAllowanceDay;
import com.enpenseSystem.mapper.FkReimAllowanceDayMapper;
import com.enpenseSystem.service.FkReimAllowanceDayService;
import org.springframework.stereotype.Service;

/** 每日补助明细通用数据服务实现，标准 CRUD 由 MyBatis-Plus ServiceImpl 提供。 */
@Service
public class FkReimAllowanceDayServiceImpl extends ServiceImpl<FkReimAllowanceDayMapper, FkReimAllowanceDay> implements FkReimAllowanceDayService {
}
