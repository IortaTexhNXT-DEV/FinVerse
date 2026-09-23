package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Marine open cover: a master contract under which the client declares shipments, each insured by a
 * certificate (a {@link Policy} linked to the cover). Authorized under maker-checker control;
 * certificates can be issued only under an authorized cover and within its limits.
 */
@Entity
@Table(name = "uw_open_cover")
public class OpenCover extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "open_cover_no", nullable = false, length = 40)
  private String openCoverNo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_party_id")
  private Party customer;

  @Column(name = "insured_name", nullable = false, length = 200)
  private String insuredName;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "limit_per_shipment", nullable = false, precision = 19, scale = 2)
  private BigDecimal limitPerShipment;

  @Column(name = "annual_limit", nullable = false, precision = 19, scale = 2)
  private BigDecimal annualLimit;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(name = "cargo_description", length = 300)
  private String cargoDescription;

  protected OpenCover() {}

  /**
   * Creates an open cover pending authorization.
   *
   * @param openCoverNo allocated number
   * @param terms terms
   */
  public OpenCover(String openCoverNo, OpenCoverTerms terms) {
    if (!terms.product().isOpenCoverAllowed()) {
      throw new BusinessRuleException(
          "OPEN_COVER_NOT_ALLOWED",
          "Product " + terms.product().getCode() + " does not allow open covers");
    }
    UnderwritingRules.requirePeriod(terms.periodFrom(), terms.periodTo());
    UnderwritingRules.requireClient(terms.customer());
    this.companyId = terms.product().getCompanyId();
    this.branchId = terms.branchId();
    this.openCoverNo = openCoverNo;
    this.product = terms.product();
    this.customer = terms.customer();
    this.insuredName = terms.insuredName();
    this.periodFrom = terms.periodFrom();
    this.periodTo = terms.periodTo();
    this.currency = terms.currency();
    this.limitPerShipment = terms.limitPerShipment();
    this.annualLimit = terms.annualLimit();
    this.rate = terms.rate();
    this.cargoDescription = terms.cargoDescription();
  }

  /**
   * Checks that a shipment may be declared under this cover.
   *
   * @param sailDate shipment date
   * @param sumInsured shipment sum insured
   * @param declaredSoFar sum insured already declared on live certificates
   */
  public void requireDeclarable(
      LocalDate sailDate, BigDecimal sumInsured, BigDecimal declaredSoFar) {
    if (!isActive()) {
      throw new BusinessRuleException(
          "OPEN_COVER_NOT_ACTIVE", "Open cover " + openCoverNo + " is not authorized");
    }
    requireWithinPeriod(sailDate);
    if (sumInsured.compareTo(limitPerShipment) > 0) {
      throw new BusinessRuleException(
          "SHIPMENT_LIMIT_EXCEEDED",
          "Sum insured exceeds the limit per shipment " + limitPerShipment);
    }
    if (declaredSoFar.add(sumInsured).compareTo(annualLimit) > 0) {
      throw new BusinessRuleException(
          "ANNUAL_LIMIT_EXCEEDED", "Declarations would exceed the annual limit " + annualLimit);
    }
  }

  private void requireWithinPeriod(LocalDate sailDate) {
    if (sailDate.isBefore(periodFrom) || sailDate.isAfter(periodTo)) {
      throw new BusinessRuleException(
          "OUTSIDE_OPEN_COVER", "Shipment date is outside the open cover period");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getOpenCoverNo() {
    return openCoverNo;
  }

  public Product getProduct() {
    return product;
  }

  public Party getCustomer() {
    return customer;
  }

  public String getInsuredName() {
    return insuredName;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getLimitPerShipment() {
    return limitPerShipment;
  }

  public BigDecimal getAnnualLimit() {
    return annualLimit;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public String getCargoDescription() {
    return cargoDescription;
  }
}
