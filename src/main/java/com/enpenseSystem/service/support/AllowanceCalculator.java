package com.enpenseSystem.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enpenseSystem.dto.AllowanceGenerateRequest;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.entity.FkCityAllowance;
import com.enpenseSystem.entity.FkReimAllowanceDay;
import com.enpenseSystem.service.FkCityAllowanceService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 每日补助计算器。
 *
 * <p>补助金额相关数据默认不信任前端。该组件会根据城市补助标准表重新读取标准金额，
 * 校验用户实报金额不能超过标准，再把可信金额回写到实体和请求对象中。</p>
 */
@Component
public class AllowanceCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final FkCityAllowanceService cityAllowanceService;

    public AllowanceCalculator(FkCityAllowanceService cityAllowanceService) {
        this.cityAllowanceService = cityAllowanceService;
    }

    /**
     * 根据目的地城市和起止日期生成默认全选的每日补助明细。
     */
    public List<ReimbursementSaveRequest.AllowanceDayRequest> generateAllowanceDays(AllowanceGenerateRequest request) {
        if (request == null || request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("行程开始日期和结束日期不能为空");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("行程结束日期不能早于开始日期");
        }
        FkCityAllowance standard = findCityStandard(request.getEndCityCode(), request.getEndCityName(), true);
        java.util.ArrayList<ReimbursementSaveRequest.AllowanceDayRequest> days = new java.util.ArrayList<>();
        LocalDate cursor = request.getStartDate();
        while (!cursor.isAfter(request.getEndDate())) {
            days.add(toAllowanceDayRequest(cursor, standard));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    /**
     * 校验一条行程下每日补助的日期、唯一性和补助城市。
     */
    public void validateAllowanceDaysForItinerary(ReimbursementSaveRequest.ItineraryRequest itinerary,
                                                  List<ReimbursementSaveRequest.AllowanceDayRequest> days) {
        if (days == null) {
            return;
        }
        FkCityAllowance destinationStandard = findCityStandard(
                itinerary.getEndCityCode(),
                itinerary.getEndCityName(),
                true
        );
        Set<LocalDate> allowanceDates = new HashSet<>();

        for (ReimbursementSaveRequest.AllowanceDayRequest day : days) {
            if (day == null || day.getAllowanceDate() == null) {
                throw new IllegalArgumentException("每日补助日期不能为空");
            }
            if (day.getAllowanceDate().isBefore(itinerary.getStartDate())
                    || day.getAllowanceDate().isAfter(itinerary.getEndDate())) {
                throw new IllegalArgumentException("每日补助日期必须在行程日期范围内：" + day.getAllowanceDate());
            }
            if (!allowanceDates.add(day.getAllowanceDate())) {
                throw new IllegalArgumentException("同一行程的每日补助日期不能重复：" + day.getAllowanceDate());
            }

            boolean codeMismatch = StringUtils.hasText(day.getCityCode())
                    && !Objects.equals(destinationStandard.getCityCode(), day.getCityCode());
            boolean nameMismatch = StringUtils.hasText(day.getCityName())
                    && !Objects.equals(destinationStandard.getCityName(), day.getCityName());
            if (codeMismatch || nameMismatch) {
                throw new IllegalArgumentException("每日补助城市必须与行程目的地一致：" + day.getAllowanceDate());
            }

            day.setCityCode(destinationStandard.getCityCode());
            day.setCityName(destinationStandard.getCityName());
            day.setCityLevel(destinationStandard.getCityLevel());
        }
    }

    /**
     * 校验并填充一条每日补助实体。
     */
    public void fillAllowanceDay(FkReimAllowanceDay day,
                                 Long mainId,
                                 Long itineraryId,
                                 ReimbursementSaveRequest.AllowanceDayRequest request) {
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

    /**
     * 根据请求中的每日补助重新估算整张报销单总额。
     */
    public BigDecimal estimateTotalAmount(ReimbursementSaveRequest request) {
        if (request.getItineraries() == null) {
            return ZERO;
        }
        java.util.ArrayList<BigDecimal> values = new java.util.ArrayList<>();
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

    /**
     * 根据城市编码或名称查询补助标准。
     */
    public FkCityAllowance findCityStandard(String cityCode, String cityName, boolean required) {
        LambdaQueryWrapper<FkCityAllowance> wrapper = new LambdaQueryWrapper<FkCityAllowance>()
                .eq(StringUtils.hasText(cityCode), FkCityAllowance::getCityCode, cityCode)
                .or(!StringUtils.hasText(cityCode) && StringUtils.hasText(cityName), w -> w.eq(FkCityAllowance::getCityName, cityName));
        FkCityAllowance standard = cityAllowanceService.getOne(wrapper, false);
        if (standard == null && required) {
            throw new IllegalArgumentException("未找到城市补助标准：" + (StringUtils.hasText(cityName) ? cityName : cityCode));
        }
        return standard;
    }

    public BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
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
}
