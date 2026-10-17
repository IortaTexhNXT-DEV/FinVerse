package com.iortatechnxt.brokerverse.crm.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client payout accounts (MKT 2.25.0). */
public interface ClientPayoutAccountRepository extends JpaRepository<ClientPayoutAccount, Long> {

  /**
   * Accounts of a client, live first then newest.
   *
   * @param clientId client
   * @return accounts
   */
  List<ClientPayoutAccount> findByClientIdOrderByActiveDescIdDesc(Long clientId);

  /**
   * The live row of an account number (MKT 2.25.1).
   *
   * @param clientId client
   * @param accountNo account number
   * @return row
   */
  Optional<ClientPayoutAccount> findByClientIdAndAccountNoAndActiveTrue(
      Long clientId, String accountNo);

  /**
   * The live check payee of a name (MKT 2.25.1).
   *
   * @param clientId client
   * @param mode mode (CHECK)
   * @param payeeKey normalised payee name
   * @return row
   */
  Optional<ClientPayoutAccount> findByClientIdAndModeAndPayeeKeyAndActiveTrue(
      Long clientId, PayoutMode mode, String payeeKey);
}
