package com.iortatechnxt.brokerverse.collections.installment.domain;

/** Enumerations of the installment plans (BRCLXN.053/058, COLLECTIONS_DESIGN 4.2). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class PlanEnums {

  private PlanEnums() {}

  /** Where the installments of a plan come from (CQ15). */
  public enum PlanSource {
    /** The policy-year invoices of a multi-year account, split by the billing frequency. */
    POLICY_YEARS,
    /** The outstanding premium of one invoice, split by the billing frequency. */
    GENERATED,
    /** Installments entered by the collector for one invoice. */
    MANUAL
  }

  /** Status of a plan. */
  public enum PlanStatus {
    /** Followed up: allocation, overdue flags, promises and billing statements. */
    ACTIVE,
    /** Every installment is paid. */
    COMPLETED,
    /** Cancelled by the collector (replaced or no longer relevant). */
    CANCELLED
  }

  /** Status of one installment, from its due date and the payments allocated to it. */
  public enum InstallmentStatus {
    /** Due date not reached, nothing paid. */
    NOT_DUE,
    /** Due today. */
    DUE,
    /** Past its due date and not fully paid (BRCLXN.053 "overdue installments flagged"). */
    OVERDUE,
    /** Partly paid, due date not passed. */
    PARTIAL,
    /** Fully paid. */
    PAID
  }
}
