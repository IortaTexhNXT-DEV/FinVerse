package com.iortatechnxt.brokerverse.disbursement.domain;

/** Enumerations of the Disbursement module (ACCOUNTING_DISBURSEMENT_DESIGN 5.1, 7.1-7.3). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class DisbursementEnums {

  private DisbursementEnums() {}

  /** Stage of a payee in workflow {@code DISB_PAYEE} (DIS 2.2.0-2.2.7). */
  public enum PayeeStage {
    /** Saved as a draft by the maker (DIS 2.2.7). */
    DRAFT,
    /** Waiting for the checker. */
    FOR_AUTHORIZATION,
    /** Usable in payment requests and vouchers. */
    ACTIVE,
    /** Deactivation waiting for the checker. */
    FOR_DEACTIVATION,
    /** Not usable. */
    INACTIVE,
    /** Reactivation waiting for the checker. */
    FOR_REACTIVATION;

    /**
     * Whether payments may be made to the payee: active, or still active while its deactivation
     * waits for the checker.
     *
     * @return true when usable
     */
    public boolean usable() {
      return this == ACTIVE || this == FOR_DEACTIVATION;
    }
  }

  /** Where a payee record came from (DIS 2.2.1, 2.2.8). */
  public enum PayeeSource {
    /** Encoded by a Disbursement user. */
    MANUAL,
    /** Payee upload. */
    UPLOAD,
    /** Migrated from the current system ({@code DISB_PAYEE_MIGRATION}). */
    MIGRATION,
    /** Created from a payee maintenance request (RRF, no-match fall-out). */
    REQUEST
  }

  /** Mode of payment (DIS 2.2.5, 2.7.0). */
  public enum DisbursementMode {
    /** Credit to account, through the Direct Credit Transaction File (TPD / ACA). */
    CTA,
    /** Debit of the BDOIR main account by authority to debit, processed at the branch. */
    ATD,
    /** Manager's check or demand draft issued by the branch. */
    MC_DD,
    /** Credit ticket processed at the branch of account. */
    CREDIT_TICKET,
    /** Telegraphic transfer. */
    TT,
    /** BDO Business Online Banking. */
    ONLINE_BANKING,
    /** Check printed from the check series of the paying account. */
    CHECK
  }

  /** How a payment request reached Disbursement (DIS 2.5.0, 2.6.0-2.6.2). */
  public enum RequestSource {
    /** A module through the {@code DisbursementGateway} port. */
    GATEWAY,
    /** A row of an uploaded request file ({@code DISB_REQUESTS}). */
    UPLOAD,
    /** Encoded by a Disbursement user from an e-mail request. */
    ENCODED,
    /** A refund or cash-advance request of {@code payrequest} (through the gateway). */
    PAYREQUEST
  }

  /** Status of a payment request in Disbursement. */
  public enum RequestStatus {
    /** Received, no voucher yet. */
    RECEIVED,
    /** The payee is not maintained (DIS 3.25.0, 3.25.2). */
    NO_PAYEE,
    /** A voucher was created for it. */
    IN_VOUCHER,
    /** Documents released without a voucher (BIR 2307 certificates, DBMID.001). */
    RELEASED,
    /** Returned to the source with a reason. */
    RETURNED,
    /** Cancelled with its voucher. */
    CANCELLED
  }

  /** Stage of a disbursement voucher in workflow {@code DISB_VOUCHER} (DIS 2.7-2.21). */
  public enum VoucherStage {
    /** With the processor. */
    IN_PROCESS,
    /** With the team leader (checker). */
    FOR_REVIEW,
    /** With the approver. */
    FOR_APPROVAL,
    /** Approved and posted. */
    APPROVED,
    /** Rejected by the approver (terminal). */
    REJECTED,
    /** Cancelled (terminal); an approved DV is reversed. */
    CANCELLED;

    /**
     * Whether the voucher can still be changed (DIS 3.27.0 addendum: only unposted items).
     *
     * @return true before approval
     */
    public boolean editable() {
      return this == IN_PROCESS || this == FOR_REVIEW || this == FOR_APPROVAL;
    }
  }

  /** Accounting state of a voucher (DIS 3.27.0). */
  public enum PostingStatus {
    /** Not approved yet. */
    NOT_POSTED,
    /** Posted at approval. */
    POSTED,
    /** The approval posting failed; listed as unregularised. */
    FAILED,
    /** Reversed by the cancellation of the approved DV. */
    REVERSED,
    /** The reversal failed; listed as unregularised. */
    REVERSAL_FAILED
  }

  /** Where a proforma line comes from (DIS 2.7.6, 2.7.10). */
  public enum LineOrigin {
    /** Built by the accounting rule of {@code DISB_VOUCHER}. */
    RULE,
    /** Changed or added by the processor. */
    EDITED,
    /** From the expense allocation. */
    ALLOCATION
  }

  /** Status of a payment instrument (DIS 2.8.x, 3.26.x; design 7.2). */
  public enum InstrumentStatus {
    /** Created with the approved DV; nothing produced yet. */
    PENDING,
    /** Check, ATD, form or MC / DD request printed. */
    PRINTED,
    /** Check or MC / DD handed to the payee. */
    RELEASED,
    /** Check deposited by the payee (deposited-checks file). */
    NEGOTIATED,
    /** Check not negotiated after {@code DISB_STALE_DAYS}. */
    STALE,
    /** Instrument cancelled with its DV. */
    CANCELLED,
    /** ATD e-mailed to the processing branch. */
    EMAILED,
    /** Branch or BOB confirmed the debit. */
    DEBITED,
    /** Credit to account included in the Direct Credit Transaction File. */
    EXTRACTED,
    /** Credit confirmed by the credited-accounts file. */
    CREDITED,
    /** MC / DD received from the branch. */
    RECEIVED,
    /** Online banking transaction approved in BrokerVerse, waiting for BOB. */
    APPROVED
  }

  /** What changed an instrument status. */
  public enum EventSource {
    /** A Disbursement user. */
    USER,
    /** The system (approval, print, end of day). */
    SYSTEM,
    /** An uploaded bank file. */
    UPLOAD,
    /** A scheduled job. */
    JOB
  }

  /** Kind of tag on a DV (DIS 2.10, 2.11). */
  public enum TagKind {
    /** Official receipt or acknowledgement receipt received from the payee. */
    OR_AR,
    /** Creditable withholding tax certificate. */
    CWT
  }

  /** Direction of a CWT certificate (DIS 2.11.1). */
  public enum CwtDirection {
    /** Certificate received from an insurer (commission, incentives). */
    RECEIVED,
    /** BDOI's BIR 2307 released to a supplier. */
    RELEASED
  }

  /** Stage of a status edit in workflow {@code DISB_STATUS_EDIT} (DIS 2.8.5). */
  public enum StatusEditStage {
    /** Waiting for the team leader. */
    REQUESTED,
    /** Approved and applied. */
    APPLIED,
    /** Rejected. */
    REJECTED
  }

  /** Stage of a funding request in workflow {@code DISB_FUNDING} (DIS 2.17). */
  public enum FundingStage {
    /** With the maker. */
    CREATED,
    /** With a team leader other than the maker. */
    FOR_VERIFICATION,
    /** With the first approver. */
    FOR_APPROVAL_1,
    /** With the second approver. */
    FOR_APPROVAL_2,
    /** Approved and posted. */
    APPROVED,
    /** Declined (terminal). */
    DECLINED,
    /** Cancelled by the maker (terminal). */
    CANCELLED
  }

  /** Status of a payee maintenance request (DIS 2.2.1). */
  public enum PayeeRequestStatus {
    /** Waiting for a payee maintainer. */
    OPEN,
    /** The payee was maintained. */
    DONE,
    /** Closed without a payee. */
    CANCELLED
  }

  /** Source of a payee maintenance request (DIS 2.2.1, 3.25.2). */
  public enum PayeeRequestSource {
    /** Refund request form of Marketing (MKT 2.25.0). */
    RRF,
    /** Requested by a Disbursement user. */
    DISBURSEMENT,
    /** A payment request whose payee did not match the master. */
    NO_MATCH
  }

  /** Status of an end-of-day run (DIS 2.16.0, 2.7.12). */
  public enum EodStatus {
    /** Outputs produced. */
    COMPLETED,
    /** Payment confirmations e-mailed. */
    CONFIRMED
  }

  /** Kind of end-of-day output (DIS 2.16.1-2.16.6, 3.28.2). */
  public enum OutputKind {
    /** Direct Credit Transaction File. */
    DCTF,
    /** Check print batch. */
    CHECKS,
    /** Authorities to debit. */
    ATD,
    /** Manager's check / demand draft forms. */
    MC_DD,
    /** Credit ticket forms. */
    CREDIT_TICKET,
    /** Telegraphic transfer forms. */
    TT,
    /** The day's disbursement vouchers. */
    VOUCHERS,
    /** An end-of-day report. */
    REPORT
  }
}
