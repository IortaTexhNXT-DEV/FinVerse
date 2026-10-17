package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import java.util.List;

/**
 * The data of a Package Request Form as entered (BRPM.008).
 *
 * @param type request type
 * @param scope generic programme or client-specific package
 * @param title package / programme name
 * @param clientId client of a client-specific package (CRM), null for a generic programme
 * @param lineCode product line
 * @param coverTypeCode cover type or subtype
 * @param productCode target product (every type but NEW)
 * @param baseVersionNo version the request starts from; null for the current one
 * @param marketSegments market segments (list MARKET_SEGMENT)
 * @param reason reason (list PKG_REQUEST_REASON)
 * @param reasonNote comment on the reason
 * @param negotiationRequired whether insurers are approached (RENEW / UPDATE / REACTIVATE; null for
 *     the type's default)
 * @param terms requested terms, coverages and target insurers
 */
public record RequestDraft(
    RequestType type,
    RequestScope scope,
    String title,
    Long clientId,
    String lineCode,
    String coverTypeCode,
    String productCode,
    Integer baseVersionNo,
    List<String> marketSegments,
    String reason,
    String reasonNote,
    Boolean negotiationRequired,
    PackageTerms terms) {

  /** Defensive copy. */
  public RequestDraft {
    marketSegments = marketSegments == null ? List.of() : List.copyOf(marketSegments);
  }
}
