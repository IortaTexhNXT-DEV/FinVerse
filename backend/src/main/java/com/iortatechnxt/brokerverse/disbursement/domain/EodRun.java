package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EodStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * An end-of-day run of a business date (DIS 2.16.0-2.16.6, 3.28.0-3.28.2, 2.7.12): the approved
 * vouchers it froze and the counts of its outputs (checks, DCTF credits, forms, reports) and of the
 * payment confirmations e-mailed afterwards.
 */
@Entity
@Table(name = "dsb_eod_run")
public class EodRun extends BaseEntity {

  private static final int MAX_MESSAGE = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Column(name = "business_date", nullable = false, updatable = false)
  private LocalDate businessDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EodStatus status = EodStatus.COMPLETED;

  @Column(nullable = false)
  private int vouchers;

  @Column(nullable = false)
  private int checks;

  @Column(nullable = false)
  private int credits;

  @Column(nullable = false)
  private int forms;

  @Column(nullable = false)
  private int reports;

  @Column(nullable = false)
  private int emails;

  @Column(length = MAX_MESSAGE)
  private String message;

  protected EodRun() {}

  /**
   * A run.
   *
   * @param companyId company
   * @param runNo run number
   * @param businessDate business date
   */
  public EodRun(Long companyId, String runNo, LocalDate businessDate) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.businessDate = businessDate;
  }

  /**
   * Records the counts of the outputs.
   *
   * @param counts counts
   */
  public void produced(EodCounts counts) {
    vouchers = counts.vouchers();
    checks = counts.checks();
    credits = counts.credits();
    forms = counts.forms();
    reports = counts.reports();
    String text = counts.message();
    message = text == null || text.length() <= MAX_MESSAGE ? text : text.substring(0, MAX_MESSAGE);
  }

  /**
   * The payment confirmations were e-mailed (DIS 2.7.12).
   *
   * @param sent e-mails queued
   */
  public void confirmed(int sent) {
    emails = sent;
    status = EodStatus.CONFIRMED;
  }

  /**
   * Counts the reports produced by the EOD reports job (DIS 3.28.0).
   *
   * @param produced reports produced
   */
  public void reported(int produced) {
    reports = produced;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public LocalDate getBusinessDate() {
    return businessDate;
  }

  public EodStatus getStatus() {
    return status;
  }

  public int getVouchers() {
    return vouchers;
  }

  public int getChecks() {
    return checks;
  }

  public int getCredits() {
    return credits;
  }

  public int getForms() {
    return forms;
  }

  public int getReports() {
    return reports;
  }

  public int getEmails() {
    return emails;
  }

  public String getMessage() {
    return message;
  }

  /**
   * Counts of an end-of-day run.
   *
   * @param vouchers vouchers frozen
   * @param checks checks printed
   * @param credits DCTF credits
   * @param forms ATD / MC-DD / CT / TT forms
   * @param reports reports produced
   * @param message summary
   */
  public record EodCounts(
      int vouchers, int checks, int credits, int forms, int reports, String message) {}
}
