package com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.booking.domain.BookingEndorsement;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService.PolicyYear;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule.InvoiceState;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule.PremiumCheck;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPremiumStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Response bodies of the Cover Lookup and the cover card of Record Claim (BRCLM.002/003/039). */
public final class CoverDtos {

  private CoverDtos() {}

  /**
   * A cover found by a search.
   *
   * @param arn account reference number
   * @param accountId account id
   * @param clientCode client
   * @param assuredName assured
   * @param productCode product
   * @param lineCode product line
   * @param insurerCode lead insurer
   * @param periodFrom start of the term
   * @param periodTo end of the term
   * @param status account status
   * @param currency currency
   * @param policyNumbers policy numbers by policy year
   */
  public record CoverHit(
      String arn,
      Long accountId,
      String clientCode,
      String assuredName,
      String productCode,
      String lineCode,
      String insurerCode,
      LocalDate periodFrom,
      LocalDate periodTo,
      String status,
      String currency,
      List<String> policyNumbers) {

    /**
     * Maps an account.
     *
     * @param a account
     * @return hit
     */
    public static CoverHit from(Account a) {
      return new CoverHit(
          a.getArn(),
          a.getId(),
          a.getClientCode(),
          a.getClientName(),
          a.getProductCode(),
          a.getLineCode(),
          a.getInsurerCode(),
          a.getPeriodFrom(),
          a.getPeriodTo(),
          a.getStatus().name(),
          a.getCurrency(),
          a.getPolicyNumbers());
    }
  }

  /**
   * A policy year of a cover.
   *
   * @param year policy year (1 = first)
   * @param policyNo policy number, null while not issued
   * @param from start
   * @param to end
   */
  public record PolicyYearDto(int year, String policyNo, LocalDate from, LocalDate to) {

    /**
     * Maps a policy year.
     *
     * @param y year
     * @return dto
     */
    public static PolicyYearDto from(PolicyYear y) {
      return new PolicyYearDto(y.year(), y.policyNo(), y.from(), y.to());
    }
  }

  /**
   * A risk item of a cover.
   *
   * @param itemNo item number
   * @param kind kind (VEHICLE, PROPERTY_LOCATION, PERSON, GENERIC)
   * @param label plate, address, person or description
   * @param city city of a location
   * @param province province of a location
   * @param locationKey normalised location key
   * @param sumInsured sum insured
   */
  public record ItemDto(
      int itemNo,
      String kind,
      String label,
      String city,
      String province,
      String locationKey,
      BigDecimal sumInsured) {

    /**
     * Maps an item.
     *
     * @param i item
     * @return dto
     */
    public static ItemDto from(RiskItem i) {
      boolean location = i.getLocationKey() != null;
      return new ItemDto(
          i.getItemNo(),
          i.getKind().name(),
          i.label(),
          location ? i.location().city() : null,
          location ? i.location().province() : null,
          i.getLocationKey(),
          i.getSumInsured());
    }
  }

  /**
   * An endorsement of a cover (cover version).
   *
   * @param endorsementNo endorsement number
   * @param type type
   * @param effectiveDate effective date
   * @param policyYear policy year
   * @param invoiceNo invoice
   * @param description description
   */
  public record EndorsementDto(
      String endorsementNo,
      String type,
      LocalDate effectiveDate,
      int policyYear,
      String invoiceNo,
      String description) {

    /**
     * Maps an endorsement.
     *
     * @param e endorsement
     * @return dto
     */
    public static EndorsementDto from(BookingEndorsement e) {
      return new EndorsementDto(
          e.getEndorsementNo(),
          e.getType().name(),
          e.getEffectiveDate(),
          e.getPolicyYear(),
          e.getInvoiceNo(),
          e.getDescription());
    }
  }

