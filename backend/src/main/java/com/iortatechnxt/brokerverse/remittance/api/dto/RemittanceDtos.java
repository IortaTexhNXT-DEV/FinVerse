package com.iortatechnxt.brokerverse.remittance.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRecord;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.EodRequest;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.OrStatus;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RunStatus;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads.UploadResult;
import com.iortatechnxt.brokerverse.remittance.service.LedgerPositions;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService.DtipStatus;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Position;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the extraction, search, DTIP and set-up endpoints. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RemittanceDtos {

  private RemittanceDtos() {}

  /**
   * An extraction run (RMTID.001/003/004).
   *
   * @param id id
   * @param runNo number
   * @param trigger trigger
   * @param insurerCode insurer scope
   * @param type type scope
   * @param invoiceNo invoice scope
   * @param businessDate business date
   * @param startedAt start
   * @param endedAt end
   * @param status status
   * @param examined invoices examined
   * @param extracted extracted
   * @param dueNotExtracted due but not extracted
   * @param notDue not yet due
   * @param batches batches created
   * @param message summary or error
   * @param createdBy user (SYSTEM for the job)
   */
  public record RunResponse(
      Long id,
      String runNo,
      ExtractionTrigger trigger,
      String insurerCode,
      RemittanceType type,
      String invoiceNo,
      LocalDate businessDate,
      Instant startedAt,
      Instant endedAt,
      RunStatus status,
      int examined,
      int extracted,
      int dueNotExtracted,
      int notDue,
      int batches,
      String message,
      String createdBy) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return DTO
     */
    public static RunResponse from(ExtractionRun r) {
      return new RunResponse(
          r.getId(),
          r.getRunNo(),
          r.getTrigger(),
          r.getInsurerCode(),
          r.getRemittanceType(),
          r.getInvoiceNo(),
          r.getBusinessDate(),
          r.getStartedAt(),
          r.getEndedAt(),
          r.getStatus(),
          r.getExaminedCount(),
          r.getExtractedCount(),
          r.getDueCount(),
          r.getNotDueCount(),
          r.getBatchCount(),
          r.getMessage(),
          r.getCreatedBy());
    }
  }

  /**
   * An extraction tag.
   *
   * @param invoiceNo invoice
   * @param insurerCode insurer
   * @param type remittance type
   * @param tag tag
   * @param reasons exclusion reasons
   * @param remarks remarks
   * @param paidAr paid AR not yet remitted
   * @param dtipBalance DTIP balance
   * @param remittable amount remitted now
   * @param batchNo batch
   * @param taggedAt time
   */
  public record TagResponse(
      String invoiceNo,
      String insurerCode,
      RemittanceType type,
      ExtractionTag tag,
      String reasons,
      String remarks,
      BigDecimal paidAr,
      BigDecimal dtipBalance,
      BigDecimal remittable,
      String batchNo,
      Instant taggedAt) {

    /**
     * Maps a tag.
     *
     * @param t tag
     * @return DTO
     */
    public static TagResponse from(InvoiceTag t) {
      return new TagResponse(
          t.getInvoiceNo(),
          t.getInsurerCode(),
          t.getRemittanceType(),
          t.getTag(),
          t.getReasons(),
          t.getRemarks(),
          t.getPaidAr(),
          t.getDtipBalance(),
          t.getRemittable(),
          t.getBatchNo(),
          t.getCreatedAt());
    }
  }

  /**
   * A manual extraction (RMTID.001/004).
   *
   * @param companyId company
   * @param insurerCode insurer, null for all
   * @param type remittance type, null for all
   * @param invoiceNo one invoice, null for all
   */
  public record ExtractionRequest(
      @NotNull Long companyId,
      @Size(max = 30) String insurerCode,
      RemittanceType type,
      @Size(max = 40) String invoiceNo) {}

  /**
   * An invoice to queue for the end-of-day extraction (RMTID.005).
   *
   * @param companyId company
   * @param invoiceNo invoice
   */
  public record EodRequestBody(@NotNull Long companyId, @NotBlank String invoiceNo) {}

  /**
   * An end-of-day request.
   *
   * @param invoiceNo invoice
   * @param requestedOn day
   * @param requestedBy user
   * @param runNo run that processed it
   * @param tag tag given
   * @param processedAt time
   */
  public record EodResponse(
      String invoiceNo,
      LocalDate requestedOn,
      String requestedBy,
      String runNo,
      ExtractionTag tag,
      Instant processedAt) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return DTO
     */
    public static EodResponse from(EodRequest r) {
      return new EodResponse(
          r.getInvoiceNo(),
          r.getRequestedOn(),
          r.getRequestedBy(),
          r.getRunNo(),
          r.getTag(),
          r.getProcessedAt());
    }
  }

  /**
   * An account found in a batch (RMTID.025).
   *
   * @param batchId batch
   * @param batchNo batch number
   * @param stage batch stage
   * @param line the account
   */
  public record AccountResponse(Long batchId, String batchNo, String stage, BatchDtos.Line line) {

    /**
     * Maps a line.
     *
     * @param l line with its batch
     * @return DTO
     */
    public static AccountResponse from(BatchLine l) {
      return new AccountResponse(
          l.getBatch().getId(),
          l.getBatch().getBatchNo(),
          l.getBatch().getStage().name(),
          BatchDtos.Line.from(l));
    }
  }

  /**
   * The DTIP status of an invoice (RMTID.028/032).
   *
   * @param invoiceNo invoice
   * @param arn ARN
   * @param insurerCode insurer
   * @param assuredName assured
   * @param currency currency
   * @param inceptionDate inception
   * @param paymentStatus payment status
   * @param remittanceStatus remittance status
   * @param hold on hold
   * @param pendingNegativeAdjustment negative adjustment pending
   * @param writtenOff written off
   * @param lockOwner lock owner
   * @param paidAr paid AR
   * @param dtipDue DTIP due
   * @param dtipRemitted DTIP remitted
   * @param dtipBalance DTIP outstanding
   * @param tag current extraction tag
   * @param reasons its reasons
   */
  public record DtipRow(
      String invoiceNo,
      String arn,
      String insurerCode,
      String assuredName,
      String currency,
      LocalDate inceptionDate,
      String paymentStatus,
      String remittanceStatus,
      boolean hold,
      boolean pendingNegativeAdjustment,
      boolean writtenOff,
      String lockOwner,
      BigDecimal paidAr,
      BigDecimal dtipDue,
      BigDecimal dtipRemitted,
      BigDecimal dtipBalance,
      ExtractionTag tag,
      String reasons) {

    /**
     * Maps an invoice and its tag.
     *
     * @param s DTIP status
     * @return DTO
     */
    public static DtipRow from(DtipStatus s) {
      OpsInvoice i = s.invoice();
      Position p = LedgerPositions.position(i);
      OpsInvoiceComponent dtip = i.component(LedgerComponent.DTIP);
      InvoiceTag t = s.tag();
      return new DtipRow(
          i.getInvoiceNo(),
          i.getArn(),
          i.getInsurerCode(),
          i.getAssuredName(),
          i.getCurrency(),
          i.getClassification().inceptionDate(),
          i.getPaymentStatus().name(),
          i.getRemittanceStatus().name(),
          i.isHoldFlag(),
          i.isPendingNegAdj(),
          i.isWrittenOff(),
          i.getLockOwner(),
          p.paidAr(),
          dtip.due(),
          dtip.getRemitted(),
          dtip.getBalance(),
          t == null ? null : t.getTag(),
          t == null ? null : t.getReasons());
    }
  }

  /**
   * An early-remittance incentive rule (RMTID.023).
   *
   * @param id id
   * @param insurerCode insurer
   * @param productLine product line, null for all
   * @param segment segment, null for all
   * @param rate percent of the basic premium
   * @param windowDays window
   * @param basis inception or booking
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param active active
   * @param description description
   */
  public record IncentiveRuleResponse(
      Long id,
      String insurerCode,
      String productLine,
      String segment,
      BigDecimal rate,
      int windowDays,
      IncentiveBasis basis,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      String description) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return DTO
     */
    public static IncentiveRuleResponse from(EarlyIncentiveRule r) {
      return new IncentiveRuleResponse(
          r.getId(),
          r.getInsurerCode(),
          r.getProductLine(),
          r.getSegment(),
          r.getRate(),
          r.getWindowDays(),
          r.getBasis(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          r.isActive(),
          r.getDescription());
    }
  }

  /**
   * A rule to save.
   *
   * @param companyId company (create)
   * @param insurerCode insurer
   * @param productLine product line, blank for all
   * @param segment segment, blank for all
   * @param rate percent of the basic premium
   * @param windowDays window
   * @param basis inception or booking
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param active active
   * @param description description
   */
  public record IncentiveRuleRequest(
      Long companyId,
      @NotBlank @Size(max = 30) String insurerCode,
      @Size(max = 30) String productLine,
      @Size(max = 40) String segment,
      @NotNull @DecimalMin(value = "0.0001") @DecimalMax("100") BigDecimal rate,
      @Min(1) @Max(366) int windowDays,
      @NotNull IncentiveBasis basis,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      @Size(max = 250) String description) {

    /**
     * The rule terms.
     *
     * @return terms
     */
    public EarlyIncentiveRule.Terms terms() {
      return new EarlyIncentiveRule.Terms(
          insurerCode.strip(),
          productLine,
          segment,
          rate,
          windowDays,
          basis,
          effectiveFrom,
          effectiveTo,
          active,
          description);
    }
  }

  /**
   * A feed run (uploads of the remittance feeds).
   *
   * @param id id
   * @param runNo run number
   * @param feedCode feed
   * @param fileName file
   * @param status status
   * @param read records read
   * @param accepted accepted
   * @param duplicates duplicates skipped
   * @param failed failed
   * @param message summary
   * @param errorDetail error of the run
   * @param startedAt start
   * @param createdBy user
   */
  public record FeedRunResponse(
      Long id,
      String runNo,
      String feedCode,
      String fileName,
      String status,
      int read,
      int accepted,
      int duplicates,
      int failed,
      String message,
      String errorDetail,
      Instant startedAt,
      String createdBy) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return DTO
     */
    public static FeedRunResponse from(FlowInRun r) {
      return new FeedRunResponse(
          r.getId(),
          r.getRunNo(),
          r.getFeedCode(),
          r.getFileName(),
          r.getStatus().name(),
          r.getReadCount(),
          r.getOkCount(),
          r.getDuplicateCount(),
          r.getFailedCount(),
          r.getMessage(),
          r.getErrorDetail(),
          r.getStartedAt(),
          r.getCreatedBy());
    }
  }

  /**
   * A record of a feed run.
   *
   * @param key idempotency key
   * @param status accepted or failed
   * @param reference what it created or updated
   * @param message failure reason
   */
  public record FeedRecordResponse(String key, String status, String reference, String message) {

    /**
     * Maps a record.
     *
     * @param r record
     * @return DTO
     */
    public static FeedRecordResponse from(FlowInRecord r) {
      return new FeedRecordResponse(
          r.getIdempotencyKey(), r.getStatus().name(), r.getReference(), r.getMessage());
    }
  }

  /**
   * An insurer OR upload and its exception report (RMTID.016).
   *
   * @param run the run
   * @param updated accounts updated with their exception status
   * @param records every record (failed ones with the reason)
   * @param matched accounts whose OR equals the paid AR
   * @param mismatched accounts whose OR differs
   */
  public record OrUploadResponse(
      FeedRunResponse run,
      List<RemittanceDtos.AccountResponse> updated,
      List<FeedRecordResponse> records,
      long matched,
      long mismatched) {

    /**
     * Maps an upload result.
     *
     * @param r result
     * @return DTO
     */
    public static OrUploadResponse from(UploadResult r) {
      List<AccountResponse> updated = r.updated().stream().map(AccountResponse::from).toList();
      long matched = r.updated().stream().filter(l -> l.getOrStatus() == OrStatus.MATCHED).count();
      return new OrUploadResponse(
          FeedRunResponse.from(r.run()),
          updated,
          r.records().stream().map(FeedRecordResponse::from).toList(),
          matched,
          r.updated().size() - matched);
    }
  }
}
