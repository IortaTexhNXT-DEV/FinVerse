package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Allocates reinsurance document numbers (gapless per series, see {@link DocumentNumberService}):
 *
 * <ul>
 *   <li>cession {@code RC-<branch>-<year>-000001}
 *   <li>facultative placement {@code FAC-<branch>-<year>-000001}
 *   <li>statement of account {@code SOA-<company>-<year>-000001}
 * </ul>
 */
@Component
public class ReinsuranceNumbers {

  private final DocumentNumberService numbers;
  private final OrganizationService organization;

  /**
   * Creates the allocator.
   *
   * @param numbers document number service
   * @param organization branch and company codes
   */
  public ReinsuranceNumbers(DocumentNumberService numbers, OrganizationService organization) {
    this.numbers = numbers;
    this.organization = organization;
  }

  /**
   * Next cession number.
   *
   * @param branchId branch of the policy
   * @param date RI accounting date
   * @return number
   */
  public String cession(Long branchId, LocalDate date) {
    return branchSeries("RC", branchId, date);
  }

  /**
   * Next facultative placement number.
   *
   * @param branchId branch of the policy
   * @param date date the requirement was identified
   * @return number
   */
  public String placement(Long branchId, LocalDate date) {
    return branchSeries("FAC", branchId, date);
  }

  /**
   * Next statement of account number.
   *
   * @param companyId company
   * @param year statement year
   * @return number
   */
  public String statement(Long companyId, int year) {
    return numbers.next("SOA-" + organization.getCompany(companyId).getCode() + "-" + year);
  }

  private String branchSeries(String series, Long branchId, LocalDate date) {
    String branch = organization.getBranch(branchId).getCode();
    return numbers.next(series + "-" + branch + "-" + date.getYear());
  }
}
