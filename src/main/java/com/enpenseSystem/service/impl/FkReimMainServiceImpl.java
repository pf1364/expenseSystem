package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.FkReimMain;
import com.enpenseSystem.mapper.FkReimMainMapper;
import com.enpenseSystem.service.FkReimMainService;
import org.springframework.stereotype.Service;

/**
 * 报销单主表通用数据服务实现。
 *
 * <p>ServiceImpl 已提供标准 CRUD，本类暂不增加额外业务逻辑。</p>
 */
@Service
public class FkReimMainServiceImpl extends ServiceImpl<FkReimMainMapper, FkReimMain> implements FkReimMainService {
}
