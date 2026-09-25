package com.iortatechnxt.brokerverse.acsl.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The layout of an insurer's statement of account (ACSL 2.4.0): the column header of each field in
 * the insurer's file. The insurer formats are not known yet (AQ21), so the standard ACSL template
 * ({@code *}) is used until an insurer layout is configured.
 */
@Entity
@Table(name = "acsl_soa_layout")
public class SoaLayout extends BaseEntity {

  /** Insurer code of the standard template. */
  public static final String STANDARD = "*";

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "invoice_header", nullable = false, length = 60)
  private String invoiceHeader;

  @Column(name = "policy_header", nullable = false, length = 60)
  private String policyHeader;

  @Column(name = "assured_header", nullable = false, length = 60)
  private String assuredHeader;

  @Column(name = "inception_header", nullable = false, length = 60)
  private String inceptionHeader;

  @Column(name = "expiry_header", nullable = false, length = 60)
  private String expiryHeader;

  @Column(name = "gross_header", nullable = false, length = 60)
  private String grossHeader;

  @Column(name = "balance_header", nullable = false, length = 60)
  private String balanceHeader;

  @Column(name = "paid_header", nullable = false, length = 60)
  private String paidHeader;

  @Column(nullable = false)
  private boolean active;

  protected SoaLayout() {}

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getName() {
    return name;
  }

  public String getInvoiceHeader() {
    return invoiceHeader;
  }

  public String getPolicyHeader() {
    return policyHeader;
  }

  public String getAssuredHeader() {
    return assuredHeader;
  }

  public String getInceptionHeader() {
    return inceptionHeader;
  }

  public String getExpiryHeader() {
    return expiryHeader;
  }

  public String getGrossHeader() {
    return grossHeader;
  }

  public String getBalanceHeader() {
    return balanceHeader;
  }

  public String getPaidHeader() {
    return paidHeader;
  }

  public boolean isActive() {
    return active;
  }
}
