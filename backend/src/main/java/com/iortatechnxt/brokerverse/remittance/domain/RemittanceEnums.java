package com.iortatechnxt.brokerverse.remittance.domain;

import java.util.Set;

/** Enumerations of the remittance module (OPERATIONS_DESIGN 4.3, BRD 6.5). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RemittanceEnums {

  private RemittanceEnums() {}

  /** Remittance type of a batch (LOV {@code REMITTANCE_TYPE}, RMTID.001), plus special. */
  public enum RemittanceType {
    /** Early remittance incentive applies (RMTID.023). */
    WITH_INCENTIVES,
    /** Normal remittance in pesos. */
    NORMAL_PHP,
    /** Normal remittance in a foreign currency ("Normal - Dollar"). */
    NORMAL_USD,
    /** Special remittance requested by Marketing (MKTID.009). */
    SPECIAL
  }

  /** Extraction tag of an invoice (RMTID.003). */
  public enum ExtractionTag {
    /** Met the criteria and included in the extract. */
    EXTRACTED,
    /** Did not meet the criteria and is not yet due for remittance. */
    UNEXTRACTED_NOT_DUE,
    /** Did not meet the criteria but is due for remittance (hold, check, over DTIP...). */
    UNEXTRACTED_DUE,
    /** Met the criteria, was extracted and returned by a user. */
    RETURNED
  }

  /** What started an extraction run (RMTID.001/003/004/005, MKTID.009). */
  public enum ExtractionTrigger {
    /** The REMITTANCE_EXTRACTION job. */
    SCHEDULED,
    /** A processor for an insurer and type. */
    MANUAL,
    /** A processor for one invoice (RMTID.004). */
    MANUAL_INVOICE,
    /** End-of-day requests of the day (RMTID.005, OQ18). */
    EOD_QUEUE,
    /** An approved special remittance request (MKTID.009). */
    SPECIAL
  }

  /** Status of an extraction run. */
  public enum RunStatus {
    /** Running. */
    RUNNING,
    /** Completed. */
    SUCCEEDED,
    /** Failed; REMIT_EXTRACTION_FAILED raised. */
    FAILED
  }

  /** Stage of a batch in the OPS_REMITTANCE workflow (RMTID.019/036). */
  public enum BatchStage {
    /** Being reviewed by the processor. */
    REVIEW_IN_PROCESS,
    /** Put on hold by the processor. */
    ON_HOLD,
    /** Submitted, waiting for the team leader. */
    FOR_APPROVAL,
    /** Approved, posted and pushed to Disbursement. */
    APPROVED,
    /** DV assigned; DTIP balance remains on some invoices. */
    PARTIALLY_REMITTED,
    /** DV assigned; no DTIP outstanding. */
    FULLY_REMITTED,
    /** Insurer ORs uploaded for every line. */
    OR_RECEIVED,
    /** Returned; lines re-tagged RETURNED and extractable again. */
    RETURNED;

    private static final Set<BatchStage> EDITABLE = Set.of(REVIEW_IN_PROCESS, ON_HOLD);
    private static final Set<BatchStage> OPEN = Set.of(REVIEW_IN_PROCESS, ON_HOLD, FOR_APPROVAL);

    /**
     * Whether lines may still be excluded or restored.
     *
     * @return true while under review
     */
    public boolean isEditable() {
      return EDITABLE.contains(this);
    }

    /**
     * Whether the batch is not yet approved nor returned.
     *
     * @return true before approval
     */
    public boolean isOpen() {
      return OPEN.contains(this);
    }
  }

  /** Comparison of an insurer OR amount with the paid AR of the line (RMTID.016). */
  public enum OrStatus {
    /** OR amount equals the paid AR. */
    MATCHED,
    /** OR amount differs from the paid AR. */
    AMOUNT_MISMATCH
  }

  /** Stored documents of a batch (RMTID.011). */
  public enum DocumentKind {
    /** Remittance schedule, PDF. */
    SCHEDULE_PDF,
    /** Remittance schedule, Excel (sent to the insurer, MKTID.001). */
    SCHEDULE_XLSX,
    /** Payment request to Disbursement, PDF. */
    PAYMENT_REQUEST_PDF
  }

  /** Stage of a hold request in the OPS_HOLD workflow (MKTID.002-007). */
  public enum HoldStage {
    /** Being prepared by Marketing. */
    DRAFT,
    /** Waiting for approval. */
    FOR_APPROVAL,
    /** Invoice on hold. */
    ACTIVE,
    /** Extension waiting for approval. */
    EXTENSION_FOR_APPROVAL,
    /** Cancellation waiting for approval. */
    CANCEL_FOR_APPROVAL,
    /** Released, expired or cancelled after approval. */
    RELEASED,
    /** Rejected by the approver. */
    REJECTED,
    /** Draft cancelled. */
    CANCELLED;

    private static final Set<HoldStage> LIVE =
        Set.of(DRAFT, FOR_APPROVAL, ACTIVE, EXTENSION_FOR_APPROVAL, CANCEL_FOR_APPROVAL);

    /**
     * Whether the request still concerns its invoice.
     *
     * @return true until released, rejected or cancelled
     */
    public boolean isLive() {
      return LIVE.contains(this);
    }

    /**
     * Whether the invoice is held (approved and not released).
     *
     * @return true for the active stages
     */
    public boolean holdsInvoice() {
      return this == ACTIVE || this == EXTENSION_FOR_APPROVAL || this == CANCEL_FOR_APPROVAL;
    }
  }

  /** Stage of a special remittance request in the OPS_SPECIAL_REMIT workflow (MKTID.009). */
  public enum SpecialStage {
    /** Received, being validated. */
    REQUESTED,
    /** Validated, waiting for approval. */
    FOR_APPROVAL,
    /** Approved; its special batch is in Process Remittance. */
    IN_PROCESS_REMITTANCE,
    /** The special batch was approved and pushed to Disbursement. */
    PUSHED_TO_DISBURSEMENT,
    /** Rejected by the approver. */
    REJECTED,
    /** The special batch was returned. */
    RETURNED
  }

  /** Where a hold or special remittance request came from (RMTID.030, OQ45). */
  public enum RequestSource {
    /** Entered on the remittance screens. */
    SCREEN,
    /** Uploaded through a Collection flow-in feed. */
    COLLECTION_FEED
  }

  /** Start date of an early-remittance incentive window (RMTID.023, OQ23). */
  public enum IncentiveBasis {
    /** From the inception date. */
    INCEPTION,
    /** From the booking date. */
    BOOKING
  }
}
