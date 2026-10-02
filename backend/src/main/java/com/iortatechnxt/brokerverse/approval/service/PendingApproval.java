package com.iortatechnxt.brokerverse.approval.service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One item waiting for the viewer's approval, as contributed by a {@link PendingApprovalSource}.
 *
 * @param module owning module code, e.g. "GL", "MASTER_DATA", "UNDERWRITING"
 * @param type item type, e.g. "Journal", "Branch", "Policy endorsement"
 * @param reference business reference (document number or code)
 * @param description short description
 * @param amount amount to approve (null for non-monetary items)
 * @param currency currency of the amount (null for non-monetary items)
 * @param submittedBy maker
 * @param submittedAt submission (or last modification) time
 * @param companyId company of the item (null when not company specific)
 * @param link frontend route that opens the item, e.g. "/gl/journals/42" (null when none)
 */
public record PendingApproval(
    String module,
    String type,
    String reference,
    String description,
    BigDecimal amount,
    String currency,
    String submittedBy,
    Instant submittedAt,
    Long companyId,
    String link) {}
