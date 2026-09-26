package com.iortatechnxt.brokerverse.brokerclaims.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The loss of a claim and its claimant (BRCLM.004/006/036; loss notice content p.24-25;
 * CLAIMS_BROKING_DESIGN 5.1): loss and reported dates, nature and type of loss, description and
 * place, catastrophe code and event, claim amount, deductible, initial reserve and the claimant
 * with its override.
 *
 * <p>Owned by wave CL1-A. Invariants: the loss date is not in the future; the reported date is
 * between the loss date and today (the start of every claim age, BRCLM.025); an event name needs a
 * catastrophe code; amounts are not negative. A loss date outside the cover period is a warning
 * handled by the service (CLQ02).
 */
@Embeddable
public class LossDetails {

  @Column(name = "loss_date", nullable = false)
  private LocalDate lossDate;

  @Column(name = "reported_date", nullable = false)
  private LocalDate reportedDate;

  @Column(name = "loss_nature", length = 40)
  private String lossNature;

  @Column(name = "claim_type", length = 40)
  private String claimType;

  @Column(name = "loss_description", length = 2000)
  private String lossDescription;

  @Column(name = "loss_place", length = 500)
  private String lossPlace;

  @Column(name = "catastrophe_code", length = 40)
  private String catastropheCode;

  @Column(name = "catastrophe_event", length = 200)
  private String catastropheEvent;

  @Column(name = "claim_amount", precision = 19, scale = 2)
  private BigDecimal claimAmount;

  @Column(precision = 19, scale = 2)
  private BigDecimal deductible;

  @Column(name = "initial_reserve", precision = 19, scale = 2)
  private BigDecimal initialReserve;

  @Column(name = "claimant_name", length = 250)
  private String claimantName;

  @Column(name = "claimant_overridden", nullable = false)
  private boolean claimantOverridden;

  @Column(name = "claimant_reason", length = 500)
  private String claimantReason;

  /** Error code of a reported date outside the loss date and today. */
  public static final String REPORTED_DATE_RANGE = "BCL_REPORTED_DATE_RANGE";

  private static final String REASON_REQUIRED = "BCL_REASON_REQUIRED";

  protected LossDetails() {}

  /**
   * Loss details of a new claim; the claimant is the assured of the cover (BRCLM.006 R1).
   *
   * @param loss dates, nature, type, description, place and catastrophe
   * @param amounts claim amount, deductible and initial loss reserve
   * @param assuredName assured of the cover, the default claimant
   * @param today business date
   * @return loss details
   */
  public static LossDetails of(Loss loss, Amounts amounts, String assuredName, LocalDate today) {
    requireDates(loss.lossDate(), loss.reportedDate(), today);
    LossDetails details = new LossDetails();
    details.lossDate = loss.lossDate();
    details.reportedDate = loss.reportedDate();
    details.apply(loss, amounts);
    details.claimantName = assuredName;
    return details;
  }

  /**
   * Changes the loss data entered at recording (BCL_RECORD). The reported date is corrected only
   * through {@link #correctReportedDate} (BRCLM.004).
   *
   * @param loss new loss data; its reported date is ignored
   * @param amounts new amounts
   * @param today business date
   */
  public void amend(Loss loss, Amounts amounts, LocalDate today) {
    requireDates(loss.lossDate(), reportedDate, today);
    apply(loss, amounts);
    this.lossDate = loss.lossDate();
  }

  /**
   * Corrects the reported date (BRCLM.004, FR-CL-012; BCL_STATUS_UPDATE with a reason).
   *
   * @param date new reported date
   * @param reason reason ({@code BCL_OVERRIDE_REASON})
   * @param today business date
   * @return the previous date
   */
  public LocalDate correctReportedDate(LocalDate date, String reason, LocalDate today) {
    requireReason(reason);
    requireDates(lossDate, date, today);
    LocalDate previous = this.reportedDate;
    this.reportedDate = date;
    return previous;
  }

  /**
   * Overrides or inputs the claimant's name (BRCLM.006, BCL_CLAIMANT_OVERRIDE).
   *
   * @param name claimant name
   * @param reason reason of the override
   * @return the previous name
   */
  public String overrideClaimant(String name, String reason) {
    if (blank(name)) {
      throw new BusinessRuleException("BCL_CLAIMANT_REQUIRED", "Enter the claimant's name");
    }
    requireReason(reason);
    String previous = this.claimantName;
    this.claimantName = name.strip();
    this.claimantOverridden = true;
    this.claimantReason = reason.strip();
    return previous;
  }

