package com.iortatechnxt.brokerverse.receivables.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link BankMatchBook}. */
public interface BankMatchBookRepository extends JpaRepository<BankMatchBook, Long> {

  /**
   * Lists the ledger entries of a match.
   *
   * @param matchId match
   * @return links
   */
  List<BankMatchBook> findByMatchId(Long matchId);

  /**
   * Lists the ledger entries of several matches.
   *
   * @param matchIds matches
   * @return links
   */
  List<BankMatchBook> findByMatchIdIn(Collection<Long> matchIds);

  /**
   * Checks whether any of the entries is already reconciled.
   *
   * @param ledgerEntryIds ledger entries
   * @return true when at least one is reconciled
   */
  boolean existsByLedgerEntryIdIn(Collection<Long> ledgerEntryIds);
}
