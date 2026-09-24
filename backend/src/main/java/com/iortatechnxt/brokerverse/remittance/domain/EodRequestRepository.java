package com.iortatechnxt.brokerverse.remittance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** End-of-day extraction requests (RMTID.005). */
public interface EodRequestRepository extends JpaRepository<EodRequest, Long> {

  /**
   * The request of an invoice on a day.
   *
   * @param invoiceNo invoice
   * @param requestedOn day
   * @return request
   */
  Optional<EodRequest> findByInvoiceNoAndRequestedOn(String invoiceNo, LocalDate requestedOn);

  /**
   * Requests not yet processed.
   *
   * @param companyId company
   * @return requests, oldest first
   */
  List<EodRequest> findByCompanyIdAndRunNoIsNullOrderByIdAsc(Long companyId);

  /**
   * Requests of a company, newest first.
   *
   * @param companyId company
   * @return requests
   */
  List<EodRequest> findTop100ByCompanyIdOrderByIdDesc(Long companyId);
}
