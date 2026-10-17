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

  /**
   * The accounts that renew a policy (shared work item BT0; Renewal duplicate check and booking of
   * the renewal, BRRN.005/040).
   *
   * @param renewalOfRef expiring ARN, SBM number or legacy reference
   * @return accounts
   */
  List<Account> findByClassificationRenewalOfRef(String renewalOfRef);

  /**
   * Accounts of any status found by account number (Customer Servicing Facility, BRCSF-003): the
   * ARN itself, the ARN without its two-digit suffix, or a policy number.
   *
   * @param companyId company
   * @param reference ARN (with or without suffix) or policy number
   * @return accounts
   */
  @Query(
      "select distinct a from Account a left join a.policyNumbers p where a.companyId = :companyId"
          + " and (a.arn = :reference or a.arn like concat(:reference, '-%') or p = :reference)")
  List<Account> findByAccountNumber(
      @Param("companyId") Long companyId, @Param("reference") String reference);

  /**
   * Accounts of a loan application number (Customer Servicing Facility, BRCSF-003).
   *
   * @param companyId company
   * @param loanApplicationNo loan application number, upper case
   * @return accounts
   */
  @Query(
      "select a from Account a where a.companyId = :companyId"
          + " and upper(a.loanApplicationNo) = :loanApplicationNo")
  List<Account> findByLoanApplication(
      @Param("companyId") Long companyId, @Param("loanApplicationNo") String loanApplicationNo);
}
