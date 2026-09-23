package com.iortatechnxt.finverse.organization.api.dto;

import com.iortatechnxt.finverse.organization.domain.Holiday;
import java.time.LocalDate;

/**
 * Holiday view.
 *
 * @param id id
 * @param companyId company
 * @param branchId branch or null
 * @param holidayDate date
 * @param description description
 */
public record HolidayResponse(
    Long id, Long companyId, Long branchId, LocalDate holidayDate, String description) {

  /**
   * Maps an entity.
   *
   * @param h holiday
   * @return response
   */
  public static HolidayResponse from(Holiday h) {
    return new HolidayResponse(
        h.getId(), h.getCompanyId(), h.getBranchId(), h.getHolidayDate(), h.getDescription());
  }
}
