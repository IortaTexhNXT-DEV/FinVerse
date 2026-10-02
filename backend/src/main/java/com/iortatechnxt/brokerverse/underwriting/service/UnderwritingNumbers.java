package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Allocates underwriting document numbers (gapless per series, see {@link DocumentNumberService}):
 *
 * <ul>
 *   <li>policy {@code P-<product>-<branch>-<year>-000001}
 *   <li>marine certificate {@code MC-<branch>-<year>-000001}
 *   <li>quotation {@code Q-<branch>-<year>-000001}, open cover {@code OC-<branch>-<year>-000001}
 *   <li>debit note {@code DN-<branch>-<year>-000001}, credit note {@code CN-<branch>-<year>-000001}
 * </ul>
 */
@Component
public class UnderwritingNumbers {

  private final DocumentNumberService numbers;
  private final OrganizationService organization;

  /**
   * Creates the allocator.
   *
   * @param numbers document number service
   * @param organization organization (branch codes)
   */
  public UnderwritingNumbers(DocumentNumberService numbers, OrganizationService organization) {
    this.numbers = numbers;
    this.organization = organization;
  }

  /**
   * Next policy number.
   *
   * @param productCode product code
   * @param branchId branch
   * @param date issue date
   * @return number
   */
  public String policy(String productCode, Long branchId, LocalDate date) {
    return next("P-" + productCode, branchId, date);
  }

  /**
   * Next marine certificate number.
   *
   * @param branchId branch
   * @param date issue date
   * @return number
   */
  public String certificate(Long branchId, LocalDate date) {
    return next("MC", branchId, date);
  }

  /**
   * Next quotation number.
   *
   * @param branchId branch
   * @param date issue date
   * @return number
   */
  public String quotation(Long branchId, LocalDate date) {
    return next("Q", branchId, date);
  }

  /**
   * Next open cover number.
   *
   * @param branchId branch
   * @param date start date
   * @return number
   */
  public String openCover(Long branchId, LocalDate date) {
    return next("OC", branchId, date);
  }

  /**
   * Next debit note (amount due to the company) or credit note (amount due by the company).
   *
   * @param debit true for a debit note
   * @param branchId branch
   * @param date document date
   * @return number
   */
  public String note(boolean debit, Long branchId, LocalDate date) {
    return next(debit ? "DN" : "CN", branchId, date);
  }

  private String next(String series, Long branchId, LocalDate date) {
    String branch = organization.getBranch(branchId).getCode();
    return numbers.next(series + "-" + branch + "-" + date.getYear());
  }
}
