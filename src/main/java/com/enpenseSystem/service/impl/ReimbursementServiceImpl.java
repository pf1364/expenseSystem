package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.enpenseSystem.exception.ResourceNotFoundException;
import com.enpenseSystem.exception.StatusConflictException;
import com.enpenseSystem.service.FkCityAllowanceService;
import com.enpenseSystem.service.FkReimAllocationService;
import com.enpenseSystem.service.FkReimAllowanceDayService;
import com.enpenseSystem.service.FkReimItineraryService;
import com.enpenseSystem.service.FkReimMainService;
import com.enpenseSystem.service.support.ReimbursementDetailCache;
import com.enpenseSystem.service.ReimbursementService;
import com.enpenseSystem.service.support.ReimbursementDetailAssembler;
import com.enpenseSystem.service.support.ReimbursementNoGenerator;
import com.enpenseSystem.service.support.AllocationCalculator;
import com.enpenseSystem.service.support.AllowanceCalculator;
import com.enpenseSystem.utils.ReimbursementConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 差旅报销单核心业务实现。
 *
 * <p>该类负责五张核心业务表的组合读写，并承担以下职责：</p>
 * <ul>
 *     <li>报销单主表、行程、每日补助和费用分摊的事务保存；</li>
 *     <li>对客户端传入的日期、城市、金额和分摊数据进行后端校验；</li>
 *     <li>根据每日补助重新计算主表汇总金额；</li>
 *     <li>通过 Redis 生成报销单号，通过 Kafka 发布提交和作废事件；</li>
 *     <li>组装分页、详情和个人统计所需的返回对象。</li>
 * </ul>
 *
 * <p>金额类数据默认不信任客户端。补助标准从城市标准表读取，
 * 每日合计和主表合计均由后端重新计算。</p>
 */
