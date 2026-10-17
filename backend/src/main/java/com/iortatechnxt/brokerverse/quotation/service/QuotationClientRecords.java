package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.catalog.service.CatalogNames;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.RecordDescriptions;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The quotations of a client for the client 360 view (BRNB.099). */
@Component
public class QuotationClientRecords implements ClientRecordsProvider {

  private final QuotationRepository quotations;
  private final CatalogNames names;

  /**
   * Creates the provider.
   *
   * @param quotations quotations
   * @param names product names
   */
  public QuotationClientRecords(QuotationRepository quotations, CatalogNames names) {
    this.quotations = quotations;
    this.names = names;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return quotations.findByClientIdOrderByCreatedAtDesc(clientId).stream()
        .map(this::record)
        .toList();
  }

  private ClientRecord record(Quotation q) {
    return new ClientRecord(
        "Quotation",
        q.getQuotationNo(),
        RecordDescriptions.quotation(
            q.getArn(),
            names.productName(q.getProductCode()),
            q.getCurrency(),
            q.getGrossPremium()),
        q.getStatus().name(),
        BusinessClock.dateOf(q.getCreatedAt()),
        "/quotations/" + q.getId());
  }
}
