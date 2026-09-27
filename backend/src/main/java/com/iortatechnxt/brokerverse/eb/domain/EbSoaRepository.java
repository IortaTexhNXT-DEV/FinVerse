package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Insurer SOAs. */
public interface EbSoaRepository
    extends JpaRepository<EbSoa, Long>, JpaSpecificationExecutor<EbSoa> {

  /**
   * An SOA of a company.
   *
   * @param id SOA
   * @param companyId company
   * @return SOA
   */
  Optional<EbSoa> findByIdAndCompanyId(Long id, Long companyId);

  /**
   * SOAs of a programme.
   *
   * @param programmeId programme
   * @return SOAs, latest first
   */
  List<EbSoa> findByProgrammeIdOrderByIdDesc(Long programmeId);

  /**
   * SOAs in a status (validation late alert).
   *
   * @param status status
   * @return SOAs
   */
  List<EbSoa> findByStatusOrderByReceivedOnAscIdAsc(EbSoa.Status status);

  /**
   * SOAs billing an invoice.
   *
   * @param invoiceNo invoice
   * @return SOAs
   */
  @Query("select s from EbSoa s join s.invoices i where i.invoiceNo = :invoiceNo")
  List<EbSoa> findByInvoiceNo(@Param("invoiceNo") String invoiceNo);
}
