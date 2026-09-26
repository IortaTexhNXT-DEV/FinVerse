package com.iortatechnxt.brokerverse.tax.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapping of one Insurance Commission schedule line to the ledger (maker-checker).
 *
 * <p>An account belongs to the line when its code lies in {@code [accountFrom, accountTo]} (text
 * comparison, so ranges should use codes of equal length) or when its report group equals {@code
 * reportGroup}. The line amount is the ledger net presented on the {@link NormalBalance} side; the
 * schedule total adds each line multiplied by {@code signFactor} (+1 adds, −1 deducts, e.g. "less
 * reinsurance premiums ceded"). For the RBC summary {@code rbcFactor} (percent) turns the line
 * amount into a capital requirement.
 */
@Entity
@Table(name = "tax_ic_line_item")
public class IcLineItem extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IcSchedule schedule;

  @Column(name = "line_code", nullable = false, length = 30)
  private String lineCode;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(name = "line_order", nullable = false)
  private int lineOrder;

  @Column(name = "account_from", length = 30)
  private String accountFrom;

  @Column(name = "account_to", length = 30)
  private String accountTo;

  @Column(name = "report_group", length = 100)
  private String reportGroup;

  @Enumerated(EnumType.STRING)
  @Column(name = "normal_balance", nullable = false, length = 6)
  private NormalBalance normalBalance;

  @Column(name = "sign_factor", nullable = false)
  private int signFactor = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private IcMeasure measure;

  @Column(name = "rbc_factor", precision = 19, scale = 8)
  private BigDecimal rbcFactor;

  protected IcLineItem() {}

  /**
   * Creates a line item (pending authorization).
   *
   * @param companyId company
   * @param schedule schedule
   * @param lineCode line code, unique per schedule
   */
  public IcLineItem(Long companyId, IcSchedule schedule, String lineCode) {
    this.companyId = companyId;
    this.schedule = schedule;
    this.lineCode = lineCode;
  }

  /**
   * Whether an account is mapped to this line.
   *
   * @param accountCode account code
   * @param accountReportGroup report group of the account (may be null)
   * @return true when inside the range or in the report group
   */
  public boolean matches(String accountCode, String accountReportGroup) {
    boolean inRange =
        accountFrom != null
            && accountTo != null
            && accountCode.compareTo(accountFrom) >= 0
            && accountCode.compareTo(accountTo) <= 0;
    boolean inGroup = reportGroup != null && reportGroup.equals(accountReportGroup);
    return inRange || inGroup;
  }

  /**
   * Readable mapping, e.g. "4100-4199" or "group Financial Assets".
   *
   * @return text
   */
  public String mappingText() {
    List<String> parts = new ArrayList<>();
    if (accountFrom != null) {
      parts.add(accountFrom.equals(accountTo) ? accountFrom : accountFrom + "-" + accountTo);
    }
    if (reportGroup != null) {
      parts.add("group " + reportGroup);
    }
    return String.join(", ", parts);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public IcSchedule getSchedule() {
    return schedule;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getLineOrder() {
    return lineOrder;
  }

  public void setLineOrder(int lineOrder) {
    this.lineOrder = lineOrder;
  }

  public String getAccountFrom() {
    return accountFrom;
  }

  public void setAccountFrom(String accountFrom) {
    this.accountFrom = accountFrom;
  }

  public String getAccountTo() {
    return accountTo;
  }

  public void setAccountTo(String accountTo) {
    this.accountTo = accountTo;
  }

  public String getReportGroup() {
    return reportGroup;
  }

  public void setReportGroup(String reportGroup) {
    this.reportGroup = reportGroup;
  }

  public NormalBalance getNormalBalance() {
    return normalBalance;
  }

  public void setNormalBalance(NormalBalance normalBalance) {
    this.normalBalance = normalBalance;
  }

  public int getSignFactor() {
    return signFactor;
  }

  public void setSignFactor(int signFactor) {
    this.signFactor = signFactor;
  }

  public IcMeasure getMeasure() {
    return measure;
  }

  public void setMeasure(IcMeasure measure) {
    this.measure = measure;
  }

  public BigDecimal getRbcFactor() {
    return rbcFactor;
  }

  public void setRbcFactor(BigDecimal rbcFactor) {
    this.rbcFactor = rbcFactor;
  }
}
