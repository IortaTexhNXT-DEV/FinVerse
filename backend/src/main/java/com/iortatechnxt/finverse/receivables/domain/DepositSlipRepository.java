package com.iortatechnxt.finverse.receivables.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link DepositSlip}. */
public interface DepositSlipRepository extends JpaRepository<DepositSlip, Long> {

  /**
   * Lists the slips of a company, newest first.
   *
   * @param companyId company
   * @return slips
   */
  List<DepositSlip> findByCompanyIdOrderBySlipDateDescIdDesc(Long companyId);

  /**
   * Finds a slip by number.
   *
   * @param companyId company
   * @param slipNo slip number
   * @return slip
   */
  Optional<DepositSlip> findByCompanyIdAndSlipNo(Long companyId, String slipNo);

  /**
   * Lists the slips of a bank account in a status.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param status status
   * @return slips
   */
  List<DepositSlip> findByCompanyIdAndBankAccountCodeAndStatus(
      Long companyId, String bankAccountCode, DepositSlipStatus status);
}
