package com.iortatechnxt.brokerverse.opsledger.domain;

/** Enumerations of the flow-in framework (BRQID.004/005). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class FlowInEnums {

  private FlowInEnums() {}

  /** Direction of a feed. */
  public enum Direction {
    /** Data flowing into BrokerVerse. */
    INBOUND,
    /** Data sent out of BrokerVerse. */
    OUTBOUND
  }

  /** Transport of a feed; only the default transports are built until BDOI's specs (OQ01). */
  public enum Transport {
    /** Files uploaded by users (default for external systems). */
    MANUAL_UPLOAD,
    /** Shared-drive folder, served by the in-system extract repository (OQ17). */
    FILE_DROP,
    /** Inside BrokerVerse (module events, in-app queues). */
    IN_APP,
    /** Interface API (parked). */
    API,
    /** SFTP (parked). */
    SFTP
  }

  /** What started a run. */
  public enum Trigger {
    /** The feed's schedule. */
    SCHEDULED,
    /** A user started it. */
    MANUAL,
    /** A file upload. */
    UPLOAD,
    /** A module event. */
    EVENT,
    /** A replay of earlier data. */
    REPLAY
  }

  /** Outcome of a run. */
  public enum RunStatus {
    /** Still running. */
    RUNNING,
    /** Every record accepted (or skipped as duplicate). */
    SUCCEEDED,
    /** Some records failed; the others were processed (BRQID.006). */
    PARTIAL,
    /** The run failed. */
    FAILED
  }

  /** Outcome of one record. */
  public enum RecordStatus {
    /** Processed. */
    ACCEPTED,
    /** Refused with a message; may be sent again. */
    FAILED
  }
}
