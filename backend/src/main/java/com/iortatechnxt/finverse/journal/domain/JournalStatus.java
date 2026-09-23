package com.iortatechnxt.finverse.journal.domain;

/**
 * Journal batch lifecycle.
 *
 * <pre>
 * DRAFT ──submit──► PENDING_APPROVAL ──approve──► POSTED ──reverse(posted)──► REVERSED
 *   ▲  │                  │
 *   │  └─cancel─► CANCELLED└─reject──► REJECTED ──edit/resubmit──┘
 * </pre>
 *
 * Approval posts immediately (on-line transaction posting), so there is no separate APPROVED state;
 * a failed posting rolls back and the batch stays PENDING_APPROVAL.
 */
public enum JournalStatus {
  DRAFT,
  PENDING_APPROVAL,
  POSTED,
  REJECTED,
  CANCELLED,
  REVERSED
}
