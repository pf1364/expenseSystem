package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.FkCityAllowance;
import com.enpenseSystem.mapper.FkCityAllowanceMapper;
import com.enpenseSystem.service.FkCityAllowanceService;
import org.springframework.stereotype.Service;

/** 城市补助标准通用数据服务实现，标准 CRUD 由 MyBatis-Plus ServiceImpl 提供。 */
@Service
public class FkCityAllowanceServiceImpl extends ServiceImpl<FkCityAllowanceMapper, FkCityAllowance> implements FkCityAllowanceService {
}