  /**
   * A ledger invoice of a cover with its premium and remittance status.
   *
   * @param invoiceNo invoice
   * @param kind booking, endorsement or cancellation
   * @param policyYear policy year
   * @param endorsementNo endorsement, may be null
   * @param currency currency
   * @param grossPremium gross premium
   * @param balance outstanding client premium
   * @param paymentStatus payment status
   * @param remittanceStatus remittance status
   * @param directPayment direct payment to the insurer
   * @param cancelled cancelled
   */
  public record InvoiceDto(
      String invoiceNo,
      String kind,
      int policyYear,
      String endorsementNo,
      String currency,
      BigDecimal grossPremium,
      BigDecimal balance,
      String paymentStatus,
      String remittanceStatus,
      boolean directPayment,
      boolean cancelled) {

    /**
     * Maps a ledger invoice.
     *
     * @param i invoice
     * @return dto
     */
    public static InvoiceDto from(OpsInvoice i) {
      return new InvoiceDto(
          i.getInvoiceNo(),
          i.getKind().name(),
          i.getPolicyYear(),
          i.getEndorsementNo(),
          i.getCurrency(),
          i.getGrossPremium(),
          i.premiumBalance(),
          i.getPaymentStatus().name(),
          i.getRemittanceStatus().name(),
          i.isDpFlag(),
          i.isCancelled());
    }
  }

  /**
   * An invoice not fully paid (premium check).
   *
   * @param invoiceNo invoice
   * @param kind kind
   * @param paymentStatus payment status
   * @param balance balance
   * @param currency currency
   */
  public record UnpaidDto(
      String invoiceNo, String kind, String paymentStatus, BigDecimal balance, String currency) {

    /**
     * Maps an invoice state.
     *
     * @param s state
     * @return dto
     */
    public static UnpaidDto from(InvoiceState s) {
      return new UnpaidDto(s.invoiceNo(), s.kind(), s.paymentStatus().name(), s.balance(), s.currency());
    }
  }

  /**
   * Result of a premium check (BRCLM.001).
   *
   * @param status result
   * @param blocking unpaid or partly paid
   * @param unpaid invoices not fully paid
   */
  public record PremiumDto(ClaimPremiumStatus status, boolean blocking, List<UnpaidDto> unpaid) {

    /**
     * Maps a check.
     *
     * @param c check
     * @return dto
     */
    public static PremiumDto from(PremiumCheck c) {
      return new PremiumDto(c.status(), c.blocking(), c.unpaid().stream().map(UnpaidDto::from).toList());
    }
  }

  /**
   * A claim of a cover.
   *
   * @param id claim id
   * @param claimNo claim number
   * @param policyYear policy year
   * @param lossDate loss date
   * @param reportedDate reported date
   * @param statusCode status
   * @param phase phase
   * @param premiumStatus premium check
   */
  public record CoverClaimDto(
      Long id,
      String claimNo,
      int policyYear,
      LocalDate lossDate,
      LocalDate reportedDate,
      String statusCode,
      String phase,
      String premiumStatus) {

    /**
     * Maps a claim.
     *
     * @param c claim
     * @return dto
     */
    public static CoverClaimDto from(Claim c) {
      return new CoverClaimDto(
          c.getId(),
          c.getClaimNo(),
          c.getCover().getPolicyYear(),
          c.getLoss().getLossDate(),
          c.getLoss().getReportedDate(),
          c.getProgress().getStatusCode(),
          c.getProgress().getPhase().name(),
          c.getCover().getPremiumStatus() == null ? null : c.getCover().getPremiumStatus().name());
    }
  }

  /**
   * An insurer share proposed for a claim.
   *
   * @param insurerCode insurer
   * @param sharePct share in percent
   * @param lead lead insurer
   */
  public record ShareDto(String insurerCode, BigDecimal sharePct, boolean lead) {

    /**
     * Maps a share.
     *
     * @param s share
     * @return dto
     */
    public static ShareDto from(OpsInvoiceShare s) {
      return new ShareDto(s.insurerCode(), s.sharePct(), s.lead());
    }
  }
}
