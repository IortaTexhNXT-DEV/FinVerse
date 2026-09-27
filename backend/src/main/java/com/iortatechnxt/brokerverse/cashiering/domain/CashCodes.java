package com.iortatechnxt.brokerverse.cashiering.domain;

/** Coded values of the cashiering records (OPERATIONS_DESIGN 4.2). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class CashCodes {

  private CashCodes() {}

  /** Receipt document kind (design principle 1: trust money vs BDOI income). */
  public enum ReceiptKind {
    /** Acknowledgement receipt: premium or non-premium payment (CSHID.001). */
    AR,
    /** Official receipt: BDOI income, Head Office only (CSHID.002/006). */
    OR
  }

  /** Receipt status. */
  public enum ReceiptStatus {
    /** Issued and posted. */
    ISSUED,
    /** Cancelled: postings and applications reversed (CSHID.012). */
    CANCELLED,
    /** Cancelled, then reinstated in full or in part (CSHID.013). */
    REINSTATED
  }

  /** Mode of payment. */
  public enum PaymentMode {
    /** Cash. */
    CASH,
    /** Check. */
    CHECK,
    /** Bills payment. */
    BILLS_PAYMENT,
    /** Trade (CIB). */
    TRADE,
    /** CLPC. */
    CLPC,
    /** Post-dated check. */
    PDC,
    /** Direct credit. */
    DIRECT_CREDIT,
    /** Auto-debit arrangement. */
    ADA,
    /** Credit to account. */
    CREDIT_TO_ACCOUNT,
    /** No cash (settlement OR netted in a remittance). */
    NON_CASH
  }

  /** Where a receipt comes from. */
  public enum ReceiptSource {
    /** Over the counter. */
    OTC,
    /** Payment file upload. */
    UPLOAD,
    /** Matured PDC. */
    PDC,
    /** Check pick-up. */
    PICKUP,
    /** Settlement OR requested by another Operations module. */
    SETTLEMENT,
    /** Commission payment details upload (CSHID.007). */
    COMMISSION_UPLOAD,
    /** BIR 2307 cash path (CSHID.026). */
    CWT,
    /** Reinstatement. */
    REINSTATEMENT
  }

  /** Cancellation or reinstatement transaction (CSHID.001-005). */
  public enum ReceiptActionType {
    /** Cancellation. */
    CANCEL,
    /** Full reinstatement. */
    REINSTATE_FULL,
    /** Partial reinstatement. */
    REINSTATE_PARTIAL
  }

  /** Payment channel (CSHID.008/009). */
  public enum PaymentChannel {
    /** Over the counter. */
    OTC,
    /** Bills Payment file (IT-DCO FS01). */
    BILLS_PAYMENT,
    /** Trade file (CIB). */
    TRADE,
    /** CLPC file. */
    CLPC,
    /** Matured PDC. */
    PDC,
    /** Direct Credit file (FS01/04). */
    DIRECT_CREDIT,
    /** Check pick-up. */
    PICKUP,
    /** BIR 2307 cash path. */
    CWT
  }

  /** Matching result of a payment (CSHID.020). */
  public enum MatchCategory {
    /** Applied to booked invoices with outstanding premium. */
    APPLIED,
    /** Matched to a pre-booked account; waits for the booking. */
    PREBOOKED,
    /** No booked or pre-booked account: unapplied. */
    UNAPPLIED_NO_MATCH,
    /** More than the outstanding premium (or nothing outstanding): the excess is unapplied. */
    EXCESS,
    /** Matched a cancelled booking: unapplied. */
    CANCELLED_REFERENCE
  }

  /** What created an application. */
  public enum ApplicationSource {
    /** Payment at acceptance. */
    PAYMENT,
    /** Pre-booked payment re-matched after booking. */
    PREBOOKED,
    /** Unapplied disposition. */
    DISPOSITION,
    /** Reinstatement. */
    REINSTATEMENT,
    /** Re-application after a premium change (ADJID.009/012/013). */
    REAPPLY,
    /** Automatch of an unapplied payment. */
    AUTOMATCH,
    /** BIR 2307 cash path. */
    CWT
  }

  /** Where an unapplied item comes from. */
  public enum UnappliedOrigin {
    /** Payment without a matching account. */
    NO_MATCH,
    /** Payment above the outstanding premium. */
    EXCESS,
    /** Payment against a cancelled booking. */
    CANCELLED_REFERENCE,
    /** Adjustment excess. */
    ADJUSTMENT,
    /** Cancellation excess. */
    CANCELLATION,
    /** Direct payment reinstatement (commission). */
    DP_REINSTATE,
    /** Return of an excluded paid AR (remittance). */
    REMITTANCE_RETURN,
    /** Re-application excess. */
    REAPPLY,
    /** Pre-booked payment released to unapplied. */
    PREBOOKED,
    /** Other. */
    OTHER
  }

  /** What a disposition type does (CSHID.024). */
  public enum DispositionAction {
    /** Apply to another invoice. */
    APPLY,
    /** Apply to the DST of an invoice only. */
    DST_APPLY,
    /** Refund through Disbursement. */
    REFUND,
    /** Reclassify to another client. */
    RECLASS,
    /** Transfer to another marketing unit. */
    TRANSFER,
    /** Settled outside the system. */
    MANUAL
  }

  /** Status of a disposition. */
  public enum DispositionStatus {
    /** Assigned, being worked. */
    MONITORING,
    /** Waiting for the team leader. */
    FOR_APPROVAL,
    /** Approved, being executed. */
    IN_PROCESS,
    /** Executed. */
    COMPLETED,
    /** Marked for reversal. */
    FOR_REVERSAL,
    /** Reversed. */
    REVERSED,
    /** Withdrawn before execution. */
    WITHDRAWN
  }

  /** PDC warehouse status (CSHID.008 item 4). */
  public enum PdcStatus {
    /** In the warehouse. */
    WAREHOUSED,
    /** Matured, payment created. */
    MATURED,
    /** Payment applied. */
    APPLIED,
    /** Returned to the client. */
    RETURNED,
    /** Replaced by another check. */
    REPLACED,
    /** Pulled out. */
    PULLED_OUT
  }

  /** Check pick-up status (CSHID.009). */
  public enum PickupStatus {
    /** Waiting for the pick-up date and AR. */
    FOR_PICKUP,
    /** AR printed. */
    AR_PRINTED,
    /** Cancelled. */
    CANCELLED
  }

  /** BIR 2307 settlement path (CSHID.026). */
  public enum CwtPath {
    /** Client pays the 2% in cash. */
    CASH,
    /** Client submits the BIR 2307 certificate. */
    CERTIFICATE
  }
}
