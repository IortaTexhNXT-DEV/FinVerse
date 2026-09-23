package com.iortatechnxt.finverse.tax.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tax remittances. */
public interface TaxRemittanceRepository extends JpaRepository<TaxRemittance, Long> {

  /**
   * Remittance of a return.
   *
   * @param returnId return id
   * @return remittance
   */
  Optional<TaxRemittance> findByReturnId(Long returnId);

  /**
   * Remittances paid in a date range.
   *
   * @param companyId company
   * @param from paid on from
   * @param to paid on to
   * @return remittances ordered by payment date
   */
  List<TaxRemittance> findByCompanyIdAndPaidOnBetweenOrderByPaidOnAscFormCodeAsc(
      Long companyId, LocalDate from, LocalDate to);
}
