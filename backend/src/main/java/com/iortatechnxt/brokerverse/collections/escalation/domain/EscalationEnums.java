package com.iortatechnxt.brokerverse.collections.escalation.domain;

/** Enumerations of the escalation rules and cases (BRCLXN.049/050, COLLECTIONS_DESIGN 4.3). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class EscalationEnums {

  private EscalationEnums() {}

  /** What a rule measures; the threshold is in days, a count or an amount (BRCLXN.049). */
  public enum Basis {
    /** Days since the booking date. */
    AGING_FROM_BOOKING,
    /** Days since the inception date (e.g. the 60th day from inception, p.40). */
    AGING_FROM_INCEPTION,
    /** Days since booking without an open promise to pay. */
    NO_COMMITMENT_BY_DAY,
    /** Number of broken promises on the account (BRCLXN.055). */
    BROKEN_PROMISES_COUNT,
    /** Days an installment of the account has been overdue (BRCLXN.053). */
    INSTALLMENT_OVERDUE_DAYS,
    /** Outstanding premium at or above an amount. */
    AMOUNT_OVER
  }

  /** Who receives an escalation. */
  public enum TargetLevel {
    /** The team lead (stage WITH_TL). */
    TL,
    /** The unit head (stage WITH_UH). */
    UH,
    /** The section head (stage WITH_UH). */
    SECTION_HEAD,
    /** A designated user (stage WITH_TL, assigned to the user). */
    USER;

    /**
     * Whether the case goes to the unit / section head stage.
     *
     * @return true for UH and SECTION_HEAD
     */
    public boolean isHead() {
      return this == UH || this == SECTION_HEAD;
    }
  }

  /** How an escalation was raised. */
  public enum Kind {
    /** By the job CLX_ESCALATION or a broken promise, from a rule. */
    AUTO,
    /** By a user, for one or several invoices (BRCLXN.050). */
    MANUAL
  }

  /** Stage of an escalation, mirrored from the workflow CLX_ESCALATION. */
  public enum Stage {
    /** Just raised. */
    RAISED,
    /** With the team lead. */
    WITH_TL,
    /** With the unit / section head. */
    WITH_UH,
    /** Acknowledged, being acted on. */
    IN_ACTION,
    /** Returned to the handler with an instruction. */
    RETURNED,
    /** Resolved or closed because the account was collected. */
    RESOLVED;

    /**
     * Whether the escalation is still open.
     *
     * @return false only when resolved
     */
    public boolean isOpen() {
      return this != RESOLVED;
    }
  }
}
