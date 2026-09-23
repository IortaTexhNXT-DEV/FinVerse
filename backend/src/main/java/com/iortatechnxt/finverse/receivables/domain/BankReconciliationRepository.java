package com.iortatechnxt.finverse.receivables.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link BankReconciliation}. */
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {

  /**
   * Finds the reconciliation of a bank account as of a date.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param asOfDate date
   * @return reconciliation
   */
  Optional<BankReconciliation> findByCompanyIdAndBankAccountCodeAndAsOfDate(
      Long companyId, String bankAccountCode, LocalDate asOfDate);

  /**
   * Lists the reconciliations of a company, latest first.
   *
   * @param companyId company
   * @return reconciliations
   */
  List<BankReconciliation> findByCompanyIdOrderByAsOfDateDescBankAccountCodeAsc(Long companyId);

  /**
   * Latest finalized reconciliation of a bank account.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param status FINALIZED
   * @return reconciliation
   */
  Optional<BankReconciliation> findFirstByCompanyIdAndBankAccountCodeAndStatusOrderByAsOfDateDesc(
      Long companyId, String bankAccountCode, ReconciliationStatus status);
}
