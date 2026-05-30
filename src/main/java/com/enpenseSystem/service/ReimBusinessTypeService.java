package com.enpenseSystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enpenseSystem.entity.ReimBusinessType;

import java.util.List;

public interface ReimBusinessTypeService extends IService<ReimBusinessType> {

    List<ReimBusinessType> listTree();
}
