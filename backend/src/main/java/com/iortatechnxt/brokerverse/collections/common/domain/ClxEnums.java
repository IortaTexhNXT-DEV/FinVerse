package com.iortatechnxt.brokerverse.collections.common.domain;

/** Codes of the Collections core (COLLECTIONS_DESIGN 4.1, BRCLXN.001-023, 052). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ClxEnums {

  private ClxEnums() {}

  /** Status of a collection item (BRCLXN.005-010, 022, CQ04). */
  public enum ItemStatus {
    /** Net outstanding premium above the threshold: in the worklist. */
    OPEN,
    /** Zero or below the threshold: closed, history kept (BRCLXN.008, 022). */
    COMPLETED,
    /** Negative balance of a cancellation / return invoice: never listed (BRCLXN.010). */
    EXCLUDED_CANCELLED,
    /** Negative balance of another invoice: credit view (CQ04). */
    CREDIT
  }

  /** Invoice category of the reports (BRCLXN.036): regular or direct bill (direct payment). */
  public enum InvoiceCategory {
    /** Premium paid to BDOI. */
    REGULAR,
    /** Premium paid directly to the insurer. */
    DIRECT_BILL
  }

  /** Whose action the next step is (p.41): Marketing until the account is Operations'. */
  public enum TaggingOwner {
    /** Marketing (collection handler, AO). */
    MARKETING,
    /** Operations (Cashiering, Commission, Adjustment). */
    OPERATIONS
  }

  /** Operations hand-off of a PR collector disposition (LOV attribute {@code ops_action}). */
  public enum OpsAction {
    /** No hand-off. */
    NONE,
    /** BIR 2307 tag to Cashiering ({@code COLLECTION_CWT2307}). */
    CWT2307_REVERSAL,
    /** Direct payment PR reversal to Commission ({@code COLLECTION_DP_LIST}). */
    DP_REVERSAL,
    /** Check pick-up to Cashiering ({@code COLLECTION_CHECK_PICKUP}). */
    CHECK_PICKUP,
    /** Cancellation to be raised in Adjustment (seam, no feed). */
    CANCEL_REQUEST
  }

  /** Kind of an assignment row (BRCLXN.052). */
  public enum AssignmentKind {
    /** Permanent reassignment. */
    PERMANENT,
    /** Temporary reassignment with an end date, reverted by the daily refresh. */
    TEMPORARY,
    /** Default assignment by an assignment rule. */
    RULE,
    /** Automatic return to the handler before a temporary assignment. */
    REVERT
  }

  /** Who recorded a disposition. */
  public enum DispositionSource {
    /** A user on one account. */
    USER,
    /** A user on several accounts at once (bulk reference). */
    BULK,
    /** The inbox (e.g. direct payment returned by the insurer). */
    INBOX
  }

  /** Status of an outbox row (COLLECTIONS_DESIGN 2.2). */
  public enum OutboxStatus {
    /** Waiting for the consumer. */
    PENDING,
    /** Acknowledged by the consumer. */
    TAKEN,
    /** Superseded by a later disposition before it was taken. */
    CANCELLED
  }
}
