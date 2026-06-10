package com.enpenseSystem.service;

import com.enpenseSystem.common.PageData;
import com.enpenseSystem.dto.AllowanceGenerateRequest;
import com.enpenseSystem.dto.PersonalStatisticsVO;
import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.enpenseSystem.dto.ReimbursementPageQuery;
import com.enpenseSystem.dto.ReimbursementPageVO;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.dto.ReimbursementSaveResponse;
import com.enpenseSystem.entity.FkCityAllowance;

import java.util.List;

public interface ReimbursementService {

    PageData<ReimbursementPageVO> page(ReimbursementPageQuery query);

    ReimbursementDetailVO detail(String reimNo);

    ReimbursementSaveResponse createDraft(ReimbursementSaveRequest request);

    ReimbursementSaveResponse createAndSubmit(ReimbursementSaveRequest request);

    ReimbursementSaveResponse update(String reimNo, ReimbursementSaveRequest request);

    ReimbursementSaveResponse submitDraft(String reimNo);

    ReimbursementSaveResponse copy(String reimNo);

    void deleteDraft(String reimNo);

    void voidBill(String reimNo);

    List<ReimbursementSaveRequest.AllowanceDayRequest> generateAllowanceDays(AllowanceGenerateRequest request);

    List<FkCityAllowance> listCityAllowances();

    PersonalStatisticsVO personalStatistics(String reimburserName, String reimburserNo);
}
