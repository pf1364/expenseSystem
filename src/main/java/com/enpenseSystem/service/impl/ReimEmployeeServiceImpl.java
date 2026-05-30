package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.ReimEmployee;
import com.enpenseSystem.mapper.ReimEmployeeMapper;
import com.enpenseSystem.service.ReimEmployeeService;
import org.springframework.stereotype.Service;

@Service
public class ReimEmployeeServiceImpl extends ServiceImpl<ReimEmployeeMapper, ReimEmployee> implements ReimEmployeeService {
}