  /**
   * Whether the claim carries a catastrophe code (flag CAT, BRCLM.036).
   *
   * @return true when tagged
   */
  public boolean isCatastrophe() {
    return catastropheCode != null;
  }

  private void apply(Loss loss, Amounts amounts) {
    if (blank(loss.catastropheCode()) && !blank(loss.catastropheEvent())) {
      throw new BusinessRuleException(
          "BCL_CATASTROPHE_REQUIRED", "Select the catastrophe code of the event");
    }
    requireNotNegative(amounts.claimAmount(), "claim amount");
    requireNotNegative(amounts.deductible(), "deductible");
    requireNotNegative(amounts.initialReserve(), "initial loss reserve");
    this.lossNature = loss.lossNature();
    this.claimType = loss.claimType();
    this.lossDescription = loss.lossDescription();
    this.lossPlace = loss.lossPlace();
    this.catastropheCode = blank(loss.catastropheCode()) ? null : loss.catastropheCode();
    this.catastropheEvent = blank(loss.catastropheEvent()) ? null : loss.catastropheEvent().strip();
    this.claimAmount = amounts.claimAmount();
    this.deductible = amounts.deductible();
    this.initialReserve = amounts.initialReserve();
  }

  private static void requireDates(LocalDate loss, LocalDate reported, LocalDate today) {
    if (loss == null || loss.isAfter(today)) {
      throw new BusinessRuleException(
          "BCL_LOSS_DATE_FUTURE", "Enter a loss date that is not in the future");
    }
    if (reported == null || reported.isBefore(loss) || reported.isAfter(today)) {
      throw new BusinessRuleException(
          REPORTED_DATE_RANGE, "The reported date must be between the loss date and today");
    }
  }

  private static void requireReason(String reason) {
    if (blank(reason)) {
      throw new BusinessRuleException(REASON_REQUIRED, "Enter the reason for the change");
    }
  }

  private static void requireNotNegative(BigDecimal amount, String what) {
    if (amount != null && amount.signum() < 0) {
      throw new BusinessRuleException("BCL_AMOUNT_NEGATIVE", "The " + what + " cannot be negative");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public LocalDate getLossDate() {
    return lossDate;
  }

  public LocalDate getReportedDate() {
    return reportedDate;
  }

  public String getLossNature() {
    return lossNature;
  }

  public String getClaimType() {
    return claimType;
  }

  public String getLossDescription() {
    return lossDescription;
  }

  public String getLossPlace() {
    return lossPlace;
  }

  public String getCatastropheCode() {
    return catastropheCode;
  }

  public String getCatastropheEvent() {
    return catastropheEvent;
  }

  public BigDecimal getClaimAmount() {
    return claimAmount;
  }

  public BigDecimal getDeductible() {
    return deductible;
  }

  public BigDecimal getInitialReserve() {
    return initialReserve;
  }

  public String getClaimantName() {
    return claimantName;
  }

  public boolean isClaimantOverridden() {
    return claimantOverridden;
  }

  public String getClaimantReason() {
    return claimantReason;
  }

  /**
   * Loss data of the PLA / CRF (p.24-25).
   *
   * @param lossDate date of accident / loss
   * @param reportedDate date the loss was reported (to BDOI by default, CLQ03)
   * @param lossNature nature of loss ({@code BCL_LOSS_NATURE})
   * @param claimType claim type ({@code BCL_CLAIM_TYPE})
   * @param lossDescription description
   * @param lossPlace place of loss (motor) or free text
   * @param catastropheCode catastrophe ({@code BCL_CATASTROPHE}), may be null
   * @param catastropheEvent event name, e.g. the typhoon's name
   */
  public record Loss(
      LocalDate lossDate,
      LocalDate reportedDate,
      String lossNature,
      String claimType,
      String lossDescription,
      String lossPlace,
      String catastropheCode,
      String catastropheEvent) {}

  /**
   * Amounts of the notice of loss (information only, no journal).
   *
   * @param claimAmount claim amount
   * @param deductible deductible
   * @param initialReserve initial loss reserve of the PLA
   */
  public record Amounts(BigDecimal claimAmount, BigDecimal deductible, BigDecimal initialReserve) {}
}
