package com.enpenseSystem.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AllowanceGenerateRequest {

    private String startCityCode;
    private String startCityName;
    private String endCityCode;
    private String endCityName;
    private LocalDate startDate;
    private LocalDate endDate;
}
