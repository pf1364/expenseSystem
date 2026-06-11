package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.FkReimItinerary;
import com.enpenseSystem.mapper.FkReimItineraryMapper;
import com.enpenseSystem.service.FkReimItineraryService;
import org.springframework.stereotype.Service;

/** 行程表通用数据服务实现，标准 CRUD 由 MyBatis-Plus ServiceImpl 提供。 */
@Service
public class FkReimItineraryServiceImpl extends ServiceImpl<FkReimItineraryMapper, FkReimItinerary> implements FkReimItineraryService {
}
