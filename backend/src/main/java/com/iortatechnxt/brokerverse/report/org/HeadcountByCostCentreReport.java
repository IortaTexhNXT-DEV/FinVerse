package com.iortatechnxt.brokerverse.report.org;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Employee;
import com.iortatechnxt.brokerverse.organization.service.EmployeeService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * {@code ORG-HEADCOUNT-CC} Headcount per cost centre (DIS 3.30.2): the active employees grouped by
 * cost centre with their branch and position, and the headcount of each cost centre.
 */
@Component
public class HeadcountByCostCentreReport implements ReportDefinition {

  private final EmployeeService employees;
  private final OrganizationService organization;

  /**
   * Creates the report.
   *
   * @param employees employee master
   * @param organization branches
   */
  public HeadcountByCostCentreReport(EmployeeService employees, OrganizationService organization) {
    this.employees = employees;
    this.organization = organization;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.frbs(
        "ORG-HEADCOUNT-CC",
        "Headcount per Cost Centre",
        "Active employees grouped by cost centre, with the headcount of each (DIS 3.30.2)",
        List.of(GlReportSupport.companyParam()));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(GlReportSupport.COMPANY);
    Map<Long, String> branches = new HashMap<>();
    List<Map<String, Object>> rows =
        employees.active(companyId).stream()
            .map(e -> row(e, branches.computeIfAbsent(e.getBranchId(), this::branchCode)))
            .toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("employeeNo", "Employee No."),
            ReportColumn.text("name", "Name"),
            ReportColumn.text("branch", "Branch"),
            ReportColumn.text("position", "Position"),
            ReportColumn.count("headcount", "Headcount"))
        .groupBy("costCenter", "Cost Centre")
        .rows(rows)
        .presorted()
        .build();
  }

  private String branchCode(Long branchId) {
    Branch branch = organization.getBranch(branchId);
    return branch.getCode() + " - " + branch.getName();
  }

  private static Map<String, Object> row(Employee e, String branch) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("costCenter", e.getCostCenter());
    m.put("employeeNo", e.getEmployeeNo());
    m.put("name", e.getFullName());
    m.put("branch", branch);
    m.put("position", Objects.toString(e.getPosition(), ""));
    m.put("headcount", 1);
    return m;
  }
}
