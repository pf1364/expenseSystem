package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enpenseSystem.common.PageData;
import com.enpenseSystem.dto.AllowanceGenerateRequest;
import com.enpenseSystem.dto.PersonalStatisticsVO;
import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.enpenseSystem.dto.ReimbursementPageQuery;
import com.enpenseSystem.dto.ReimbursementPageVO;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.dto.ReimbursementSaveResponse;
import com.enpenseSystem.entity.FkCityAllowance;
import com.enpenseSystem.entity.FkReimAllocation;
import com.enpenseSystem.entity.FkReimAllowanceDay;
import com.enpenseSystem.entity.FkReimItinerary;
import com.enpenseSystem.entity.FkReimMain;
import com.enpenseSystem.service.FkCityAllowanceService;
import com.enpenseSystem.service.FkReimAllocationService;
import com.enpenseSystem.service.FkReimAllowanceDayService;
import com.enpenseSystem.service.FkReimItineraryService;
import com.enpenseSystem.service.FkReimMainService;
import com.enpenseSystem.service.ReimbursementService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_DRAFT_NAME = "草稿";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_SUBMITTED_NAME = "已提交";
    private static final String STATUS_VOIDED = "VOIDED";
    private static final String STATUS_VOIDED_NAME = "已作废";
    private static final String BILL_TYPE = "TRAVEL_REIMBURSEMENT";
    private static final String BILL_TYPE_NAME = "差旅费用报销单";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final FkReimMainService mainService;
    private final FkReimItineraryService itineraryService;
    private final FkReimAllowanceDayService allowanceDayService;
    private final FkReimAllocationService allocationService;
    private final FkCityAllowanceService cityAllowanceService;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public ReimbursementServiceImpl(FkReimMainService mainService,
                                    FkReimItineraryService itineraryService,
                                    FkReimAllowanceDayService allowanceDayService,
                                    FkReimAllocationService allocationService,
                                    FkCityAllowanceService cityAllowanceService,
                                    ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                    ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
        this.mainService = mainService;
        this.itineraryService = itineraryService;
        this.allowanceDayService = allowanceDayService;
        this.allocationService = allocationService;
        this.cityAllowanceService = cityAllowanceService;
        this.redisTemplateProvider = redisTemplateProvider;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
    }

    @Override
    public PageData<ReimbursementPageVO> page(ReimbursementPageQuery query) {
        LambdaQueryWrapper<FkReimMain> wrapper = new LambdaQueryWrapper<FkReimMain>()
                .like(StringUtils.hasText(query.getReimNo()), FkReimMain::getReimNo, query.getReimNo())
                .like(StringUtils.hasText(query.getTitle()), FkReimMain::getTitle, query.getTitle())
                .like(StringUtils.hasText(query.getReason()), FkReimMain::getReason, query.getReason())
                .like(StringUtils.hasText(query.getReimCompanyName()), FkReimMain::getReimCompanyNames, query.getReimCompanyName())
                .eq(StringUtils.hasText(query.getReimDepartmentName()), FkReimMain::getReimDepartmentName, query.getReimDepartmentName())
                .eq(StringUtils.hasText(query.getReimburserName()), FkReimMain::getReimburserName, query.getReimburserName())
                .eq(StringUtils.hasText(query.getReimburserNo()), FkReimMain::getReimburserNo, query.getReimburserNo())
                .eq(StringUtils.hasText(query.getBusinessTypeName()), FkReimMain::getBusinessTypeName, query.getBusinessTypeName())
                .eq(StringUtils.hasText(query.getBillStatus()), FkReimMain::getBillStatus, query.getBillStatus())
                .orderByDesc(FkReimMain::getCreatedAt);
        if (StringUtils.hasText(query.getReimburserKeyword())) {
            wrapper.and(w -> w.eq(FkReimMain::getReimburserName, query.getReimburserKeyword())
                    .or()
                    .eq(FkReimMain::getReimburserNo, query.getReimburserKeyword()));
        }

        Page<FkReimMain> page = mainService.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<ReimbursementPageVO> records = page.getRecords().stream().map(this::toPageVO).collect(Collectors.toList());
        return new PageData<>(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public ReimbursementDetailVO detail(String reimNo) {
        FkReimMain main = getMainByReimNo(reimNo);
        List<FkReimItinerary> itineraries = itineraryService.list(new LambdaQueryWrapper<FkReimItinerary>()
                .eq(FkReimItinerary::getMainId, main.getId())
                .orderByAsc(FkReimItinerary::getStartDate)
                .orderByAsc(FkReimItinerary::getId));
        List<FkReimAllowanceDay> days = allowanceDayService.list(new LambdaQueryWrapper<FkReimAllowanceDay>()
                .eq(FkReimAllowanceDay::getMainId, main.getId())
                .orderByAsc(FkReimAllowanceDay::getAllowanceDate)
                .orderByAsc(FkReimAllowanceDay::getId));
        Map<Long, List<FkReimAllowanceDay>> daysByItinerary = days.stream()
                .collect(Collectors.groupingBy(FkReimAllowanceDay::getItineraryId, LinkedHashMap::new, Collectors.toList()));
        List<FkReimAllocation> allocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>()
                .eq(FkReimAllocation::getMainId, main.getId())
                .orderByAsc(FkReimAllocation::getSortNo)
                .orderByAsc(FkReimAllocation::getId));

        ReimbursementDetailVO vo = new ReimbursementDetailVO();
        BeanUtils.copyProperties(main, vo);
        vo.setItineraries(itineraries.stream().map(item -> toItineraryVO(item, daysByItinerary.get(item.getId()))).collect(Collectors.toList()));
        vo.setAllocations(allocations.stream().map(this::toAllocationVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse createDraft(ReimbursementSaveRequest request) {
        return saveNew(request, STATUS_DRAFT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse createAndSubmit(ReimbursementSaveRequest request) {
        validateForSubmit(request);
        ReimbursementSaveResponse response = saveNew(request, STATUS_SUBMITTED);
        publishEvent("reim.bill.submitted", response.getReimNo());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse update(String reimNo, ReimbursementSaveRequest request) {
        FkReimMain main = getMainByReimNo(reimNo);
        if (STATUS_VOIDED.equals(main.getBillStatus())) {
            throw new IllegalArgumentException("已作废单据不能修改");
        }
        request.setReimNo(reimNo);
        fillMain(main, request, main.getBillStatus());
        saveChildren(main, request);
        refreshMainTotals(main);
        return new ReimbursementSaveResponse(main.getReimNo(), main.getBillStatus(), main.getBillStatusName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse submitDraft(String reimNo) {
        ReimbursementDetailVO detail = detail(reimNo);
        ReimbursementSaveRequest request = toSaveRequest(detail);
        validateForSubmit(request);
        FkReimMain main = getMainByReimNo(reimNo);
        main.setBillStatus(STATUS_SUBMITTED);
        main.setBillStatusName(STATUS_SUBMITTED_NAME);
        main.setSubmittedAt(LocalDateTime.now());
        main.setUpdatedAt(LocalDateTime.now());
        mainService.updateById(main);
        publishEvent("reim.bill.submitted", reimNo);
        return new ReimbursementSaveResponse(reimNo, STATUS_SUBMITTED, STATUS_SUBMITTED_NAME);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse copy(String reimNo) {
        FkReimMain source = getMainByReimNo(reimNo);
        LocalDateTime now = LocalDateTime.now();

        FkReimMain target = new FkReimMain();
        BeanUtils.copyProperties(source, target);
        target.setId(null);
        target.setReimNo(nextReimNo());
        target.setBillStatus(STATUS_DRAFT);
        target.setBillStatusName(STATUS_DRAFT_NAME);
        target.setSubmittedAt(null);
        target.setCreatedAt(now);
        target.setUpdatedAt(now);
        mainService.save(target);

        List<FkReimItinerary> sourceItineraries = itineraryService.list(new LambdaQueryWrapper<FkReimItinerary>()
                .eq(FkReimItinerary::getMainId, source.getId())
                .orderByAsc(FkReimItinerary::getId));
        Map<Long, Long> itineraryIdMap = new HashMap<>();
        for (FkReimItinerary sourceItinerary : sourceItineraries) {
            Long sourceItineraryId = sourceItinerary.getId();
            FkReimItinerary targetItinerary = new FkReimItinerary();
            BeanUtils.copyProperties(sourceItinerary, targetItinerary);
            targetItinerary.setId(null);
            targetItinerary.setMainId(target.getId());
            targetItinerary.setCreatedAt(now);
            targetItinerary.setUpdatedAt(now);
            itineraryService.save(targetItinerary);
            itineraryIdMap.put(sourceItineraryId, targetItinerary.getId());
        }

        List<FkReimAllowanceDay> sourceDays = allowanceDayService.list(new LambdaQueryWrapper<FkReimAllowanceDay>()
                .eq(FkReimAllowanceDay::getMainId, source.getId())
                .orderByAsc(FkReimAllowanceDay::getId));
        for (FkReimAllowanceDay sourceDay : sourceDays) {
            FkReimAllowanceDay targetDay = new FkReimAllowanceDay();
            BeanUtils.copyProperties(sourceDay, targetDay);
            targetDay.setId(null);
            targetDay.setMainId(target.getId());
            targetDay.setItineraryId(itineraryIdMap.get(sourceDay.getItineraryId()));
            targetDay.setCreatedAt(now);
            targetDay.setUpdatedAt(now);
            allowanceDayService.save(targetDay);
        }

        List<FkReimAllocation> sourceAllocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>()
                .eq(FkReimAllocation::getMainId, source.getId())
                .orderByAsc(FkReimAllocation::getSortNo)
                .orderByAsc(FkReimAllocation::getId));
        for (FkReimAllocation sourceAllocation : sourceAllocations) {
            FkReimAllocation targetAllocation = new FkReimAllocation();
            BeanUtils.copyProperties(sourceAllocation, targetAllocation);
            targetAllocation.setId(null);
            targetAllocation.setMainId(target.getId());
            targetAllocation.setCreatedAt(now);
            targetAllocation.setUpdatedAt(now);
            allocationService.save(targetAllocation);
        }

        refreshMainTotals(target);
        return new ReimbursementSaveResponse(target.getReimNo(), target.getBillStatus(), target.getBillStatusName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(String reimNo) {
        FkReimMain main = getMainByReimNo(reimNo);
        if (!STATUS_DRAFT.equals(main.getBillStatus())) {
            throw new IllegalArgumentException("只能删除草稿单据");
        }
        deleteChildren(main.getId());
        mainService.removeById(main.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidBill(String reimNo) {
        FkReimMain main = getMainByReimNo(reimNo);
        if (STATUS_DRAFT.equals(main.getBillStatus())) {
            throw new IllegalArgumentException("草稿请直接删除，不需要作废");
        }
        main.setBillStatus(STATUS_VOIDED);
        main.setBillStatusName(STATUS_VOIDED_NAME);
        main.setUpdatedAt(LocalDateTime.now());
        mainService.updateById(main);
        publishEvent("reim.bill.voided", reimNo);
    }

    @Override
    public List<ReimbursementSaveRequest.AllowanceDayRequest> generateAllowanceDays(AllowanceGenerateRequest request) {
        if (request == null || request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("行程开始日期和结束日期不能为空");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("行程结束日期不能早于开始日期");
        }
        FkCityAllowance standard = findCityStandard(request.getEndCityCode(), request.getEndCityName(), true);
        List<ReimbursementSaveRequest.AllowanceDayRequest> days = new ArrayList<>();
        LocalDate cursor = request.getStartDate();
        while (!cursor.isAfter(request.getEndDate())) {
            days.add(toAllowanceDayRequest(cursor, standard));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    @Override
    public List<FkCityAllowance> listCityAllowances() {
        return cityAllowanceService.list(new LambdaQueryWrapper<FkCityAllowance>().orderByAsc(FkCityAllowance::getCityLevel).orderByAsc(FkCityAllowance::getCityName));
    }

    @Override
    public PersonalStatisticsVO personalStatistics(String reimburserName, String reimburserNo) {
        LambdaQueryWrapper<FkReimMain> wrapper = new LambdaQueryWrapper<FkReimMain>()
                .eq(StringUtils.hasText(reimburserName), FkReimMain::getReimburserName, reimburserName)
                .eq(StringUtils.hasText(reimburserNo), FkReimMain::getReimburserNo, reimburserNo)
                .ne(FkReimMain::getBillStatus, STATUS_VOIDED)
                .orderByDesc(FkReimMain::getCreatedAt);
        if (!StringUtils.hasText(reimburserName) && !StringUtils.hasText(reimburserNo)) {
            throw new IllegalArgumentException("请选择报销人");
        }
        List<FkReimMain> mains = mainService.list(wrapper);
        PersonalStatisticsVO vo = new PersonalStatisticsVO();
        vo.setTotalCount((long) mains.size());
        vo.setDraftCount(mains.stream().filter(item -> STATUS_DRAFT.equals(item.getBillStatus())).count());
        vo.setSubmittedCount(mains.stream().filter(item -> STATUS_SUBMITTED.equals(item.getBillStatus())).count());
        vo.setTotalAmount(mains.stream().map(FkReimMain::getAllowanceAmount).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add));

        Map<String, BigDecimal> monthMap = mains.stream()
                .filter(item -> item.getCreatedAt() != null)
                .collect(Collectors.groupingBy(item -> item.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new,
                        Collectors.mapping(item -> nvl(item.getAllowanceAmount()), Collectors.reducing(ZERO, BigDecimal::add))));
        monthMap.forEach((month, amount) -> vo.getMonthlyAmounts().add(new PersonalStatisticsVO.MonthlyAmount(month, amount)));

        Set<Long> mainIds = mains.stream().map(FkReimMain::getId).collect(Collectors.toSet());
        if (!mainIds.isEmpty()) {
            List<FkReimAllocation> allocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>()
                    .in(FkReimAllocation::getMainId, mainIds)
                    .eq(FkReimAllocation::getAllocationOwnerType, "COMPANY"));
            Map<String, BigDecimal> companyMap = allocations.stream()
                    .collect(Collectors.groupingBy(FkReimAllocation::getAllocationOwnerName,
                            LinkedHashMap::new,
                            Collectors.mapping(item -> nvl(item.getAllocationAmount()), Collectors.reducing(ZERO, BigDecimal::add))));
            companyMap.forEach((name, amount) -> vo.getCompanyShares().add(new PersonalStatisticsVO.CompanyShare(name, amount)));
        }
        vo.setRecentBills(mains.stream().limit(10).map(this::toPageVO).collect(Collectors.toList()));
        return vo;
    }

    private ReimbursementSaveResponse saveNew(ReimbursementSaveRequest request, String status) {
        if (STATUS_SUBMITTED.equals(status)) {
            validateForSubmit(request);
        }
        FkReimMain main = new FkReimMain();
        main.setReimNo(nextReimNo());
        fillMain(main, request, status);
        mainService.save(main);
        saveChildren(main, request);
        refreshMainTotals(main);
        return new ReimbursementSaveResponse(main.getReimNo(), main.getBillStatus(), main.getBillStatusName());
    }

    private void fillMain(FkReimMain main, ReimbursementSaveRequest request, String status) {
        LocalDateTime now = LocalDateTime.now();
        main.setBillStatus(status);
        main.setBillStatusName(STATUS_SUBMITTED.equals(status) ? STATUS_SUBMITTED_NAME : STATUS_DRAFT_NAME);
        main.setBillType(BILL_TYPE);
        main.setBillTypeName(BILL_TYPE_NAME);
        main.setReimburserId(text(request.getReimburserId()));
        main.setReimburserNo(text(request.getReimburserNo()));
        main.setReimburserName(text(request.getReimburserName()));
        main.setReimDepartmentId(text(request.getReimDepartmentId()));
        main.setReimDepartmentNo(text(request.getReimDepartmentNo()));
        main.setReimDepartmentName(text(request.getReimDepartmentName()));
        main.setReimCompanyNames(text(request.getReimCompanyNames()));
        main.setBusinessTypeId(text(request.getBusinessTypeId()));
        main.setBusinessTypeNo(text(request.getBusinessTypeNo()));
        main.setBusinessTypeName(text(request.getBusinessTypeName()));
        main.setTitle(text(request.getTitle()));
        main.setReason(text(request.getReason()));
        main.setRemark(request.getRemark());
        if (main.getCreatedAt() == null) {
            main.setCreatedAt(now);
        }
        main.setUpdatedAt(now);
        if (STATUS_SUBMITTED.equals(status) && main.getSubmittedAt() == null) {
            main.setSubmittedAt(now);
        }
        main.setAllowanceAmount(nvl(main.getAllowanceAmount()));
        main.setMealAmount(nvl(main.getMealAmount()));
        main.setTrafficAmount(nvl(main.getTrafficAmount()));
        main.setCommunicationAmount(nvl(main.getCommunicationAmount()));
    }

    private void saveChildren(FkReimMain main, ReimbursementSaveRequest request) {
        List<ReimbursementSaveRequest.ItineraryRequest> itineraryRequests = request.getItineraries() == null ? List.of() : request.getItineraries();
        List<FkReimItinerary> dbItineraries = itineraryService.list(new LambdaQueryWrapper<FkReimItinerary>().eq(FkReimItinerary::getMainId, main.getId()));
        Map<Long, FkReimItinerary> dbItineraryMap = dbItineraries.stream().collect(Collectors.toMap(FkReimItinerary::getId, item -> item));
        Set<Long> keepItineraryIds = new HashSet<>();

        for (ReimbursementSaveRequest.ItineraryRequest item : itineraryRequests) {
            if (!isUsableItinerary(item)) {
                continue;
            }
            FkReimItinerary itinerary = item.getId() != null && dbItineraryMap.containsKey(item.getId()) ? dbItineraryMap.get(item.getId()) : new FkReimItinerary();
            fillItinerary(itinerary, main.getId(), item);
            if (itinerary.getId() == null) {
                itineraryService.save(itinerary);
            } else {
                itineraryService.updateById(itinerary);
            }
            keepItineraryIds.add(itinerary.getId());
            saveAllowanceDays(main, itinerary, item);
        }

        List<Long> deleteItineraryIds = dbItineraries.stream()
                .map(FkReimItinerary::getId)
                .filter(id -> !keepItineraryIds.contains(id))
                .collect(Collectors.toList());
        if (!deleteItineraryIds.isEmpty()) {
            allowanceDayService.remove(new LambdaQueryWrapper<FkReimAllowanceDay>().in(FkReimAllowanceDay::getItineraryId, deleteItineraryIds));
            itineraryService.removeByIds(deleteItineraryIds);
        }

        saveAllocations(main, request.getAllocations());
    }

    private void saveAllowanceDays(FkReimMain main, FkReimItinerary itinerary, ReimbursementSaveRequest.ItineraryRequest request) {
        List<ReimbursementSaveRequest.AllowanceDayRequest> requestedDays = request.getAllowanceDays();
        if (requestedDays == null || requestedDays.isEmpty()) {
            AllowanceGenerateRequest generateRequest = new AllowanceGenerateRequest();
            generateRequest.setEndCityCode(itinerary.getEndCityCode());
            generateRequest.setEndCityName(itinerary.getEndCityName());
            generateRequest.setStartDate(itinerary.getStartDate());
            generateRequest.setEndDate(itinerary.getEndDate());
            requestedDays = generateAllowanceDays(generateRequest);
        }

        List<FkReimAllowanceDay> dbDays = allowanceDayService.list(new LambdaQueryWrapper<FkReimAllowanceDay>().eq(FkReimAllowanceDay::getItineraryId, itinerary.getId()));
        Map<Long, FkReimAllowanceDay> dbDayMap = dbDays.stream().collect(Collectors.toMap(FkReimAllowanceDay::getId, item -> item));
        Set<Long> keepIds = new HashSet<>();
        for (ReimbursementSaveRequest.AllowanceDayRequest dayRequest : requestedDays) {
            if (dayRequest.getAllowanceDate() == null) {
                continue;
            }
            FkReimAllowanceDay day = dayRequest.getId() != null && dbDayMap.containsKey(dayRequest.getId()) ? dbDayMap.get(dayRequest.getId()) : new FkReimAllowanceDay();
            fillAllowanceDay(day, main.getId(), itinerary.getId(), dayRequest);
            if (day.getId() == null) {
                allowanceDayService.save(day);
            } else {
                allowanceDayService.updateById(day);
            }
            keepIds.add(day.getId());
        }
        List<Long> deleteIds = dbDays.stream().map(FkReimAllowanceDay::getId).filter(id -> !keepIds.contains(id)).collect(Collectors.toList());
        if (!deleteIds.isEmpty()) {
            allowanceDayService.removeByIds(deleteIds);
        }
    }

    private void saveAllocations(FkReimMain main, List<ReimbursementSaveRequest.AllocationRequest> allocationRequests) {
        List<ReimbursementSaveRequest.AllocationRequest> requests = allocationRequests == null ? List.of() : allocationRequests;
        List<FkReimAllocation> dbAllocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>().eq(FkReimAllocation::getMainId, main.getId()));
        Map<Long, FkReimAllocation> dbMap = dbAllocations.stream().collect(Collectors.toMap(FkReimAllocation::getId, item -> item));
        Set<Long> keepIds = new HashSet<>();
        int index = 1;
        for (ReimbursementSaveRequest.AllocationRequest request : requests) {
            if (!StringUtils.hasText(request.getAllocationOwnerName())) {
                continue;
            }
            FkReimAllocation allocation = request.getId() != null && dbMap.containsKey(request.getId()) ? dbMap.get(request.getId()) : new FkReimAllocation();
            fillAllocation(allocation, main.getId(), request, index++);
            if (allocation.getId() == null) {
                allocationService.save(allocation);
            } else {
                allocationService.updateById(allocation);
            }
            keepIds.add(allocation.getId());
        }
        List<Long> deleteIds = dbAllocations.stream().map(FkReimAllocation::getId).filter(id -> !keepIds.contains(id)).collect(Collectors.toList());
        if (!deleteIds.isEmpty()) {
            allocationService.removeByIds(deleteIds);
        }
    }

    private void refreshMainTotals(FkReimMain main) {
        List<FkReimAllowanceDay> days = allowanceDayService.list(new LambdaQueryWrapper<FkReimAllowanceDay>().eq(FkReimAllowanceDay::getMainId, main.getId()));
        BigDecimal mealAmount = sum(days.stream().map(FkReimAllowanceDay::getMealAmount).collect(Collectors.toList()));
        BigDecimal trafficAmount = sum(days.stream().map(FkReimAllowanceDay::getTrafficAmount).collect(Collectors.toList()));
        BigDecimal communicationAmount = sum(days.stream().map(FkReimAllowanceDay::getCommunicationAmount).collect(Collectors.toList()));
        BigDecimal total = mealAmount.add(trafficAmount).add(communicationAmount).setScale(2, RoundingMode.HALF_UP);

        List<FkReimAllocation> allocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>().eq(FkReimAllocation::getMainId, main.getId()).orderByAsc(FkReimAllocation::getSortNo));
        List<String> companies = allocations.stream()
                .filter(item -> "COMPANY".equalsIgnoreCase(item.getAllocationOwnerType()))
                .map(FkReimAllocation::getAllocationOwnerName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        List<String> departments = allocations.stream()
                .filter(item -> "DEPARTMENT".equalsIgnoreCase(item.getAllocationOwnerType()))
                .map(FkReimAllocation::getAllocationOwnerName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        main.setMealAmount(mealAmount);
        main.setTrafficAmount(trafficAmount);
        main.setCommunicationAmount(communicationAmount);
        main.setAllowanceAmount(total);
        if (!companies.isEmpty()) {
            main.setReimCompanyNames(String.join(",", companies));
        }
        if (!departments.isEmpty()) {
            main.setReimDepartmentName(String.join(",", departments));
        }
        main.setUpdatedAt(LocalDateTime.now());
        mainService.updateById(main);
    }

    private void validateForSubmit(ReimbursementSaveRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("报销标题不能为空");
        }
        if (!StringUtils.hasText(request.getReimburserName())) {
            throw new IllegalArgumentException("报销人不能为空");
        }
        if (!StringUtils.hasText(request.getReimDepartmentName())) {
            throw new IllegalArgumentException("报销部门不能为空");
        }
        if (!StringUtils.hasText(request.getBusinessTypeName())) {
            throw new IllegalArgumentException("业务类型不能为空");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("出差事由不能为空");
        }
        if (request.getItineraries() == null || request.getItineraries().isEmpty()) {
            throw new IllegalArgumentException("至少需要一条行程");
        }
        request.getItineraries().forEach(this::validateItineraryForSubmit);
        validateAllocationsForSubmit(request.getAllocations(), estimateTotalAmount(request));
    }

    private void validateItineraryForSubmit(ReimbursementSaveRequest.ItineraryRequest item) {
        if (!isUsableItinerary(item)) {
            throw new IllegalArgumentException("行程城市和日期不能为空");
        }
        if (item.getEndDate().isBefore(item.getStartDate())) {
            throw new IllegalArgumentException("行程结束日期不能早于开始日期");
        }
        List<ReimbursementSaveRequest.AllowanceDayRequest> days = item.getAllowanceDays();
        if (days == null || days.isEmpty()) {
            AllowanceGenerateRequest generateRequest = new AllowanceGenerateRequest();
            generateRequest.setEndCityCode(item.getEndCityCode());
            generateRequest.setEndCityName(item.getEndCityName());
            generateRequest.setStartDate(item.getStartDate());
            generateRequest.setEndDate(item.getEndDate());
            days = generateAllowanceDays(generateRequest);
            item.setAllowanceDays(days);
        }
        days.forEach(day -> fillAllowanceDay(new FkReimAllowanceDay(), 0L, 0L, day));
    }

    private void validateAllocationsForSubmit(List<ReimbursementSaveRequest.AllocationRequest> allocations, BigDecimal totalAmount) {
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("费用归属及分摊不能为空");
        }
        BigDecimal ratioTotal = ZERO;
        BigDecimal amountTotal = ZERO;
        for (ReimbursementSaveRequest.AllocationRequest allocation : allocations) {
            if (!StringUtils.hasText(allocation.getAllocationOwnerName())) {
                throw new IllegalArgumentException("分摊归属名称不能为空");
            }
            BigDecimal ratio = normalizeRatio(allocation.getAllocationRatio());
            BigDecimal amount = nvl(allocation.getAllocationAmount());
            if (amount.compareTo(ZERO) == 0 && totalAmount.compareTo(ZERO) > 0) {
                amount = totalAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                allocation.setAllocationAmount(amount);
            }
            allocation.setAllocationRatio(ratio);
            ratioTotal = ratioTotal.add(ratio);
            amountTotal = amountTotal.add(amount);
        }
        if (ratioTotal.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.000001")) > 0) {
            throw new IllegalArgumentException("分摊比例合计必须为100%");
        }
        if (amountTotal.subtract(totalAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("分摊金额合计必须等于报销总金额");
        }
    }

    private BigDecimal estimateTotalAmount(ReimbursementSaveRequest request) {
        if (request.getItineraries() == null) {
            return ZERO;
        }
        List<BigDecimal> values = new ArrayList<>();
        request.getItineraries().forEach(itinerary -> {
            List<ReimbursementSaveRequest.AllowanceDayRequest> days = itinerary.getAllowanceDays();
            if (days != null) {
                days.forEach(day -> {
                    fillAllowanceDay(new FkReimAllowanceDay(), 0L, 0L, day);
                    values.add(nvl(day.getDayAmount()));
                });
            }
        });
        return sum(values);
    }

    private void fillItinerary(FkReimItinerary itinerary, Long mainId, ReimbursementSaveRequest.ItineraryRequest request) {
        LocalDateTime now = LocalDateTime.now();
        itinerary.setMainId(mainId);
        itinerary.setTravelerId(text(request.getTravelerId()));
        itinerary.setTravelerNo(text(request.getTravelerNo()));
        itinerary.setTravelerName(text(request.getTravelerName()));
        itinerary.setStartCityCode(text(request.getStartCityCode()));
        itinerary.setStartCityName(text(request.getStartCityName()));
        itinerary.setEndCityCode(text(request.getEndCityCode()));
        itinerary.setEndCityName(text(request.getEndCityName()));
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setDays((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        itinerary.setRouteText(StringUtils.hasText(request.getRouteText()) ? request.getRouteText() : request.getStartCityName() + "-" + request.getEndCityName());
        itinerary.setDescription(text(request.getDescription()));
        if (itinerary.getCreatedAt() == null) {
            itinerary.setCreatedAt(now);
        }
        itinerary.setUpdatedAt(now);
    }

    private void fillAllowanceDay(FkReimAllowanceDay day, Long mainId, Long itineraryId, ReimbursementSaveRequest.AllowanceDayRequest request) {
        FkCityAllowance standard = findCityStandard(request.getCityCode(), request.getCityName(), true);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal meal = checkedAmount(request.getMealSelected(), request.getMealAmount(), standard.getMealStandard(), "餐补");
        BigDecimal traffic = checkedAmount(request.getTrafficSelected(), request.getTrafficAmount(), standard.getTrafficStandard(), "交通补助");
        BigDecimal communication = checkedAmount(request.getCommunicationSelected(), request.getCommunicationAmount(), standard.getCommunicationStandard(), "通讯补助");
        day.setMainId(mainId);
        day.setItineraryId(itineraryId);
        day.setAllowanceDate(request.getAllowanceDate());
        day.setWeekName(StringUtils.hasText(request.getWeekName()) ? request.getWeekName() : weekName(request.getAllowanceDate().getDayOfWeek()));
        day.setCityCode(standard.getCityCode());
        day.setCityName(standard.getCityName());
        day.setCityLevel(standard.getCityLevel());
        day.setMealStandard(nvl(standard.getMealStandard()));
        day.setMealSelected(selected(request.getMealSelected()));
        day.setMealAmount(meal);
        day.setTrafficStandard(nvl(standard.getTrafficStandard()));
        day.setTrafficSelected(selected(request.getTrafficSelected()));
        day.setTrafficAmount(traffic);
        day.setCommunicationStandard(nvl(standard.getCommunicationStandard()));
        day.setCommunicationSelected(selected(request.getCommunicationSelected()));
        day.setCommunicationAmount(communication);
        day.setDayAmount(meal.add(traffic).add(communication).setScale(2, RoundingMode.HALF_UP));
        if (day.getCreatedAt() == null) {
            day.setCreatedAt(now);
        }
        day.setUpdatedAt(now);

        request.setCityCode(day.getCityCode());
        request.setCityName(day.getCityName());
        request.setCityLevel(day.getCityLevel());
        request.setMealStandard(day.getMealStandard());
        request.setMealAmount(day.getMealAmount());
        request.setTrafficStandard(day.getTrafficStandard());
        request.setTrafficAmount(day.getTrafficAmount());
        request.setCommunicationStandard(day.getCommunicationStandard());
        request.setCommunicationAmount(day.getCommunicationAmount());
        request.setDayAmount(day.getDayAmount());
    }

    private void fillAllocation(FkReimAllocation allocation, Long mainId, ReimbursementSaveRequest.AllocationRequest request, int index) {
        LocalDateTime now = LocalDateTime.now();
        allocation.setMainId(mainId);
        allocation.setAllocationOwnerType(StringUtils.hasText(request.getAllocationOwnerType()) ? request.getAllocationOwnerType() : "COMPANY");
        allocation.setAllocationOwnerId(text(request.getAllocationOwnerId()));
        allocation.setAllocationOwnerNo(text(request.getAllocationOwnerNo()));
        allocation.setAllocationOwnerName(text(request.getAllocationOwnerName()));
        allocation.setBusinessId(text(request.getBusinessId()));
        allocation.setBusinessName(text(request.getBusinessName()));
        allocation.setAllocationRatio(normalizeRatio(request.getAllocationRatio()));
        allocation.setAllocationAmount(nvl(request.getAllocationAmount()));
        allocation.setSortNo(request.getSortNo() == null ? index : request.getSortNo());
        if (allocation.getCreatedAt() == null) {
            allocation.setCreatedAt(now);
        }
        allocation.setUpdatedAt(now);
    }

    private FkCityAllowance findCityStandard(String cityCode, String cityName, boolean required) {
        LambdaQueryWrapper<FkCityAllowance> wrapper = new LambdaQueryWrapper<FkCityAllowance>()
                .eq(StringUtils.hasText(cityCode), FkCityAllowance::getCityCode, cityCode)
                .or(!StringUtils.hasText(cityCode) && StringUtils.hasText(cityName), w -> w.eq(FkCityAllowance::getCityName, cityName));
        FkCityAllowance standard = cityAllowanceService.getOne(wrapper, false);
        if (standard == null && required) {
            throw new IllegalArgumentException("未找到城市补助标准：" + (StringUtils.hasText(cityName) ? cityName : cityCode));
        }
        return standard;
    }

    private ReimbursementSaveRequest.AllowanceDayRequest toAllowanceDayRequest(LocalDate date, FkCityAllowance standard) {
        ReimbursementSaveRequest.AllowanceDayRequest day = new ReimbursementSaveRequest.AllowanceDayRequest();
        day.setAllowanceDate(date);
        day.setWeekName(weekName(date.getDayOfWeek()));
        day.setCityCode(standard.getCityCode());
        day.setCityName(standard.getCityName());
        day.setCityLevel(standard.getCityLevel());
        day.setMealStandard(nvl(standard.getMealStandard()));
        day.setMealSelected(1);
        day.setMealAmount(nvl(standard.getMealStandard()));
        day.setTrafficStandard(nvl(standard.getTrafficStandard()));
        day.setTrafficSelected(1);
        day.setTrafficAmount(nvl(standard.getTrafficStandard()));
        day.setCommunicationStandard(nvl(standard.getCommunicationStandard()));
        day.setCommunicationSelected(1);
        day.setCommunicationAmount(nvl(standard.getCommunicationStandard()));
        day.setDayAmount(day.getMealAmount().add(day.getTrafficAmount()).add(day.getCommunicationAmount()).setScale(2, RoundingMode.HALF_UP));
        return day;
    }

    private ReimbursementPageVO toPageVO(FkReimMain main) {
        ReimbursementPageVO vo = new ReimbursementPageVO();
        BeanUtils.copyProperties(main, vo);
        return vo;
    }

    private ReimbursementDetailVO.ItineraryVO toItineraryVO(FkReimItinerary itinerary, List<FkReimAllowanceDay> days) {
        ReimbursementDetailVO.ItineraryVO vo = new ReimbursementDetailVO.ItineraryVO();
        BeanUtils.copyProperties(itinerary, vo);
        if (days != null) {
            vo.setAllowanceDays(days.stream().map(this::toAllowanceDayVO).collect(Collectors.toList()));
        }
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

    private ReimbursementSaveRequest toSaveRequest(ReimbursementDetailVO detail) {
        ReimbursementSaveRequest request = new ReimbursementSaveRequest();
        BeanUtils.copyProperties(detail, request);
        request.setItineraries(detail.getItineraries().stream().map(item -> {
            ReimbursementSaveRequest.ItineraryRequest itinerary = new ReimbursementSaveRequest.ItineraryRequest();
            BeanUtils.copyProperties(item, itinerary);
            itinerary.setAllowanceDays(item.getAllowanceDays().stream().map(day -> {
                ReimbursementSaveRequest.AllowanceDayRequest requestDay = new ReimbursementSaveRequest.AllowanceDayRequest();
                BeanUtils.copyProperties(day, requestDay);
                return requestDay;
            }).collect(Collectors.toList()));
            return itinerary;
        }).collect(Collectors.toList()));
        request.setAllocations(detail.getAllocations().stream().map(item -> {
            ReimbursementSaveRequest.AllocationRequest allocation = new ReimbursementSaveRequest.AllocationRequest();
            BeanUtils.copyProperties(item, allocation);
            return allocation;
        }).collect(Collectors.toList()));
        return request;
    }

    private FkReimMain getMainByReimNo(String reimNo) {
        if (!StringUtils.hasText(reimNo)) {
            throw new IllegalArgumentException("报销单号不能为空");
        }
        FkReimMain main = mainService.getOne(new LambdaQueryWrapper<FkReimMain>().eq(FkReimMain::getReimNo, reimNo), false);
        if (main == null) {
            throw new IllegalArgumentException("报销单不存在：" + reimNo);
        }
        return main;
    }

    private void deleteChildren(Long mainId) {
        allowanceDayService.remove(new LambdaQueryWrapper<FkReimAllowanceDay>().eq(FkReimAllowanceDay::getMainId, mainId));
        itineraryService.remove(new LambdaQueryWrapper<FkReimItinerary>().eq(FkReimItinerary::getMainId, mainId));
        allocationService.remove(new LambdaQueryWrapper<FkReimAllocation>().eq(FkReimAllocation::getMainId, mainId));
    }

    private boolean isUsableItinerary(ReimbursementSaveRequest.ItineraryRequest item) {
        return item != null
                && StringUtils.hasText(item.getStartCityName())
                && StringUtils.hasText(item.getEndCityName())
                && item.getStartDate() != null
                && item.getEndDate() != null;
    }

    private BigDecimal checkedAmount(Integer selected, BigDecimal amount, BigDecimal standard, String label) {
        BigDecimal value = selected(selected) == 1 ? nvl(amount) : ZERO;
        BigDecimal limit = nvl(standard);
        if (value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(label + "不能小于0");
        }
        if (value.compareTo(limit) > 0) {
            throw new IllegalArgumentException(label + "不能超过标准金额" + limit);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private int selected(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }

    private BigDecimal normalizeRatio(BigDecimal ratio) {
        if (ratio == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = ratio.compareTo(BigDecimal.ONE) > 0 ? ratio.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP) : ratio;
        return normalized.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String weekName(DayOfWeek dayOfWeek) {
        Map<DayOfWeek, String> names = new HashMap<>();
        names.put(DayOfWeek.MONDAY, "星期一");
        names.put(DayOfWeek.TUESDAY, "星期二");
        names.put(DayOfWeek.WEDNESDAY, "星期三");
        names.put(DayOfWeek.THURSDAY, "星期四");
        names.put(DayOfWeek.FRIDAY, "星期五");
        names.put(DayOfWeek.SATURDAY, "星期六");
        names.put(DayOfWeek.SUNDAY, "星期日");
        return names.get(dayOfWeek);
    }

    private String nextReimNo() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = "reim:no:" + day;
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                Long seq = redisTemplate.opsForValue().increment(key);
                if (seq != null) {
                    return "CLBX" + day + String.format("%04d", seq);
                }
            }
        } catch (Exception ignored) {
            // Local training environments often do not start Redis; timestamp fallback keeps creation usable.
        }
        return "CLBX" + day + DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now());
    }

    private void publishEvent(String topic, String reimNo) {
        if (!kafkaEnabled) {
            return;
        }
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate != null) {
            kafkaTemplate.send(topic, reimNo);
        }
    }
}
