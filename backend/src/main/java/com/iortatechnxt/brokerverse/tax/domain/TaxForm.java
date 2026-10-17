package com.iortatechnxt.brokerverse.tax.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A return in the tax filing calendar (maker-checker): authority, filing frequency, due-date rule,
 * the worksheet that computes it and the GL accounts its remittance clears.
 *
 * <p>{@code trackFiling = false} marks reminder-only forms prepared outside BrokerVerse (e.g.
 * 1601-C from payroll): they are listed on the calendar but raise no alerts and have no returns.
 * The calendar starts at {@code effectiveFrom}, so go-live does not produce alerts for older
 * periods.
 */
@Entity
@Table(name = "tax_form")
public class TaxForm extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TaxAuthority authority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private FilingFrequency frequency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorksheetKind worksheet;

  @Column(name = "due_months_after", nullable = false)
  private int dueMonthsAfter;

  @Column(name = "due_day", nullable = false)
  private int dueDay;

  @Column(name = "payable_account_code", length = 30)
  private String payableAccountCode;

  @Column(name = "credit_account_code", length = 30)
  private String creditAccountCode;

  @Column(name = "track_filing", nullable = false)
  private boolean trackFiling;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  protected TaxForm() {}

  /**
   * Creates a form (pending authorization).
   *
   * @param companyId company
   * @param code form code, e.g. "2550Q"
   */
  public TaxForm(Long companyId, String code) {
    this.companyId = companyId;
    this.code = code;
  }

  /**
   * Filing schedule of the form.
   *
   * @return schedule
   */
  public FilingSchedule schedule() {
    return new FilingSchedule(frequency, dueMonthsAfter, dueDay);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TaxAuthority getAuthority() {
    return authority;
  }

  public void setAuthority(TaxAuthority authority) {
    this.authority = authority;
  }

  public FilingFrequency getFrequency() {
    return frequency;
  }

  public void setFrequency(FilingFrequency frequency) {
    this.frequency = frequency;
  }

  public WorksheetKind getWorksheet() {
    return worksheet;
  }

  public void setWorksheet(WorksheetKind worksheet) {
    this.worksheet = worksheet;
  }

  public int getDueMonthsAfter() {
    return dueMonthsAfter;
  }

  public void setDueMonthsAfter(int dueMonthsAfter) {
    this.dueMonthsAfter = dueMonthsAfter;
  }

  public int getDueDay() {
    return dueDay;
  }

  public void setDueDay(int dueDay) {
    this.dueDay = dueDay;
  }

  public String getPayableAccountCode() {
    return payableAccountCode;
  }

  public void setPayableAccountCode(String payableAccountCode) {
    this.payableAccountCode = payableAccountCode;
  }

  public String getCreditAccountCode() {
    return creditAccountCode;
  }

  public void setCreditAccountCode(String creditAccountCode) {
    this.creditAccountCode = creditAccountCode;
  }

  public boolean isTrackFiling() {
    return trackFiling;
  }

  public void setTrackFiling(boolean trackFiling) {
    this.trackFiling = trackFiling;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }
}
