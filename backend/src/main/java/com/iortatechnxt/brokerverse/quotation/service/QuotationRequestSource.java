package com.iortatechnxt.brokerverse.quotation.service;

import java.util.List;

/**
 * Port for source systems that send quotation requests (BRNB.023/028, HLS; parked Q11). An
 * implementation (SFTP file reader, REST client...) is added once BDOI specifies the interface; it
 * returns the requests received since the last call and the {@code QUOTATION_REQUEST_INTAKE} job
 * stores them in the request staging table ({@code quo_request}), skipping references already
 * received. Until then requests are captured manually or uploaded with the {@code
 * QUOTATION_REQUEST} bulk handler.
 */
public interface QuotationRequestSource {

  /**
   * Company the requests belong to.
   *
   * @return company id
   */
  Long companyId();

  /**
   * Source channel code (list SOURCE_CHANNEL), e.g. HLS.
   *
   * @return channel
   */
  String channel();

  /**
   * Requests received since the last call.
   *
   * @return requests; each should carry its external reference
   */
  List<IncomingQuotationRequest> fetch();
}
