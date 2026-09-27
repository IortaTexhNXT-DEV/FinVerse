package com.iortatechnxt.brokerverse.receivables.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link BankMatch}. */
public interface BankMatchRepository extends JpaRepository<BankMatch, Long> {

  /**
   * Lists the matches of a bank account, newest first.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @return matches
   */
  List<BankMatch> findTop200ByCompanyIdAndBankAccountCodeOrderByIdDesc(
      Long companyId, String bankAccountCode);
}
