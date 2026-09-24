package com.iortatechnxt.brokerverse.quotation;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.DraftItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.Terms;
import com.iortatechnxt.brokerverse.quotation.service.QuotationService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Quotation test data: unique vehicles, demo clients, drafts and documents. */
@Component
public class QuotationFixtures {

  private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000L);

  static final LocalDate FROM = LocalDate.of(2026, 11, 1);
  static final LocalDate TO = LocalDate.of(2027, 11, 1);

  private final ClientService clients;
  private final DocumentService documents;
  private final QuotationService quotations;
  private final AsUser as;
  private final TestData data;

  QuotationFixtures(
      ClientService clients,
      DocumentService documents,
      QuotationService quotations,
      AsUser as,
      TestData data) {
    this.clients = clients;
    this.documents = documents;
    this.quotations = quotations;
    this.as = as;
    this.data = data;
  }

  Long company() {
    return data.company().getId();
  }

  /** A unique token for vehicle identifiers. */
  static String token() {
    return Long.toString(SEQ.incrementAndGet() * 7907 + System.nanoTime() % 1000, 36).toUpperCase();
  }

  /** A demo client by code. */
  Client client(String code) {
    return clients.requireByCode(company(), code);
  }

  /** A new prospect. */
  Client prospect() {
    return as.run(
        "ao",
        () ->
            clients.createProspect(
                company(),
                new ClientDetails(
                    ClientType.INDIVIDUAL,
                    new PersonName("Quoteprospect" + token(), "Test", null, null, null),
                    LocalDate.of(1991, 2, 2),
                    null,
                    null,
                    "CBG",
                    false,
                    null)));
  }

  /** A vehicle with unique identifiers. */
  static RiskItemData vehicle(String sumInsured) {
    String id = token();
    return new RiskItemData(
        null,
        new BigDecimal(sumInsured),
        null,
        new BigDecimal("100000"),
        new BigDecimal("100000"),
        new Vehicle("Q" + id, null, "QE" + id, "QC" + id, "Toyota", "Vios", 2025, "SEDAN", null, 5),
        null,
        null);
  }

  /** A motor draft with one item per group given. */
  static QuotationDraft motor(Long clientId, boolean directPayment, DraftItem... items) {
    return new QuotationDraft(
        clientId,
        "MTR10",
        "CBG",
        "EMAIL",
        null,
        null,
        new Terms("INS-MGIC", "MKT", FROM, TO, null, directPayment, null, "Test quotation"),
        List.of(items));
  }

  /** Creates a one-vehicle motor quotation as ao. */
  Quotation create(Client client) {
    return as.run(
        "ao",
        () ->
            quotations.create(
                company(), motor(client.getId(), false, new DraftItem(1, vehicle("1000000")))));
  }

  /** Attaches a document of a type to a record. */
  void attach(String entityType, Long id, String documentType) {
    as.run(
        "ao",
        () ->
            documents.upload(
                new AttachmentTarget(entityType, String.valueOf(id)),
                List.of(
                    new UploadedFile(
                        "acceptance.pdf", "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions(documentType, false, null, null)));
  }
}
