package com.iortatechnxt.brokerverse.cashiering.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** ORs of insurer settlements kept until Disbursement approves (FRS.CSH.07.01.01). */
public interface SettlementOrRepository extends JpaRepository<SettlementOr, Long> {

  /**
   * The request of a source transaction.
   *
   * @param sourceModule module
   * @param sourceRef reference
   * @return request
   */
  Optional<SettlementOr> findBySourceModuleAndSourceRef(String sourceModule, String sourceRef);

  /**
   * The requests waiting for a payment request.
   *
   * @param companyId company
   * @param sourceModule module
   * @param awaitRef payment request reference
   * @param status status
   * @return requests
   */
  List<SettlementOr> findByCompanyIdAndSourceModuleAndAwaitRefAndStatusOrderByIdAsc(
      Long companyId, String sourceModule, String awaitRef, String status);

  /**
   * The requests of a company, latest first.
   *
   * @param companyId company
   * @param pageable page
   * @return page
   */
  Page<SettlementOr> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);
}
