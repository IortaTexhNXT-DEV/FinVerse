package com.iortatechnxt.brokerverse.account.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Legacy headers of imported accounts (V823). */
public interface AccountLegacyHeaderRepository extends JpaRepository<AccountLegacyHeader, Long> {

  /**
   * The header of an account.
   *
   * @param accountId account
   * @return header
   */
  Optional<AccountLegacyHeader> findByAccountId(Long accountId);

  /**
   * Headers of accounts.
   *
   * @param accountIds accounts
   * @return headers
   */
  List<AccountLegacyHeader> findByAccountIdIn(Collection<Long> accountIds);

  /**
   * The live header of a legacy policy reference.
   *
   * @param companyId company
   * @param legacyRef legacy policy reference
   * @return header
   */
  Optional<AccountLegacyHeader> findByCompanyIdAndLegacyRefAndRolledBackAtIsNull(
      Long companyId, String legacyRef);

  /**
   * Live headers of a policy or cover number.
   *
   * @param companyId company
   * @param policyNo policy number
   * @param coverNo cover number
   * @return headers
   */
  List<AccountLegacyHeader> findByCompanyIdAndPolicyNoOrCompanyIdAndCoverNo(
      Long companyId, String policyNo, Long companyId2, String coverNo);
}
