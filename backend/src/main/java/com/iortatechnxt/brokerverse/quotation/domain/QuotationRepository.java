package com.iortatechnxt.brokerverse.quotation.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Package quotations (bean name distinct from the underwriting quotations). */
@Repository("brokingQuotationRepository")
public interface QuotationRepository
    extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {

  /**
   * A quotation by ARN.
   *
   * @param arn Account Reference Number
   * @return quotation
   */
  Optional<Quotation> findByArn(String arn);

  /**
   * A quotation by number.
   *
   * @param companyId company
   * @param quotationNo quotation number
   * @return quotation
   */
  Optional<Quotation> findByCompanyIdAndQuotationNo(Long companyId, String quotationNo);

  /**
   * Quotations of a client, newest first.
   *
   * @param clientId client
   * @return quotations
   */
  List<Quotation> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
