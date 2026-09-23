package com.iortatechnxt.finverse.budget.domain;

/**
 * Budget version lifecycle.
 *
 * <pre>
 * DRAFT ──submit──► SUBMITTED ──approve──► APPROVED ──(newer version approved)──► SUPERSEDED
 *   ▲                   │
 *   └──── edit ◄── REJECTED ◄──reject──┘
 * </pre>
 *
 * Only the latest APPROVED version of a company and fiscal year is used for monitoring.
 */
public enum BudgetStatus {
  DRAFT,
  SUBMITTED,
  APPROVED,
  REJECTED,
  SUPERSEDED
}