@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    /** 金额计算使用的两位小数零值，避免到处重复创建 BigDecimal。 */
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    /** 报销单主表服务。 */
    private final FkReimMainService mainService;
    /** 行程表服务。 */
    private final FkReimItineraryService itineraryService;
    /** 每日补助明细表服务。 */
    private final FkReimAllowanceDayService allowanceDayService;
    /** 费用归属及分摊表服务。 */
    private final FkReimAllocationService allocationService;
    /** 城市补助标准表服务。 */
    private final FkCityAllowanceService cityAllowanceService;
    /** Kafka 为可选依赖，仅在配置开启时发送业务事件。 */
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    /** 报销单完整详情缓存，封装 Redis Key、JSON、TTL 和事务提交后失效。 */
    private final ReimbursementDetailCache detailCache;
    /** 详情对象组装器：负责 Entity、VO 和保存请求之间的转换。 */
    private final ReimbursementDetailAssembler detailAssembler;
    /** 报销单号生成器：负责 Redis 自增和本地时间戳降级。 */
    private final ReimbursementNoGenerator reimNoGenerator;
    /** 费用分摊计算器：负责比例、金额和提交合计校验。 */
    private final AllocationCalculator allocationCalculator;
    /** 每日补助计算器：负责城市标准查询、补助生成和金额校验。 */
    private final AllowanceCalculator allowanceCalculator;

    /** 是否启用 Kafka 消息发送，默认关闭。 */
    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public ReimbursementServiceImpl(FkReimMainService mainService,
                                    FkReimItineraryService itineraryService,
                                    FkReimAllowanceDayService allowanceDayService,
                                    FkReimAllocationService allocationService,
                                    FkCityAllowanceService cityAllowanceService,
                                    ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
                                    ReimbursementDetailCache detailCache,
                                    ReimbursementDetailAssembler detailAssembler,
                                    ReimbursementNoGenerator reimNoGenerator,
                                    AllocationCalculator allocationCalculator,
                                    AllowanceCalculator allowanceCalculator) {
        this.mainService = mainService;
        this.itineraryService = itineraryService;
        this.allowanceDayService = allowanceDayService;
        this.allocationService = allocationService;
        this.cityAllowanceService = cityAllowanceService;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.detailCache = detailCache;
        this.detailAssembler = detailAssembler;
        this.reimNoGenerator = reimNoGenerator;
        this.allocationCalculator = allocationCalculator;
        this.allowanceCalculator = allowanceCalculator;
    }

    /**
     * 动态拼装查询条件并分页查询报销单主表。
     *
     * <p>列表所需的公司名称和金额已冗余在主表中，因此分页时不需要
     * 再逐条查询分摊表，避免产生 N+1 查询。</p>
     */
    @Override
    public PageData<ReimbursementPageVO> page(ReimbursementPageQuery query) {
        // 只有参数有值时才拼接对应 SQL 条件；单号、标题、事由和公司使用模糊查询。
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
            // 一个选择值同时兼容“姓名”和“工号”，两者之间使用 OR。
            wrapper.and(w -> w.eq(FkReimMain::getReimburserName, query.getReimburserKeyword())
                    .or()
                    .eq(FkReimMain::getReimburserNo, query.getReimburserKeyword()));
        }

        // MyBatis-Plus 根据页码和页大小执行分页 SQL，并额外查询符合条件的总记录数。
        Page<FkReimMain> page = mainService.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        // Entity 仅用于数据库映射，对外返回前转换为列表专用 VO。
        List<ReimbursementPageVO> records = page.getRecords().stream().map(detailAssembler::toPageVO).collect(Collectors.toList());
        return new PageData<>(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    /**
     * 查询报销单完整详情。
     *
     * <p>大致流程：先按单号查询主表，再分别查询行程、每日补助和费用分摊；
     * 随后在内存中把每日补助按行程 ID 分组，最终组装成前端需要的嵌套结构。
     * 整个过程是固定次数查询，不会为每条行程单独查询一次补助。</p>
     */
    @Override
    public ReimbursementDetailVO detail(String reimNo) {
        // 三态缓存读取：正常命中直接返回，空值命中直接返回 404，只有 MISS 查询数据库。
        ReimbursementDetailCache.LookupResult cacheResult = detailCache.lookup(reimNo);
        if (cacheResult.status() == ReimbursementDetailCache.LookupStatus.HIT) {
            return cacheResult.detail();
        }
        if (cacheResult.status() == ReimbursementDetailCache.LookupStatus.NULL_HIT) {
            throw new ResourceNotFoundException("报销单不存在：" + reimNo);
        }
        // 尝试查询数据库，查询结果正常时写入缓存，查询结果不存在时写入空值缓存，查询异常时不写缓存。
        try {
            ReimbursementDetailVO detail = loadDetailFromDatabase(reimNo);
            detailCache.put(reimNo, detail);
            return detail;
        } catch (ResourceNotFoundException exception) {
            // 不存在的单号短暂缓存 2 分钟，避免相同恶意请求持续穿透到数据库。
            detailCache.putNull(reimNo);
            throw exception;
        }
    }

    /**
     * 固定查询四次数据库并组装完整详情。
     *
     * <p>该方法不读取或写入缓存，供缓存未命中和提交草稿校验使用。</p>
     */
    private ReimbursementDetailVO loadDetailFromDatabase(String reimNo) {
        // 先找到主表记录并取得主键，后续三张子表都通过 main_id 关联该主键。
        FkReimMain main = getMainByReimNo(reimNo);

        // 查询当前报销单的全部行程，按出发日期和主键排序，保证前端显示顺序稳定。
        List<FkReimItinerary> itineraries = itineraryService.list(new LambdaQueryWrapper<FkReimItinerary>()
                .eq(FkReimItinerary::getMainId, main.getId())
                .orderByAsc(FkReimItinerary::getStartDate)
                .orderByAsc(FkReimItinerary::getId));

        // 一次性查询整张报销单的全部每日补助，避免在遍历行程时反复访问数据库。
        List<FkReimAllowanceDay> days = allowanceDayService.list(new LambdaQueryWrapper<FkReimAllowanceDay>()
                .eq(FkReimAllowanceDay::getMainId, main.getId())
                .orderByAsc(FkReimAllowanceDay::getAllowanceDate)
                .orderByAsc(FkReimAllowanceDay::getId));

        // 查询费用归属及分摊，sort_no 决定前端展示顺序。
        List<FkReimAllocation> allocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>()
                .eq(FkReimAllocation::getMainId, main.getId())
                .orderByAsc(FkReimAllocation::getSortNo)
                .orderByAsc(FkReimAllocation::getId));

        // 四次数据库查询得到的是平铺结构，交给组装器统一转换成前端需要的嵌套详情结构。
        return detailAssembler.assemble(main, itineraries, days, allocations);
    }

    /**
     * 创建草稿。
     *
     * <p>草稿允许基础信息不完整，但有效行程中的日期、城市和金额仍会在写库前校验。
     * 方法使用事务，主表或任一子表保存失败时全部回滚。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse createDraft(ReimbursementSaveRequest request) {
        return saveNew(request, ReimbursementConstants.STATUS_DRAFT);
    }

    /**
     * 创建并提交一张新报销单。
     *
     * <p>先执行完整提交校验，再在同一数据库事务中保存主表和所有子表。
     * 保存成功后按配置决定是否发布 Kafka 提交事件。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse createAndSubmit(ReimbursementSaveRequest request) {
        validateForSubmit(request);
        ReimbursementSaveResponse response = saveNew(request, ReimbursementConstants.STATUS_SUBMITTED);
        publishEvent("reim.bill.submitted", response.getReimNo());
        return response;
    }

    /**
     * 更新已有报销单。
     *
     * <p>先校验单据状态，再更新主表并按 ID 差异同步所有子表，
     * 最后从数据库中的每日补助重新汇总主表金额。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse update(String reimNo, ReimbursementSaveRequest request) {
        // 路径中的单号是更新目标，不能相信请求体中可能被篡改的 reimNo。
        FkReimMain main = getMainByReimNo(reimNo);
        // 只有草稿状态可以修改，已提交或已作废的单据不允许编辑
        if (!ReimbursementConstants.STATUS_DRAFT.equals(main.getBillStatus())) {
            throw new StatusConflictException("只有草稿状态可以修改");
        }
        // 乐观锁校验：客户端必须回传详情接口给出的 version，后端比对后拒绝并发冲突的保存。
        if (request.getVersion() != null && !request.getVersion().equals(main.getVersion())) {
            throw new StatusConflictException("该报销单已被他人修改，请刷新后重新编辑");
        }
        request.setReimNo(reimNo);
        // 保留数据库中的当前状态，更新接口不接受客户端自行修改状态。
        fillMain(main, request, main.getBillStatus());
        saveChildren(main, request);
        refreshMainTotals(main);
        detailCache.evictAfterCommit(reimNo);
        return new ReimbursementSaveResponse(main.getReimNo(), main.getBillStatus(), main.getBillStatusName(), main.getVersion());
    }

    /**
     * 提交数据库中已有的草稿数据。
     *
     * <p>先通过详情查询还原完整请求对象，再复用新建提交的校验规则，
     * 校验通过后只更新主表状态和提交时间，并发布提交事件。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse submitDraft(String reimNo, Integer version) {
        FkReimMain main = getMainByReimNo(reimNo);

        // 只有草稿状态可以提交
        if (!ReimbursementConstants.STATUS_DRAFT.equals(main.getBillStatus())) {
            throw new StatusConflictException("只有草稿状态可以提交");
        }

        // 乐观锁校验：客户端必须回传详情接口给出的 version
        if (version != null && !version.equals(main.getVersion())) {
            throw new StatusConflictException("该报销单已被他人修改，请刷新后重新提交");
        }

        // 详情 VO 中已经包含全部子表数据，将其转换为保存请求后可复用统一校验逻辑。
        // 提交属于写业务，必须直接读取数据库中的最新状态，不能依赖可能即将失效的缓存。
        ReimbursementDetailVO detail = loadDetailFromDatabase(reimNo);
        ReimbursementSaveRequest request = detailAssembler.toSaveRequest(detail);
        validateForSubmit(request);

        // 带状态和版本条件的原子更新，防止并发重复提交和乐观锁冲突
        Integer incrementedVersion = main.getVersion() == null ? 1 : main.getVersion() + 1;
        LambdaUpdateWrapper<FkReimMain> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FkReimMain::getId, main.getId())
               .eq(FkReimMain::getBillStatus, ReimbursementConstants.STATUS_DRAFT)
               .eq(main.getVersion() != null, FkReimMain::getVersion, main.getVersion())
               .set(FkReimMain::getBillStatus, ReimbursementConstants.STATUS_SUBMITTED)
               .set(FkReimMain::getBillStatusName, ReimbursementConstants.STATUS_SUBMITTED_NAME)
               .set(FkReimMain::getSubmittedAt, LocalDateTime.now())
               .set(FkReimMain::getUpdatedAt, LocalDateTime.now())
               .set(FkReimMain::getVersion, incrementedVersion);
        boolean updated = mainService.update(wrapper);
        if (!updated) {
            throw new StatusConflictException("提交失败：单据状态已变更，请刷新后重试");
        }

        publishEvent("reim.bill.submitted", reimNo);
        detailCache.evictAfterCommit(reimNo);
        return new ReimbursementSaveResponse(reimNo,
                ReimbursementConstants.STATUS_SUBMITTED, ReimbursementConstants.STATUS_SUBMITTED_NAME, incrementedVersion);
    }

    /**
     * 深度复制一张报销单为新草稿。
     *
     * <p>复制时必须重新生成主表和子表主键。每日补助依赖行程主键，
     * 因此会维护“源行程 ID -> 新行程 ID”的映射关系。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimbursementSaveResponse copy(String reimNo) {
        // 查询源主表；整个复制过程处于同一事务，任一子表失败都会回滚。
        FkReimMain source = getMainByReimNo(reimNo);
        LocalDateTime now = LocalDateTime.now();

        // 复制主表基础字段，但清空主键、生成新单号，并强制设置为草稿。
        FkReimMain target = new FkReimMain();
        BeanUtils.copyProperties(source, target);
        target.setId(null);
        target.setReimNo(reimNoGenerator.nextReimNo());
        target.setVersion(1);
        target.setBillStatus(ReimbursementConstants.STATUS_DRAFT);
        target.setBillStatusName(ReimbursementConstants.STATUS_DRAFT_NAME);
        target.setSubmittedAt(null);
        target.setCreatedAt(now);
        target.setUpdatedAt(now);
        mainService.save(target);

        // 先复制行程，并记录每个旧行程 ID 对应的新行程 ID。
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

        // 复制每日补助时，用映射表替换 itinerary_id，使补助关联到新行程。
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

        // 分摊只关联主表，因此替换 main_id 后即可复制。
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

        // 不直接沿用源主表汇总字段，而是根据已复制的子表重新计算一次。
        refreshMainTotals(target);
        // 新单号可能在创建前被恶意请求并留下空值缓存，事务提交后必须清理。
        detailCache.evictAfterCommit(target.getReimNo());
        return new ReimbursementSaveResponse(target.getReimNo(), target.getBillStatus(), target.getBillStatusName(), target.getVersion());
    }

    /**
     * 删除草稿及其全部子表数据。
     *
     * <p>先删除子表再删除主表，事务保证不会出现只删除一部分的数据。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(String reimNo) {
        FkReimMain main = getMainByReimNo(reimNo);
        if (!ReimbursementConstants.STATUS_DRAFT.equals(main.getBillStatus())) {
            throw new IllegalArgumentException("只能删除草稿单据");
        }
        deleteChildren(main.getId());
        mainService.removeById(main.getId());
        detailCache.evictAfterCommit(reimNo);
    }

    /**
     * 作废非草稿单据。
     *
     * <p>作废是状态变更，不会删除业务数据。状态更新后可发布 Kafka 作废事件。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidBill(String reimNo) {
        FkReimMain main = getMainByReimNo(reimNo);
        if (ReimbursementConstants.STATUS_DRAFT.equals(main.getBillStatus())) {
            throw new StatusConflictException("草稿请直接删除，不需要作废");
        }
        if (ReimbursementConstants.STATUS_VOIDED.equals(main.getBillStatus())) {
            throw new StatusConflictException("该单据已作废，不能重复操作");
        }
        main.setBillStatus(ReimbursementConstants.STATUS_VOIDED);
        main.setBillStatusName(ReimbursementConstants.STATUS_VOIDED_NAME);
        main.setUpdatedAt(LocalDateTime.now());
        mainService.updateById(main);
        publishEvent("reim.bill.voided", reimNo);
        detailCache.evictAfterCommit(reimNo);
    }

    /**
     * 按目的地城市标准生成一段日期范围内的每日补助。
     *
     * <p>开始日和结束日都包含在结果中。生成时三种补助默认全部选中，
     * 初始实报金额等于数据库中的城市标准。</p>
     */
    @Override
    public List<ReimbursementSaveRequest.AllowanceDayRequest> generateAllowanceDays(AllowanceGenerateRequest request) {
        return allowanceCalculator.generateAllowanceDays(request);
    }

    /** 查询全部城市标准，按城市等级、城市名称升序返回。 */
    @Override
    public List<FkCityAllowance> listCityAllowances() {
        return cityAllowanceService.list(new LambdaQueryWrapper<FkCityAllowance>().orderByAsc(FkCityAllowance::getCityLevel).orderByAsc(FkCityAllowance::getCityName));
    }

    /**
     * 计算个人报销统计。
     *
     * <p>当前实现先查询该人员全部非作废主表记录，在内存中计算数量、总金额和月度数据；
     * 再批量查询这些单据的公司分摊，计算公司金额占比。</p>
     */
    @Override
    public PersonalStatisticsVO personalStatistics(String reimburserName, String reimburserNo) {
        // 姓名和工号有值时分别使用精确查询，并排除已作废单据。
        LambdaQueryWrapper<FkReimMain> wrapper = new LambdaQueryWrapper<FkReimMain>()
                .eq(StringUtils.hasText(reimburserName), FkReimMain::getReimburserName, reimburserName)
                .eq(StringUtils.hasText(reimburserNo), FkReimMain::getReimburserNo, reimburserNo)
                .ne(FkReimMain::getBillStatus, ReimbursementConstants.STATUS_VOIDED)
                .orderByDesc(FkReimMain::getCreatedAt);
        if (!StringUtils.hasText(reimburserName) && !StringUtils.hasText(reimburserNo)) {
            throw new IllegalArgumentException("请选择报销人");
        }

        // 查询结果已按创建时间倒序，后面可直接截取最近 10 张单据。
        List<FkReimMain> mains = mainService.list(wrapper);
        PersonalStatisticsVO vo = new PersonalStatisticsVO();

        // 在内存中统计总数、各状态数量和非作废单据金额总和。
        vo.setTotalCount((long) mains.size());
        vo.setDraftCount(mains.stream().filter(item -> ReimbursementConstants.STATUS_DRAFT.equals(item.getBillStatus())).count());
        vo.setSubmittedCount(mains.stream().filter(item -> ReimbursementConstants.STATUS_SUBMITTED.equals(item.getBillStatus())).count());
        vo.setTotalAmount(mains.stream().map(FkReimMain::getAllowanceAmount).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add));

        // 按 created_at 的 yyyy-MM 分组，生成柱状图所需的月度金额。
        Map<String, BigDecimal> monthMap = mains.stream()
                .filter(item -> item.getCreatedAt() != null)
                .collect(Collectors.groupingBy(item -> item.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new,
                        Collectors.mapping(item -> allowanceCalculator.nvl(item.getAllowanceAmount()), Collectors.reducing(ZERO, BigDecimal::add))));
        monthMap.forEach((month, amount) -> vo.getMonthlyAmounts().add(new PersonalStatisticsVO.MonthlyAmount(month, amount)));

        // 一次查询该人员所有报销单的公司分摊，再按公司名称汇总，供饼图使用。
        Set<Long> mainIds = mains.stream().map(FkReimMain::getId).collect(Collectors.toSet());
        if (!mainIds.isEmpty()) {
            List<FkReimAllocation> allocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>()
                    .in(FkReimAllocation::getMainId, mainIds)
                    .eq(FkReimAllocation::getAllocationOwnerType, "COMPANY"));
            Map<String, BigDecimal> companyMap = allocations.stream()
                    .collect(Collectors.groupingBy(FkReimAllocation::getAllocationOwnerName,
                            LinkedHashMap::new,
                            Collectors.mapping(item -> allocationCalculator.nvl(item.getAllocationAmount()), Collectors.reducing(ZERO, BigDecimal::add))));
            companyMap.forEach((name, amount) -> vo.getCompanyShares().add(new PersonalStatisticsVO.CompanyShare(name, amount)));
        }
        // mains 已倒序排列，前 10 条就是最近报销单。
        vo.setRecentBills(mains.stream().limit(10).map(detailAssembler::toPageVO).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 新建报销单的共用保存流程。
     *
     * <p>创建草稿和创建并提交都会调用本方法，区别仅在目标状态和是否要求完整校验。</p>
     */
    private ReimbursementSaveResponse saveNew(ReimbursementSaveRequest request, String status) {
        if (ReimbursementConstants.STATUS_SUBMITTED.equals(status)) {
            validateForSubmit(request);
        }
        FkReimMain main = new FkReimMain();
        // 单号只能由后端生成，避免客户端伪造或重复。
        main.setReimNo(reimNoGenerator.nextReimNo());
        fillMain(main, request, status);
        // 必须先保存主表取得自增 ID，子表才能写入 main_id。
        mainService.save(main);
        saveChildren(main, request);
        // 子表保存完成后，以数据库数据为准更新主表汇总字段。
        refreshMainTotals(main);
        // 清除该新单号在创建前可能已经产生的空值缓存。
        detailCache.evictAfterCommit(main.getReimNo());
        return new ReimbursementSaveResponse(main.getReimNo(), main.getBillStatus(), main.getBillStatusName(), main.getVersion());
    }

    /**
     * 把请求中的基础信息写入主表实体。
     *
     * <p>该方法不执行数据库操作，只负责字段赋值、状态控制和时间维护。</p>
     */
    private void fillMain(FkReimMain main, ReimbursementSaveRequest request, String status) {
        LocalDateTime now = LocalDateTime.now();
        main.setBillStatus(status);
        main.setBillStatusName(ReimbursementConstants.STATUS_SUBMITTED.equals(status) ? ReimbursementConstants.STATUS_SUBMITTED_NAME : ReimbursementConstants.STATUS_DRAFT_NAME);
        main.setBillType(ReimbursementConstants.BILL_TYPE);
        main.setBillTypeName(ReimbursementConstants.BILL_TYPE_NAME);
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
        // 乐观锁版本号管理：新建单据版本从 1 开始，每次更新递增
        if (main.getVersion() == null) {
            main.setVersion(1);
        } else {
            main.setVersion(main.getVersion() + 1);
        }
        main.setUpdatedAt(now);
        if (ReimbursementConstants.STATUS_SUBMITTED.equals(status) && main.getSubmittedAt() == null) {
            main.setSubmittedAt(now);
        }
        main.setAllowanceAmount(allowanceCalculator.nvl(main.getAllowanceAmount()));
        main.setMealAmount(allowanceCalculator.nvl(main.getMealAmount()));
        main.setTrafficAmount(allowanceCalculator.nvl(main.getTrafficAmount()));
        main.setCommunicationAmount(allowanceCalculator.nvl(main.getCommunicationAmount()));
    }

    /**
     * 同步报销单的全部子表。
     *
     * <p>请求中的子表集合被视为最终状态：已有 ID 更新、无 ID 新增，
     * 数据库存在但请求未带回的记录删除。行程完成后再同步费用分摊。</p>
     */
    private void saveChildren(FkReimMain main, ReimbursementSaveRequest request) {
        // 子表使用“请求即最终状态”的同步方式：有 ID 更新、无 ID 新增、未带回的旧记录删除。
        List<ReimbursementSaveRequest.ItineraryRequest> itineraryRequests = request.getItineraries() == null ? List.of() : request.getItineraries();

        // 查询数据库中原有行程，用于判断请求中的行程是新增还是更新，以及找出需要删除的旧行程。
        List<FkReimItinerary> dbItineraries = itineraryService.list(new LambdaQueryWrapper<FkReimItinerary>().eq(FkReimItinerary::getMainId, main.getId()));
        Map<Long, FkReimItinerary> dbItineraryMap = dbItineraries.stream().collect(Collectors.toMap(FkReimItinerary::getId, item -> item));
        Set<Long> keepItineraryIds = new HashSet<>();

        for (ReimbursementSaveRequest.ItineraryRequest item : itineraryRequests) {
            // 草稿允许存在尚未填写完整的行程；这种占位数据不写入数据库。
            if (!isUsableItinerary(item)) {
                continue;
            }
            // ID 属于当前报销单时复用原实体更新，否则创建新实体。
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

        // 原数据库行程中没有出现在 keep 集合内的，表示用户已从页面删除。
        List<Long> deleteItineraryIds = dbItineraries.stream()
                .map(FkReimItinerary::getId)
                .filter(id -> !keepItineraryIds.contains(id))
                .collect(Collectors.toList());
        if (!deleteItineraryIds.isEmpty()) {
            // 先删行程下的每日补助，再删行程，避免留下无父记录的明细。
            allowanceDayService.remove(new LambdaQueryWrapper<FkReimAllowanceDay>().in(FkReimAllowanceDay::getItineraryId, deleteItineraryIds));
            itineraryService.removeByIds(deleteItineraryIds);
        }

        saveAllocations(main, request.getAllocations());
    }

    /**
     * 同步一条行程下的每日补助。
     *
     * <p>前端没有传明细时自动生成；传了明细时先校验日期、城市和金额，
     * 再按 ID 执行新增、更新和删除。</p>
     */
    private void saveAllowanceDays(FkReimMain main, FkReimItinerary itinerary, ReimbursementSaveRequest.ItineraryRequest request) {
        List<ReimbursementSaveRequest.AllowanceDayRequest> requestedDays = request.getAllowanceDays();
        if (requestedDays == null || requestedDays.isEmpty()) {
            // 用行程目的地和首尾日期生成默认全额补助。
            AllowanceGenerateRequest generateRequest = new AllowanceGenerateRequest();
            generateRequest.setEndCityCode(itinerary.getEndCityCode());
            generateRequest.setEndCityName(itinerary.getEndCityName());
            generateRequest.setStartDate(itinerary.getStartDate());
            generateRequest.setEndDate(itinerary.getEndDate());
            requestedDays = generateAllowanceDays(generateRequest);
        }

        // 客户端可以绕过页面限制，写库前必须重新校验补助日期和城市。
        allowanceCalculator.validateAllowanceDaysForItinerary(request, requestedDays);

        // 查询当前行程原有的每日明细，用于进行差异同步。
        List<FkReimAllowanceDay> dbDays = allowanceDayService.list(new LambdaQueryWrapper<FkReimAllowanceDay>().eq(FkReimAllowanceDay::getItineraryId, itinerary.getId()));
        Map<Long, FkReimAllowanceDay> dbDayMap = dbDays.stream().collect(Collectors.toMap(FkReimAllowanceDay::getId, item -> item));
        Set<Long> keepIds = new HashSet<>();
        for (ReimbursementSaveRequest.AllowanceDayRequest dayRequest : requestedDays) {
            FkReimAllowanceDay day = dayRequest.getId() != null && dbDayMap.containsKey(dayRequest.getId()) ? dbDayMap.get(dayRequest.getId()) : new FkReimAllowanceDay();
            allowanceCalculator.fillAllowanceDay(day, main.getId(), itinerary.getId(), dayRequest);
            if (day.getId() == null) {
                allowanceDayService.save(day);
            } else {
                allowanceDayService.updateById(day);
            }
            keepIds.add(day.getId());
        }
        // 数据库存在但请求未带回的日期明细视为用户取消，执行物理删除。
        List<Long> deleteIds = dbDays.stream().map(FkReimAllowanceDay::getId).filter(id -> !keepIds.contains(id)).collect(Collectors.toList());
        if (!deleteIds.isEmpty()) {
            allowanceDayService.removeByIds(deleteIds);
        }
    }

    /**
     * 同步费用归属及分摊列表。
     *
     * <p>空归属名称的草稿占位行不保存；有效行在写库前校验单行比例和金额边界。</p>
     */
    private void saveAllocations(FkReimMain main, List<ReimbursementSaveRequest.AllocationRequest> allocationRequests) {
        List<ReimbursementSaveRequest.AllocationRequest> requests = allocationRequests == null ? List.of() : allocationRequests;

        // 查询数据库原有分摊，用于判断新增、更新和删除。
        List<FkReimAllocation> dbAllocations = allocationService.list(new LambdaQueryWrapper<FkReimAllocation>().eq(FkReimAllocation::getMainId, main.getId()));
        Map<Long, FkReimAllocation> dbMap = dbAllocations.stream().collect(Collectors.toMap(FkReimAllocation::getId, item -> item));
        Set<Long> keepIds = new HashSet<>();
        int index = 1;
        for (ReimbursementSaveRequest.AllocationRequest request : requests) {
            // 草稿可保留前端空白行，但空白行不落库。
            if (!StringUtils.hasText(request.getAllocationOwnerName())) {
                continue;
            }
            // 草稿虽可不完整，也不能写入负金额或超过 100% 的单行比例。
            allocationCalculator.validateDraftAllocation(
                    allocationCalculator.normalizeRatio(request.getAllocationRatio()),
                    allocationCalculator.nvl(request.getAllocationAmount()));
            FkReimAllocation allocation = request.getId() != null && dbMap.containsKey(request.getId()) ? dbMap.get(request.getId()) : new FkReimAllocation();
            fillAllocation(allocation, main.getId(), request, index++);
            if (allocation.getId() == null) {
                allocationService.save(allocation);
            } else {
                allocationService.updateById(allocation);
            }
            keepIds.add(allocation.getId());
        }
        // 未带回的旧分摊表示用户已删除。
        List<Long> deleteIds = dbAllocations.stream().map(FkReimAllocation::getId).filter(id -> !keepIds.contains(id)).collect(Collectors.toList());
        if (!deleteIds.isEmpty()) {
            allocationService.removeByIds(deleteIds);
        }
    }

    /**
     * 根据子表中的权威数据刷新主表汇总字段。
     *
     * <p>金额来自每日补助表，公司名称来自分摊表。前端传入的总金额和公司名称
     * 只用于临时展示，最终数据库值以本方法计算结果为准。</p>
     */
    private void refreshMainTotals(FkReimMain main) {
        // 主表金额只从已落库的每日补助重新汇总，不使用客户端传入的合计值。
        List<FkReimAllowanceDay> days = allowanceDayService.list(new LambdaQueryWrapper<FkReimAllowanceDay>().eq(FkReimAllowanceDay::getMainId, main.getId()));
        // 分别汇总三类补助，再得到整张报销单的补助总金额。
        BigDecimal mealAmount = allowanceCalculator.sum(days.stream().map(FkReimAllowanceDay::getMealAmount).collect(Collectors.toList()));
        BigDecimal trafficAmount = allowanceCalculator.sum(days.stream().map(FkReimAllowanceDay::getTrafficAmount).collect(Collectors.toList()));
        BigDecimal communicationAmount = allowanceCalculator.sum(days.stream().map(FkReimAllowanceDay::getCommunicationAmount).collect(Collectors.toList()));
        BigDecimal total = mealAmount.add(trafficAmount).add(communicationAmount).setScale(2, RoundingMode.HALF_UP);

        // 查询分摊表并提取不同归属类型的名称，写入主表冗余字段以优化列表查询。
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

    /**
     * 执行报销单提交前的完整业务校验。
     *
     * <p>草稿保存可以缺少字段，但进入已提交状态前必须具备完整基础信息、
     * 至少一条有效行程、合法每日补助和总额一致的费用分摊。</p>
     */
    private void validateForSubmit(ReimbursementSaveRequest request) {
        // 单字段必填校验已由 Bean Validation（SubmitGroup）接管，此处仅做跨字段/跨行/需查数据库的校验。
        request.getItineraries().forEach(this::validateItineraryForSubmit);
        allocationCalculator.validateForSubmit(request.getAllocations(), allowanceCalculator.estimateTotalAmount(request));
    }

    /**
     * 校验单条行程，并在未传每日补助时自动生成明细。
     */
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
            days = allowanceCalculator.generateAllowanceDays(generateRequest);
            item.setAllowanceDays(days);
        }
        allowanceCalculator.validateAllowanceDaysForItinerary(item, days);
        // 使用临时 Entity 触发统一金额校验，并把标准金额及日合计回写到请求对象。
        days.forEach(day -> allowanceCalculator.fillAllowanceDay(new FkReimAllowanceDay(), 0L, 0L, day));
    }

    /**
     * 把行程请求字段写入行程实体，并由后端计算行程天数和默认路线文本。
     */
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

    /**
     * 把费用分摊请求写入实体，并统一比例精度和默认排序号。
     */
    private void fillAllocation(FkReimAllocation allocation, Long mainId, ReimbursementSaveRequest.AllocationRequest request, int index) {
        LocalDateTime now = LocalDateTime.now();
        allocation.setMainId(mainId);
        allocation.setAllocationOwnerType(StringUtils.hasText(request.getAllocationOwnerType()) ? request.getAllocationOwnerType() : "COMPANY");
        allocation.setAllocationOwnerId(text(request.getAllocationOwnerId()));
        allocation.setAllocationOwnerNo(text(request.getAllocationOwnerNo()));
        allocation.setAllocationOwnerName(text(request.getAllocationOwnerName()));
        allocation.setBusinessId(text(request.getBusinessId()));
        allocation.setBusinessName(text(request.getBusinessName()));
        allocation.setAllocationRatio(allocationCalculator.normalizeRatio(request.getAllocationRatio()));
        allocation.setAllocationAmount(allocationCalculator.nvl(request.getAllocationAmount()));
        allocation.setSortNo(request.getSortNo() == null ? index : request.getSortNo());
        if (allocation.getCreatedAt() == null) {
            allocation.setCreatedAt(now);
        }
        allocation.setUpdatedAt(now);
    }

    /**
     * 按唯一报销单号查询主表，不存在时统一抛出业务异常。
     */
    private FkReimMain getMainByReimNo(String reimNo) {
        if (!StringUtils.hasText(reimNo)) {
            throw new IllegalArgumentException("报销单号不能为空");
        }
        FkReimMain main = mainService.getOne(new LambdaQueryWrapper<FkReimMain>().eq(FkReimMain::getReimNo, reimNo), false);
        if (main == null) {
            throw new ResourceNotFoundException("报销单不存在：" + reimNo);
        }
        return main;
    }

    /**
     * 删除指定主表下的全部子数据。
     *
     * <p>删除顺序为每日补助、行程、费用分摊；调用方处于事务中。</p>
     */
    private void deleteChildren(Long mainId) {
        allowanceDayService.remove(new LambdaQueryWrapper<FkReimAllowanceDay>().eq(FkReimAllowanceDay::getMainId, mainId));
        itineraryService.remove(new LambdaQueryWrapper<FkReimItinerary>().eq(FkReimItinerary::getMainId, mainId));
        allocationService.remove(new LambdaQueryWrapper<FkReimAllocation>().eq(FkReimAllocation::getMainId, mainId));
    }

    /** 判断草稿中的行程是否已经具备可保存的城市和日期信息。 */
    private boolean isUsableItinerary(ReimbursementSaveRequest.ItineraryRequest item) {
        return item != null
                && StringUtils.hasText(item.getStartCityName())
                && StringUtils.hasText(item.getEndCityName())
                && item.getStartDate() != null
                && item.getEndDate() != null;
    }

    /** 将空字符串字段转换为空串，避免实体中出现不必要的 null。 */
    private String text(String value) {
        return value == null ? "" : value;
    }

    /**
     * 按配置向 Kafka 发布报销单领域事件。
     *
     * <p>当前消息体只有报销单号。Kafka 默认关闭，关闭或未创建 KafkaTemplate 时直接跳过。</p>
     */
    private void publishEvent(String topic, String reimNo) {
        if (!kafkaEnabled) {
            return;
        }
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate != null) {
            // send 为异步调用；当前实现未等待发送结果，也未实现失败补偿。
            kafkaTemplate.send(topic, reimNo);
        }
    }
}
