package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerContext;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * The legacy facts of an unapplied payment carried from legacy at cut-over (DATA_MIGRATION_DESIGN
 * 14.3): source system, legacy reference and loading batch, the ledger context (LEGACY: the money
 * is on the legacy unapplied collections account), the acknowledgement receipt issued in legacy and
 * the references the automatch uses.
 *
 * @param sourceSystem legacy source system, null for BIBS items
 * @param legacyRef legacy UPP reference
 * @param migrationBatch loading batch
 * @param ledgerContext NEW or LEGACY
 * @param legacyArNo acknowledgement receipt of legacy
 * @param legacyArDate its date
 * @param matchRefs invoice, cover, PN and bank references, comma separated
 */
@Embeddable
public record UnappliedLegacy(
    @Column(name = "source_system", length = 10) String sourceSystem,
    @Column(name = "legacy_ref", length = 80) String legacyRef,
    @Column(name = "migration_batch", length = 20) String migrationBatch,
    @Enumerated(EnumType.STRING) @Column(name = "ledger_context", nullable = false, length = 10)
        LedgerContext ledgerContext,
    @Column(name = "legacy_ar_no", length = 40) String legacyArNo,
    @Column(name = "legacy_ar_date") LocalDate legacyArDate,
    @Column(name = "match_refs", length = 500) String matchRefs) {

  /** An item of BIBS. */
  public static final UnappliedLegacy NONE =
      new UnappliedLegacy(null, null, null, LedgerContext.NEW, null, null, null);

  /** A null context reads as NEW. */
  public UnappliedLegacy {
    ledgerContext = ledgerContext == null ? LedgerContext.NEW : ledgerContext;
  }

  /**
   * The automatch references.
   *
   * @return references, empty when none
   */
  public List<String> references() {
    return matchRefs == null || matchRefs.isBlank()
        ? List.of()
        : Arrays.stream(matchRefs.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
  }
}
