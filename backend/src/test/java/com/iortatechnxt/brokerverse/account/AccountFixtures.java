package com.iortatechnxt.brokerverse.account;

import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Account test data: unique vehicles and locations, demo clients, documents. */
@Component
public class AccountFixtures {

  private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000L);

  static final LocalDate FROM = LocalDate.of(2026, 10, 1);
  static final LocalDate TO = LocalDate.of(2027, 10, 1);

  private final ClientService clients;
  private final DocumentService documents;
  private final AsUser as;
  private final TestData data;

  AccountFixtures(ClientService clients, DocumentService documents, AsUser as, TestData data) {
    this.clients = clients;
    this.documents = documents;
    this.as = as;
    this.data = data;
  }

  Long company() {
    return data.company().getId();
  }

  /** A unique alphanumeric token. */
  static String token() {
    return Long.toString(SEQ.incrementAndGet() * 7919 + System.nanoTime() % 1000, 36).toUpperCase();
  }

  /** A confirmed demo client (V983). */
  Client confirmed(String code) {
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
                    new PersonName("Prospect" + token(), "Test", null, null, null),
                    LocalDate.of(1990, 1, 1),
                    null,
                    null,
                    "CBG",
                    false,
                    null)));
  }

  /** A vehicle with unique identifiers. */
  static RiskItemData vehicle(String id, String sumInsured) {
    return new RiskItemData(
        null,
        new BigDecimal(sumInsured),
        null,
        new BigDecimal("100000"),
        new BigDecimal("100000"),
        new Vehicle(
            "P" + id, null, "E" + id, "C" + id, "Toyota", "Vios", 2025, "SEDAN", "White", 5),
        null,
        null);
  }

  /** A location of risk. */
  static RiskItemData location(String address, String item, String sumInsured) {
    return new RiskItemData(
        null,
        null,
        null,
        null,
        null,
        null,
        new Location(
            address,
            "Makati",
            "Metro Manila",
            "DWELLING",
            "CLASS_1",
            List.of(new InsuredItem(item, new BigDecimal(sumInsured)))),
        null);
  }

  /** A draft with the usual defaults. */
  static AccountDraft draft(
      Long clientId, String product, String segment, List<RiskItemData> items) {
    return new AccountDraft(
        clientId,
        product,
        segment,
        "EMAIL",
        "INS-MGIC",
        "MKT",
        FROM,
        TO,
        false,
        1,
        "PHP",
        PaymentArrangement.VIA_BDOI,
        Mortgage.NONE,
        null,
        items,
        null,
        null,
        null);
  }

  /** Attaches a PDF of a document type to an account. */
  void attach(Long accountId, String documentType, String user) {
    as.run(
        user,
        () ->
            documents.upload(
                new AttachmentTarget("Account", String.valueOf(accountId)),
                List.of(
                    new UploadedFile(
                        "doc.pdf", "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions(documentType, false, null, null)));
  }
}
