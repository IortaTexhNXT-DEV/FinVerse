package com.iortatechnxt.brokerverse.quotation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Quotation requests. */
public interface QuotationRequestRepository
    extends JpaRepository<QuotationRequest, Long>, JpaSpecificationExecutor<QuotationRequest> {

  /**
   * Whether a source system already delivered a request.
   *
   * @param channel source channel
   * @param externalRef reference in the source system
   * @return true when known
   */
  boolean existsByChannelAndExternalRef(String channel, String externalRef);
}
