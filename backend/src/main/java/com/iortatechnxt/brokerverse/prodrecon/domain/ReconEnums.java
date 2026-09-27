package com.iortatechnxt.brokerverse.prodrecon.domain;

/** Enumerations of production reconciliation (PRCID.001-039). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ReconEnums {

  private ReconEnums() {}

  /** Frequency of a scheduled extraction (PRCID.001, OQ29). */
  public enum Frequency {
    /** Once a month on a day of the month. */
    MONTHLY,
    /** Once a week on a day of the week. */
    WEEKLY
  }

  /** What started an extraction. */
  public enum ExtractTrigger {
    /** The {@code PRODUCTION_EXTRACT} job (PRCID.001). */
    SCHEDULED,
    /** A user for a booking period (PRCID.011). */
    MANUAL
  }

  /** Outcome of an insurer upload attempt (PRCID.031/032). */
  public enum UploadStatus {
    /** Registered, rows being read. */
    RECEIVED,
    /** Every row accepted. */
    PROCESSED,
    /** Some rows failed. */
    PARTIAL,
    /** The file could not be processed. */
    FAILED,
    /** Refused: identical to an earlier upload (PRCID.010). */
    DUPLICATE_BLOCKED
  }

  /** Reconciliation status of an item (PRCID.030). */
  public enum ReconStatus {
    /** Keys and every compared field agree within the tolerance. */
    MATCHED,
    /** Keys agree, some fields differ beyond the tolerance. */
    MATCHED_WITH_DISCREPANCY,
    /** Booked by BDOI, absent from the insurer's report. */
    BDOI_ONLY,
    /** Insurer production matched to an account not yet booked (PRCID.023). */
    UNMATCHED_PREBOOKED,
    /** Insurer production without any BDOI account (unbooked). */
    UNMATCHED_NO_BOOKING;

    /**
     * Whether the item is insurer production without a booked invoice (unbooked repository).
     *
     * @return true for the two unmatched statuses
     */
    public boolean isInsurerOnly() {
      return this == UNMATCHED_PREBOOKED || this == UNMATCHED_NO_BOOKING;
    }
  }

  /** How an item was paired. */
  public enum MatchMethod {
    /** By the matching engine. */
    AUTO,
    /** By a reconciliation handler. */
    MANUAL
  }

  /** Resolution of an insurer-only item in the unbooked repository (PRCID.019/033). */
  public enum UnbookedStatus {
    /** Waiting for a booking or a disposition. */
    OPEN,
    /** An account exists but is not booked yet. */
    PREBOOKED,
    /** Booked since and matched. */
    BOOKED,
    /** Closed by a disposition. */
    CLOSED
  }
}
