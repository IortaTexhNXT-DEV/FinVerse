package com.iortatechnxt.brokerverse.cashiering.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Versions of the AR and OR forms (FRS.CSH.02.06.01). */
public interface ReceiptFormRepository extends JpaRepository<ReceiptForm, Long> {

  /**
   * The approved version in use on a date: the latest effective on or before it.
   *
   * @param companyId company
   * @param formKind AR or OR
   * @param status APPROVED
   * @param date print date
   * @return version
   */
  Optional<ReceiptForm>
      findFirstByCompanyIdAndFormKindAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescVersionNoDesc(
          Long companyId, String formKind, String status, LocalDate date);

  /**
   * The versions of a company.
   *
   * @param companyId company
   * @return versions, by kind and latest first
   */
  List<ReceiptForm> findByCompanyIdOrderByFormKindAscVersionNoDesc(Long companyId);

  /**
   * The last version of a form.
   *
   * @param companyId company
   * @param formKind AR or OR
   * @return version
   */
  Optional<ReceiptForm> findFirstByCompanyIdAndFormKindOrderByVersionNoDesc(
      Long companyId, String formKind);
}
