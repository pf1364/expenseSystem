package com.enpenseSystem.service.support;

import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.enpenseSystem.dto.ReimbursementPageVO;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.entity.FkReimAllocation;
import com.enpenseSystem.entity.FkReimAllowanceDay;
import com.enpenseSystem.entity.FkReimItinerary;
import com.enpenseSystem.entity.FkReimMain;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报销单返回对象组装器。
 *
 * <p>该组件只负责 Entity、VO、保存请求之间的对象转换，不访问数据库，也不做金额校验。
 * 把这些转换逻辑从核心 Service 中拆出来后，Service 可以专注于事务、查询和业务流程编排。</p>
 */
@Component
public class ReimbursementDetailAssembler {

    /**
     * 将主表和三类子表组装成详情接口需要的嵌套结构。
     *
     * <p>每日补助先按 itineraryId 分组，再挂到对应行程下面，避免在组装时产生额外数据库查询。</p>
     */
    public ReimbursementDetailVO assemble(FkReimMain main,
                                          List<FkReimItinerary> itineraries,
                                          List<FkReimAllowanceDay> days,
                                          List<FkReimAllocation> allocations) {
        Map<Long, List<FkReimAllowanceDay>> daysByItinerary = safeList(days).stream()
                .collect(Collectors.groupingBy(FkReimAllowanceDay::getItineraryId,
                        LinkedHashMap::new, Collectors.toList()));

        ReimbursementDetailVO vo = new ReimbursementDetailVO();
        BeanUtils.copyProperties(main, vo);
        vo.setItineraries(safeList(itineraries).stream()
                .map(item -> toItineraryVO(item, daysByItinerary.get(item.getId())))
                .collect(Collectors.toList()));
        vo.setAllocations(safeList(allocations).stream()
                .map(this::toAllocationVO)
                .collect(Collectors.toList()));
        return vo;
    }

    /**
     * 将主表实体转换成分页列表行对象。
     */
    public ReimbursementPageVO toPageVO(FkReimMain main) {
        ReimbursementPageVO vo = new ReimbursementPageVO();
        BeanUtils.copyProperties(main, vo);
        return vo;
    }

    /**
     * 将详情对象还原成保存请求。
     *
     * <p>提交已有草稿和复制报销单时会复用创建提交的校验逻辑，因此需要把数据库详情还原成请求结构。</p>
     */
    public ReimbursementSaveRequest toSaveRequest(ReimbursementDetailVO detail) {
        ReimbursementSaveRequest request = new ReimbursementSaveRequest();
        BeanUtils.copyProperties(detail, request);
        request.setItineraries(safeList(detail.getItineraries()).stream().map(item -> {
            ReimbursementSaveRequest.ItineraryRequest itinerary = new ReimbursementSaveRequest.ItineraryRequest();
            BeanUtils.copyProperties(item, itinerary);
            itinerary.setAllowanceDays(safeList(item.getAllowanceDays()).stream().map(day -> {
                ReimbursementSaveRequest.AllowanceDayRequest requestDay = new ReimbursementSaveRequest.AllowanceDayRequest();
                BeanUtils.copyProperties(day, requestDay);
                return requestDay;
            }).collect(Collectors.toList()));
            return itinerary;
        }).collect(Collectors.toList()));
        request.setAllocations(safeList(detail.getAllocations()).stream().map(item -> {
            ReimbursementSaveRequest.AllocationRequest allocation = new ReimbursementSaveRequest.AllocationRequest();
            BeanUtils.copyProperties(item, allocation);
            return allocation;
        }).collect(Collectors.toList()));
        return request;
    }

    private ReimbursementDetailVO.ItineraryVO toItineraryVO(FkReimItinerary itinerary, List<FkReimAllowanceDay> days) {
        ReimbursementDetailVO.ItineraryVO vo = new ReimbursementDetailVO.ItineraryVO();
        BeanUtils.copyProperties(itinerary, vo);
        vo.setAllowanceDays(safeList(days).stream().map(this::toAllowanceDayVO).collect(Collectors.toList()));
        return vo;
    }

    private ReimbursementDetailVO.AllowanceDayVO toAllowanceDayVO(FkReimAllowanceDay day) {
        ReimbursementDetailVO.AllowanceDayVO vo = new ReimbursementDetailVO.AllowanceDayVO();
        BeanUtils.copyProperties(day, vo);
        return vo;
    }

    private ReimbursementDetailVO.AllocationVO toAllocationVO(FkReimAllocation allocation) {
        ReimbursementDetailVO.AllocationVO vo = new ReimbursementDetailVO.AllocationVO();
        BeanUtils.copyProperties(allocation, vo);
        return vo;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
