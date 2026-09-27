package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * One version of a packaged product (BRPM.006/007/017, PMADD01/02/06; PRODUCT_MAINTENANCE_DESIGN
 * sections 4.2 and 5.1): the rate scheme, package term, coverages, insurers and insurer x coverage
 * terms, and the validation checkpoint. A package is never edited in place: commercial terms change
 * only through a new DRAFT version, which is submitted, validated by someone other than its maker
 * and released with an effective date, while the released version keeps selling.
 */
@Entity
@Table(name = "cat_product_version")
public class ProductVersion extends BaseEntity {

  private static final String VERSION = "Version ";

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProductVersionStatus status = ProductVersionStatus.DRAFT;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(name = "package_start_date")
  private LocalDate packageStartDate;

  @Column(name = "package_end_date")
  private LocalDate packageEndDate;

  @Column(name = "anniversary_date")
  private LocalDate anniversaryDate;

  @Embedded private SchemeTerms scheme;

  @Column(name = "source_request_no", length = 40)
  private String sourceRequestNo;

  @Column(name = "mancom_signoff_ref", length = 60)
  private String mancomSignoffRef;

  @Column(name = "change_summary", length = 1000)
  private String changeSummary;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "validated_by", length = 50)
  private String validatedBy;

  @Column(name = "validated_at")
  private Instant validatedAt;

  @Column(name = "validation_checklist", length = 4000)
  private String validationChecklist;

  @Column(name = "test_premium", precision = 19, scale = 2)
  private BigDecimal testPremium;

  @Column(name = "returned_reason", length = 500)
  private String returnedReason;

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "cat_package_coverage", joinColumns = @JoinColumn(name = "version_id"))
  @OrderBy("sortOrder")
  private List<VersionCoverage> coverages = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(name = "cat_package_insurer", joinColumns = @JoinColumn(name = "version_id"))
  @OrderBy("insurerCode")
  private List<VersionInsurer> insurers = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(
      name = "cat_package_insurer_term",
      joinColumns = @JoinColumn(name = "version_id"))
  @OrderBy("insurerCode, coverageCode")
  private List<VersionInsurerTerm> insurerTerms = new ArrayList<>();

  protected ProductVersion() {}

  /**
   * Creates a DRAFT version.
   *
   * @param productCode risk code
   * @param versionNo version number (next of the product)
   * @param content rate scheme, dates, coverages, insurers and terms
   * @param origin source request, ManCom reference and change summary
   */
  public ProductVersion(String productCode, int versionNo, Content content, Origin origin) {
    this.productCode = productCode;
    this.versionNo = versionNo;
    apply(content);
    this.sourceRequestNo = origin.sourceRequestNo();
    this.mancomSignoffRef = origin.mancomSignoffRef();
    this.changeSummary = origin.changeSummary();
  }

  /**
   * Replaces the content of a DRAFT version.
   *
   * @param content new content
   * @param origin origin (the change summary and references may be refined)
   */
  public void replace(Content content, Origin origin) {
    requireStatus(ProductVersionStatus.DRAFT, "VERSION_NOT_DRAFT", "is not a draft");
    apply(content);
    this.mancomSignoffRef = origin.mancomSignoffRef();
    this.changeSummary = origin.changeSummary();
    if (origin.sourceRequestNo() != null) {
      this.sourceRequestNo = origin.sourceRequestNo();
    }
  }

  private void apply(Content c) {
    if (c.effectiveFrom() == null) {
      throw new BusinessRuleException("PACKAGE_DATES_INVALID", "Enter the effective date");
    }
    this.effectiveFrom = c.effectiveFrom();
    this.packageStartDate = c.packageStartDate();
    this.packageEndDate = c.packageEndDate();
    this.anniversaryDate = c.anniversaryDate();
    this.scheme = c.scheme();
    this.coverages = new ArrayList<>(c.coverages());
    this.insurers = new ArrayList<>(c.insurers());
    this.insurerTerms = new ArrayList<>(c.insurerTerms());
  }

  /**
   * Submits the draft for the validation checkpoint (PMADD06); the completeness checks are done by
   * the service first.
   *
   * @param user maker who submits
   * @param when time
   */
  public void submit(String user, Instant when) {
    requireStatus(ProductVersionStatus.DRAFT, "VERSION_NOT_DRAFT", "is not a draft");
    this.status = ProductVersionStatus.FOR_VALIDATION;
    this.submittedBy = user;
    this.submittedAt = when;
    this.returnedReason = null;
  }

  /**
   * Releases the version after validation (PMADD06).
   *
   * @param validator validator (never the maker or submitter; checked by the service)
   * @param when time
   * @param checklist confirmed checklist items (JSON)
   * @param premium test premium computed on a sample item, null when not computable
   */
  public void release(String validator, Instant when, String checklist, BigDecimal premium) {
    requireStatus(
        ProductVersionStatus.FOR_VALIDATION, "VERSION_NOT_FOR_VALIDATION", "is not submitted");
    this.status = ProductVersionStatus.RELEASED;
    this.validatedBy = validator;
    this.validatedAt = when;
    this.validationChecklist = checklist;
    this.testPremium = premium;
  }

  /**
   * Sends the submitted version back to DRAFT (PMADD06).
   *
   * @param reason reason
   */
  public void returnToDraft(String reason) {
    requireStatus(
        ProductVersionStatus.FOR_VALIDATION, "VERSION_NOT_FOR_VALIDATION", "is not submitted");
    this.status = ProductVersionStatus.DRAFT;
    this.returnedReason = reason;
  }

  /**
   * Ends the selling period of a released version the day before its successor starts.
   *
   * @param successorFrom effective date of the next released version
   */
  public void endBefore(LocalDate successorFrom) {
    this.effectiveTo = successorFrom.minusDays(1);
  }

  /**
   * Marks a released version SUPERSEDED once its selling period has ended.
   *
   * @param today business date
   * @return true when the status changed
   */
  public boolean supersedeIfEnded(LocalDate today) {
    if (status == ProductVersionStatus.RELEASED
        && effectiveTo != null
        && effectiveTo.isBefore(today)) {
      this.status = ProductVersionStatus.SUPERSEDED;
      return true;
    }
    return false;
  }

  /** Marks the version EXPIRED: its package end date passed without a successor (BRPM.006). */
  public void expire() {
    this.status = ProductVersionStatus.EXPIRED;
  }

  /**
   * Whether the version priced business on a date: RELEASED or SUPERSEDED and within its selling
   * period.
   *
   * @param date date
   * @return true when in force
   */
  public boolean isInForce(LocalDate date) {
    boolean sold =
        status == ProductVersionStatus.RELEASED || status == ProductVersionStatus.SUPERSEDED;
    return sold
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  /**
   * Whether the version is being set up (DRAFT or FOR_VALIDATION).
   *
   * @return true when open
   */
  public boolean isOpen() {
    return status == ProductVersionStatus.DRAFT || status == ProductVersionStatus.FOR_VALIDATION;
  }

  /**
   * The insurer entry of a panel insurer.
   *
   * @param insurerCode insurer party code
   * @return entry, null when the insurer is not on the package
   */
  public VersionInsurer insurer(String insurerCode) {
    return insurers.stream()
        .filter(i -> i.insurerCode().equals(insurerCode))
        .findFirst()
        .orElse(null);
  }

  private void requireStatus(ProductVersionStatus expected, String code, String message) {
    if (status != expected) {
      throw new BusinessRuleException(
          code, VERSION + versionNo + " of " + productCode + " " + message + " (" + status + ")");
    }
  }

  /**
   * Reference used in the audit trail.
   *
   * @return "code vN"
   */
  public String reference() {
    return productCode + " v" + versionNo;
  }

  public String getProductCode() {
    return productCode;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public ProductVersionStatus getStatus() {
    return status;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public LocalDate getPackageStartDate() {
    return packageStartDate;
  }

  public LocalDate getPackageEndDate() {
    return packageEndDate;
  }

  public LocalDate getAnniversaryDate() {
    return anniversaryDate;
  }

  public SchemeTerms getScheme() {
    return scheme;
  }

  public String getSourceRequestNo() {
    return sourceRequestNo;
  }

  public String getMancomSignoffRef() {
    return mancomSignoffRef;
  }

  public String getChangeSummary() {
    return changeSummary;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getValidatedBy() {
    return validatedBy;
  }

  public Instant getValidatedAt() {
    return validatedAt;
  }

  public String getValidationChecklist() {
    return validationChecklist;
  }

  public BigDecimal getTestPremium() {
    return testPremium;
  }

  public String getReturnedReason() {
    return returnedReason;
  }

  public List<VersionCoverage> getCoverages() {
    return List.copyOf(coverages);
  }

  public List<VersionInsurer> getInsurers() {
    return List.copyOf(insurers);
  }

  public List<VersionInsurerTerm> getInsurerTerms() {
    return List.copyOf(insurerTerms);
  }

  /**
   * The editable content of a version.
   *
   * @param effectiveFrom date the version sells from once released
   * @param packageStartDate start of the insurer agreement
   * @param packageEndDate end of the insurer agreement
   * @param anniversaryDate anniversary date, null when none
   * @param scheme rate scheme
   * @param coverages coverages
   * @param insurers insurers
   * @param insurerTerms insurer x coverage terms
   */
  public record Content(
      LocalDate effectiveFrom,
      LocalDate packageStartDate,
      LocalDate packageEndDate,
      LocalDate anniversaryDate,
      SchemeTerms scheme,
      List<VersionCoverage> coverages,
      List<VersionInsurer> insurers,
      List<VersionInsurerTerm> insurerTerms) {

    /** Defensive copies. */
    public Content {
      coverages = coverages == null ? List.of() : List.copyOf(coverages);
      insurers = insurers == null ? List.of() : List.copyOf(insurers);
      insurerTerms = insurerTerms == null ? List.of() : List.copyOf(insurerTerms);
    }
  }

  /**
   * Where the version comes from.
   *
   * @param sourceRequestNo package request number, null for a catalog-only version
   * @param mancomSignoffRef ManCom sign-off reference, null when none
   * @param changeSummary what changes against the previous version
   */
  public record Origin(String sourceRequestNo, String mancomSignoffRef, String changeSummary) {}
}
