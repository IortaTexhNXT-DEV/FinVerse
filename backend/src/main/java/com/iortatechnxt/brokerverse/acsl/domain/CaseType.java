package com.iortatechnxt.brokerverse.acsl.domain;

/** Type of an ACSL case (LOV {@code ACSL_CASE_TYPE}, ACSL 2.5.x-2.6.x). */
public enum CaseType {
  /** Account investigation (ACSL 2.5.0). */
  INVESTIGATION,
  /** Account analysis request, e.g. the refund of a cancelled account (ACSL 2.5.5, MKT 1.11.0). */
  ANALYSIS_REQUEST,
  /** AR refund payment application (ACSL 2.6.0). */
  REFUND_APPLICATION,
  /** Sub-ledger payment reversal (ACSL 2.6.1). */
  PAYMENT_REVERSAL
}
