package com.iortatechnxt.brokerverse.adjustment.api.dto;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.RefundBasis;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * An endorsement request as entered (ADJID.001-004/008/023/028): the invoices (one request per
 * invoice; one invoice for a preview or a change), the type and terms, the amounts of an amount
 * change and the justifications of a duplicate or an over-adjustment.
 *
 * @param invoiceNos invoices of the Operations ledger
 * @param endorsementType list ENDORSEMENT_TYPE
 * @param requestType list ENDORSEMENT_REQUEST_TYPE, may be null
 * @param reasonCode list CANCELLATION_REASON, may be null
 * @param endorsementRef insurer's endorsement reference, may be null
 * @param effectiveDate effective date
 * @param refundBasis PRO_RATA or SHORT_PERIOD, null for pro-rata
 * @param sumInsuredChange TSI change (signed), may be null
 * @param ratePercent premium rate in percent, null for the product rate
 * @param newPeriodFrom new inception (period changes), may be null
 * @param newPeriodTo new expiry (period changes), may be null
 * @param description description of the change
 * @param instructions additional or other instructions, may be null
 * @param amounts amounts of an amount change, may be null
 * @param duplicateOverride why a duplicate proceeds, may be null
 * @param baselineOverride why the baseline may be exceeded, may be null
 */
public record RequestInput(
    List<@Size(max = 40) String> invoiceNos,
    @NotBlank @Size(max = 40) String endorsementType,
    @Size(max = 40) String requestType,
    @Size(max = 40) String reasonCode,
    @Size(max = 60) String endorsementRef,
    @NotNull LocalDate effectiveDate,
    RefundBasis refundBasis,
    BigDecimal sumInsuredChange,
    BigDecimal ratePercent,
    LocalDate newPeriodFrom,
    LocalDate newPeriodTo,
    @NotBlank @Size(max = 1000) String description,
    @Size(max = 1000) String instructions,
    @Valid AmountsInput amounts,
    @Size(max = 500) String duplicateOverride,
    @Size(max = 500) String baselineOverride) {

  /** Missing invoices are none. */
  public RequestInput {
    invoiceNos = invoiceNos == null ? List.of() : List.copyOf(invoiceNos);
  }

  /**
   * The service draft on an invoice.
   *
   * @param invoiceNo invoice
   * @return draft
   */
  public RequestDraft toDraft(String invoiceNo) {
    return new RequestDraft(
        invoiceNo,
        new RequestTerms(
            endorsementType,
            blank(requestType),
            blank(reasonCode),
            blank(endorsementRef),
            effectiveDate,
            refundBasis,
            sumInsuredChange,
            ratePercent,
            newPeriodFrom,
            newPeriodTo,
            description == null ? null : description.strip(),
            blank(instructions)),
        amounts == null ? AmountInput.NONE : amounts.toInput(),
        duplicateOverride,
        baselineOverride);
  }

  /**
   * The first invoice (preview, change).
   *
   * @return invoice number, null when none
   */
  public String firstInvoice() {
    return invoiceNos.isEmpty() ? null : invoiceNos.get(0);
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * Amounts of an amount change (signed; null = no change).
   *
   * @param basic basic premium
   * @param dst documentary stamp tax
   * @param premiumTaxVat premium tax / VAT
   * @param lgt local government tax
   * @param fst fire service tax
   * @param other other charges
   * @param commission commission, null to derive it
   * @param vatOnCommission VAT on commission, null to derive it
   */
  public record AmountsInput(
      BigDecimal basic,
      BigDecimal dst,
      BigDecimal premiumTaxVat,
      BigDecimal lgt,
      BigDecimal fst,
      BigDecimal other,
      BigDecimal commission,
      BigDecimal vatOnCommission) {

    /**
     * The domain value.
     *
     * @return amounts
     */
    public AmountInput toInput() {
      return new AmountInput(
          basic, dst, premiumTaxVat, lgt, fst, other, commission, vatOnCommission);
    }
  }
}
