package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The quotations of a client for the client 360 view (BRNB.099). */
@Component
public class QuotationClientRecords implements ClientRecordsProvider {

  private final QuotationRepository quotations;

  /**
   * Creates the provider.
   *
   * @param quotations quotations
   */
  public QuotationClientRecords(QuotationRepository quotations) {
    this.quotations = quotations;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return quotations.findByClientIdOrderByCreatedAtDesc(clientId).stream()
        .map(QuotationClientRecords::record)
        .toList();
  }

  private static ClientRecord record(Quotation q) {
    String premium =
        q.getGrossPremium() == null ? "not rated" : q.getGrossPremium().toPlainString();
    return new ClientRecord(
        "Quotation",
        q.getQuotationNo(),
        q.getArn() + " - " + q.getProductCode() + " - gross premium " + premium,
        q.getStatus().name(),
        LocalDate.ofInstant(q.getCreatedAt(), ZoneOffset.UTC),
        "/quotations/" + q.getId());
  }
}
