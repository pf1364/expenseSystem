package com.enpenseSystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enpenseSystem.common.ApiResponse;
import com.enpenseSystem.entity.ReimBusinessType;
import com.enpenseSystem.entity.ReimCity;
import com.enpenseSystem.entity.ReimCompany;
import com.enpenseSystem.entity.ReimDepartment;
import com.enpenseSystem.entity.ReimEmployee;
import com.enpenseSystem.entity.ReimProject;
import com.enpenseSystem.service.ReimBusinessTypeService;
import com.enpenseSystem.service.ReimCityService;
import com.enpenseSystem.service.ReimCompanyService;
import com.enpenseSystem.service.ReimDepartmentService;
import com.enpenseSystem.service.ReimEmployeeService;
import com.enpenseSystem.service.ReimProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reimbursement")
public class ReimbursementDataController {

    private final ReimCompanyService reimCompanyService;
    private final ReimDepartmentService reimDepartmentService;
    private final ReimEmployeeService reimEmployeeService;
    private final ReimBusinessTypeService reimBusinessTypeService;
    private final ReimCityService reimCityService;
    private final ReimProjectService reimProjectService;

    public ReimbursementDataController(ReimCompanyService reimCompanyService,
                                       ReimDepartmentService reimDepartmentService,
                                       ReimEmployeeService reimEmployeeService,
                                       ReimBusinessTypeService reimBusinessTypeService,
                                       ReimCityService reimCityService,
                                       ReimProjectService reimProjectService) {
        this.reimCompanyService = reimCompanyService;
        this.reimDepartmentService = reimDepartmentService;
        this.reimEmployeeService = reimEmployeeService;
        this.reimBusinessTypeService = reimBusinessTypeService;
        this.reimCityService = reimCityService;
        this.reimProjectService = reimProjectService;
    }

    @GetMapping("/companies")
    public ApiResponse<List<ReimCompany>> listCompanies() {
        return ApiResponse.success(reimCompanyService.list(new LambdaQueryWrapper<ReimCompany>()
                .orderByAsc(ReimCompany::getReimCompanyNo)));
    }

    @GetMapping("/departments")
    public ApiResponse<List<ReimDepartment>> listDepartments() {
        return ApiResponse.success(reimDepartmentService.list(new LambdaQueryWrapper<ReimDepartment>()
                .orderByAsc(ReimDepartment::getReimDepartmentNo)));
    }

    @GetMapping("/employees")
    public ApiResponse<List<ReimEmployee>> listEmployees() {
        return ApiResponse.success(reimEmployeeService.list(new LambdaQueryWrapper<ReimEmployee>()
                .orderByAsc(ReimEmployee::getReimburserNo)));
    }

    @GetMapping("/business-types")
    public ApiResponse<List<ReimBusinessType>> listBusinessTypes() {
        return ApiResponse.success(reimBusinessTypeService.listTree());
    }

    @GetMapping("/cities")
    public ApiResponse<List<ReimCity>> listCities() {
        return ApiResponse.success(reimCityService.list(new LambdaQueryWrapper<ReimCity>()
                .orderByAsc(ReimCity::getCityNo)));
    }

    @GetMapping("/projects")
    public ApiResponse<List<ReimProject>> listProjects() {
        return ApiResponse.success(reimProjectService.list(new LambdaQueryWrapper<ReimProject>()
                .orderByAsc(ReimProject::getProjectNo)));
    }
}
