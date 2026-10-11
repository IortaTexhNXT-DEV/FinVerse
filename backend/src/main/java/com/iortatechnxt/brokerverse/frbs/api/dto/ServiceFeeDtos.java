package com.iortatechnxt.brokerverse.frbs.api.dto;

import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.LineStatus;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItem;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient.RecipientValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule.RuleValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the service-fee endpoints (FRBS 2.10.0-2.10.2). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ServiceFeeDtos {

  private ServiceFeeDtos() {}

  /**
   * A run to compute.
   *
   * @param from first day of the period
   * @param to last day of the period
   */
  public record ComputeBody(@NotNull LocalDate from, @NotNull LocalDate to) {}

  /**
   * A comment on a workflow action.
   *
   * @param comment comment
   */
  public record CommentBody(@Size(max = 500) String comment) {}

  /**
   * A release tag.
   *
   * @param releasedOn date the recipient was credited
   */
  public record ReleaseBody(@NotNull LocalDate releasedOn) {}

  /**
   * A service-fee run.
   *
   * @param id id
   * @param runNo run number
   * @param companyId company
   * @param periodFrom first day
   * @param periodTo last day
   * @param stage stage
   * @param invoiceCount invoices
   * @param feeTotal total fee
   * @param computedAt computed at
   * @param createdBy prepared by
   * @param submittedBy submitted by
   * @param approvedBy approved by
   * @param approvedAt approved at
   */
  public record RunResponse(
      Long id,
      String runNo,
      Long companyId,
      LocalDate periodFrom,
      LocalDate periodTo,
      RunStage stage,
      int invoiceCount,
      BigDecimal feeTotal,
      Instant computedAt,
      String createdBy,
      String submittedBy,
      String approvedBy,
      Instant approvedAt) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return response
     */
    public static RunResponse from(ServiceFeeRun r) {
      return new RunResponse(
          r.getId(),
          r.getRunNo(),
          r.getCompanyId(),
          r.getPeriodFrom(),
          r.getPeriodTo(),
          r.getStage(),
          r.getInvoiceCount(),
          r.getFeeTotal(),
          r.getComputedAt(),
          r.getCreatedBy(),
          r.getSubmittedBy(),
          r.getApprovedBy(),
          r.getApprovedAt());
    }
  }

  /**
   * Amounts of a line.
   *
   * @param currency currency
   * @param rate rate in percent
   * @param invoiceCount invoices
   * @param commission commission
   * @param wtax withholding tax deducted
   * @param base commission base
   * @param fee service fee
   */
  public record LineAmounts(
      String currency,
      BigDecimal rate,
      int invoiceCount,
      BigDecimal commission,
      BigDecimal wtax,
      BigDecimal base,
      BigDecimal fee) {}

  /**
   * Payout of a line.
   *
   * @param accrualBatchNo accrual journal
   * @param requestNo Disbursement request
   * @param gatewayStatus gateway status
   * @param dvNo DV number
   * @param message return reason or information
   * @param sendCount sendings
   */
  public record LinePayout(
      String accrualBatchNo,
      String requestNo,
      String gatewayStatus,
      String dvNo,
      String message,
      int sendCount) {}

  /**
   * Tags of a line.
   *
   * @param releasedOn release date
   * @param releasedBy released by (SYSTEM when reported by Disbursement)
   * @param liquidatedOn liquidation date
   * @param liquidatedBy liquidated by
   * @param liquidationReportId attachment id of the liquidation report
   * @param remarks remarks
   */
  public record LineTags(
      LocalDate releasedOn,
      String releasedBy,
      LocalDate liquidatedOn,
      String liquidatedBy,
      String liquidationReportId,
      String remarks) {}

  /**
   * A service-fee line.
   *
   * @param id id
   * @param lineNo line number
   * @param segment service-fee segment
   * @param salesUnit sales unit
   * @param payeeCode recipient
   * @param payeeName recipient name
   * @param costCenter cost centre
   * @param status status
   * @param amounts amounts
   * @param payout accrual and payout
   * @param tags release and liquidation
   */
  public record LineResponse(
      Long id,
      int lineNo,
      String segment,
      String salesUnit,
      String payeeCode,
      String payeeName,
      String costCenter,
      LineStatus status,
      LineAmounts amounts,
      LinePayout payout,
      LineTags tags) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static LineResponse from(ServiceFeeLine l) {
      return new LineResponse(
          l.getId(),
          l.getLineNo(),
          l.getSegment(),
          l.getSalesUnit(),
          l.getPayeeCode(),
          l.getPayeeName(),
          l.getCostCenter(),
          l.getStatus(),
          new LineAmounts(
              l.getCurrency(),
              l.getRate(),
              l.getInvoiceCount(),
              l.getCommission(),
              l.getWtax(),
              l.getBase(),
              l.getFee()),
          new LinePayout(
              l.getAccrualBatchNo(),
              l.getRequestNo(),
              l.getGatewayStatus(),
              l.getDvNo(),
              l.getGatewayMessage(),
              l.getSendCount()),
          new LineTags(
              l.getReleasedOn(),
              l.getReleasedBy(),
              l.getLiquidatedOn(),
              l.getLiquidatedBy(),
              l.getLiquidationRef(),
              l.getRemarks()));
    }
  }

  /**
   * An invoice of a run.
   *
   * @param lineId line
   * @param invoiceNo invoice
   * @param rootInvoiceNo root invoice
   * @param clientCode client
   * @param assuredName assured
   * @param insurerCode insurer
   * @param marketSegment market segment
   * @param paidOn fully paid on
   * @param commission commission
   * @param wtax withholding tax
   * @param base base
   * @param fee fee
   */
  public record ItemResponse(
      Long lineId,
      String invoiceNo,
      String rootInvoiceNo,
      String clientCode,
      String assuredName,
      String insurerCode,
      String marketSegment,
      LocalDate paidOn,
      BigDecimal commission,
      BigDecimal wtax,
      BigDecimal base,
      BigDecimal fee) {

    /**
     * Maps an invoice.
     *
     * @param i invoice
     * @return response
     */
    public static ItemResponse from(ServiceFeeItem i) {
      return new ItemResponse(
          i.getLineId(),
          i.getInvoiceNo(),
          i.getRootInvoiceNo(),
          i.getClientCode(),
          i.getAssuredName(),
          i.getInsurerCode(),
          i.getMarketSegment(),
          i.getPaidOn(),
          i.getCommission(),
          i.getWtax(),
          i.getBase(),
          i.getFee());
    }
  }

  /**
   * A rule to add or change.
   *
   * @param segment service-fee segment
   * @param marketSegments market segments covered
   * @param rate rate in percent
   * @param netOfWtax commission base net of the insurer's withholding tax
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param active applied
   * @param description description
   */
  public record RuleBody(
      @NotBlank String segment,
      @NotEmpty List<String> marketSegments,
      @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal rate,
      boolean netOfWtax,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      @Size(max = 250) String description) {

    /**
     * The content.
     *
     * @return values
     */
    public RuleValues values() {
      return new RuleValues(
          segment.trim(),
          marketSegments.stream().map(String::trim).toList(),
          rate,
          netOfWtax,
          effectiveFrom,
          effectiveTo,
          active,
          description);
    }
  }

  /**
   * A rule.
   *
   * @param id id
   * @param segment service-fee segment
   * @param marketSegments market segments
   * @param rate rate
   * @param base base
   * @param netOfWtax net of withholding tax
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param active applied
   * @param description description
   */
  public record RuleResponse(
      Long id,
      String segment,
      List<String> marketSegments,
      BigDecimal rate,
      String base,
      boolean netOfWtax,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      String description) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return response
     */
    public static RuleResponse from(ServiceFeeRule r) {
      return new RuleResponse(
          r.getId(),
          r.getSegment(),
          r.getMarketSegments(),
          r.getRate(),
          r.getBase(),
          r.isNetOfWtax(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          r.isActive(),
          r.getDescription());
    }
  }

  /**
   * A recipient to add or change.
   *
   * @param salesUnit sales unit (ignored on change)
   * @param payeeCode payee party code
   * @param payeeName payee name
   * @param costCenter cost centre
   * @param active used
   */
  public record RecipientBody(
      @Size(max = 20) String salesUnit,
      @NotBlank @Size(max = 30) String payeeCode,
      @NotBlank @Size(max = 250) String payeeName,
      @Size(max = 20) String costCenter,
      boolean active) {

    /**
     * The content.
     *
     * @return values
     */
    public RecipientValues values() {
      return new RecipientValues(
          payeeCode.trim(),
          payeeName.trim(),
          costCenter == null || costCenter.isBlank() ? null : costCenter.trim(),
          active);
    }
  }

  /**
   * A recipient.
   *
   * @param id id
   * @param salesUnit sales unit
   * @param payeeCode payee
   * @param payeeName payee name
   * @param costCenter cost centre
   * @param active used
   */
  public record RecipientResponse(
      Long id,
      String salesUnit,
      String payeeCode,
      String payeeName,
      String costCenter,
      boolean active) {

    /**
     * Maps a recipient.
     *
     * @param r recipient
     * @return response
     */
    public static RecipientResponse from(ServiceFeeRecipient r) {
      return new RecipientResponse(
          r.getId(),
          r.getSalesUnit(),
          r.getPayeeCode(),
          r.getPayeeName(),
          r.getCostCenter(),
          r.isActive());
    }
  }
}
