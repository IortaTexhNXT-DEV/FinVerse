package com.iortatechnxt.brokerverse.account.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Accounts. */
public interface AccountRepository
    extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

  /**
   * An account by Account Reference Number.
   *
   * @param arn ARN
   * @return account
   */
  Optional<Account> findByArn(String arn);

  /**
   * Whether an ARN is taken.
   *
   * @param arn ARN
   * @return true when an account has it
   */
  boolean existsByArn(String arn);

  /**
   * Accounts of a client, newest first (client 360 view).
   *
   * @param clientId client
   * @return accounts
   */
  List<Account> findByClientIdOrderByCreatedAtDesc(Long clientId);

  /**
   * Accounts in given statuses whose ARN, policy number or promissory note number is a reference.
   *
   * @param companyId company
   * @param statuses statuses
   * @param reference ARN, policy number or PN number
   * @return accounts
   */
  @Query(
      "select distinct a from Account a left join a.policyNumbers p left join a.pnNumbers n"
          + " where a.companyId = :companyId and a.status in :statuses"
          + " and (a.arn = :reference or p = :reference or n = :reference)")
  List<Account> findByReference(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<AccountStatus> statuses,
      @Param("reference") String reference);
}
