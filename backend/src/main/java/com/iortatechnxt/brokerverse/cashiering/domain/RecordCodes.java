package com.iortatechnxt.brokerverse.cashiering.domain;

/**
 * Coded values of BDOI's creation, cancellation and reinstatement records (Operations Cashiering
 * FRS v3.2: FRS.CSH.02.01, 02.02, 02.05, 03.01, 04.01).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RecordCodes {

  private RecordCodes() {}

  /** What a record does to its receipt; the prefix of its number. */
  public enum RecordKind {
    /** Creation of an AR or OR (prefix CR). */
    CREATION("CR"),
    /** Cancellation of an issued AR or OR (prefix CN). */
    CANCELLATION("CN"),
    /** Reinstatement of an AR or OR (prefix RE). */
    REINSTATEMENT("RE");

    private final String prefix;

    RecordKind(String prefix) {
      this.prefix = prefix;
    }

    /**
     * The number prefix.
     *
     * @return CR, CN or RE
     */
    public String prefix() {
      return prefix;
    }
  }

  /** Status of a record (FRS.CSH.02.01.08 to 02.05.07). */
  public enum RecordStage {
    /** Saved by its creator. */
    CREATED,
    /** Submitted to the Approver/Poster. */
    FOR_POSTING,
    /** Returned by the Approver/Poster to the creator with a reason. */
    RETURNED,
    /** Posted: AR/OR Issued, AR/OR Cancelled or AR/OR Reinstated. */
    POSTED,
    /** Cancelled by its creator before submission. */
    RECORD_CANCELLED
  }

  /** Who pays (FRS.CSH.02.01.02). */
  public enum EntryType {
    /** A client (default for an AR). */
    CLIENT,
    /** An insurer (default for an OR). */
    INSURER,
    /** Another payor. */
    OTHER
  }

  /** Payment type of a record. */
  public enum TenderType {
    /** Cash. */
    CASH,
    /** Check. */
    CHECK,
    /** Direct credit (OR only). */
    DIRECT_CREDIT
  }

  /** Type of reinstatement (FRS.CSH.04.01.06). */
  public enum ReinstatementType {
    /** All the accounts of the receipt. */
    FULL,
    /** The accounts selected (AR only). */
    PARTIAL
  }
}
