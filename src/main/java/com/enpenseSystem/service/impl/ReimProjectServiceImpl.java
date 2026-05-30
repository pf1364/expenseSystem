package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.ReimProject;
import com.enpenseSystem.mapper.ReimProjectMapper;
import com.enpenseSystem.service.ReimProjectService;
import org.springframework.stereotype.Service;

@Service
public class ReimProjectServiceImpl extends ServiceImpl<ReimProjectMapper, ReimProject> implements ReimProjectService {
}
