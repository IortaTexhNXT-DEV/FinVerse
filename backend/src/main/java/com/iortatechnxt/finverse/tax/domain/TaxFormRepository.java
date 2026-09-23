package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tax forms of the filing calendar. */
public interface TaxFormRepository extends JpaRepository<TaxForm, Long> {

  /**
   * Forms of a company.
   *
   * @param companyId company
   * @return forms ordered by code
   */
  List<TaxForm> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Forms of a company in a status.
   *
   * @param companyId company
   * @param status record status
   * @return forms ordered by code
   */
  List<TaxForm> findByCompanyIdAndRecordStatusOrderByCode(Long companyId, RecordStatus status);

  /**
   * Finds a form.
   *
   * @param companyId company
   * @param code form code
   * @return form
   */
  Optional<TaxForm> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Whether a form exists.
   *
   * @param companyId company
   * @param code form code
   * @return true when present
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);
}
