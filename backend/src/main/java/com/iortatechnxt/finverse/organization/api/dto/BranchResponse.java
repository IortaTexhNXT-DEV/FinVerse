package com.iortatechnxt.finverse.organization.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.organization.domain.Branch;
import java.time.LocalDate;

/**
 * Branch view.
 *
 * @param id id
 * @param companyId company id
 * @param code code
 * @param name name
 * @param region region
 * @param address address
 * @param openingDate opening date
 * @param headOffice head office flag
 * @param forexAuthorized forex flag
 * @param contactPhone phone
 * @param contactEmail email
 * @param managerName manager
 * @param weeklyHolidays weekly holidays
 * @param recordStatus maker-checker status
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 */
public record BranchResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    String region,
    String address,
    LocalDate openingDate,
    boolean headOffice,
    boolean forexAuthorized,
    String contactPhone,
    String contactEmail,
    String managerName,
    String weeklyHolidays,
    RecordStatus recordStatus,
    String createdBy,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param b branch
   * @return response
   */
  public static BranchResponse from(Branch b) {
    return new BranchResponse(
        b.getId(),
        b.getCompany().getId(),
        b.getCode(),
        b.getName(),
        b.getRegion(),
        b.getAddress(),
        b.getOpeningDate(),
        b.isHeadOffice(),
        b.isForexAuthorized(),
        b.getContactPhone(),
        b.getContactEmail(),
        b.getManagerName(),
        b.getWeeklyHolidays(),
        b.getRecordStatus(),
        b.getCreatedBy(),
        b.getMaker(),
        b.getAuthorizedBy());
  }
}
