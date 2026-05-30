package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enpenseSystem.entity.ReimBusinessType;
import com.enpenseSystem.mapper.ReimBusinessTypeMapper;
import com.enpenseSystem.service.ReimBusinessTypeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReimBusinessTypeServiceImpl extends ServiceImpl<ReimBusinessTypeMapper, ReimBusinessType> implements ReimBusinessTypeService {

    @Override
    public List<ReimBusinessType> listTree() {
        List<ReimBusinessType> allTypes = list(new LambdaQueryWrapper<ReimBusinessType>()
                .orderByAsc(ReimBusinessType::getBusinessTypeNo));
        Map<String, ReimBusinessType> typeMap = new LinkedHashMap<>();
        for (ReimBusinessType type : allTypes) {
            type.setChildren(new ArrayList<>());
            typeMap.put(type.getBusinessTypeId(), type);
        }

        List<ReimBusinessType> roots = new ArrayList<>();
        for (ReimBusinessType type : allTypes) {
            String parentId = type.getParentBusinessTypeId();
            ReimBusinessType parent = parentId == null ? null : typeMap.get(parentId);
            if (parent == null) {
                roots.add(type);
            } else {
                parent.getChildren().add(type);
            }
        }
        return roots;
    }
}
