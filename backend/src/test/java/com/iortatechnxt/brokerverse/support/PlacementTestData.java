package com.iortatechnxt.brokerverse.support;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.placement.domain.PlacementSlip;
import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Accounts at the placement and issuance stages for tests: each call creates a new account with
 * unique risk identifiers and drives it through the lifecycle as the SIT/UAT users would.
 */
@Component
public class PlacementTestData {

  public static final String CBG_CLIENT = "CL-2026-900001";
  public static final String CORPORATE_CLIENT = "CL-2026-900003";

  private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000L);

  private final AccountService accounts;
  private final AccountLifecycleService lifecycle;
  private final PlacementSlipService slips;
  private final ClientService clients;
  private final DocumentService documents;
  private final AsUser as;
  private final TestData data;

  PlacementTestData(
      AccountService accounts,
      AccountLifecycleService lifecycle,
      PlacementSlipService slips,
      ClientService clients,
      DocumentService documents,
      AsUser as,
      TestData data) {
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.slips = slips;
    this.clients = clients;
    this.documents = documents;
    this.as = as;
    this.data = data;
  }

  public Long company() {
    return data.company().getId();
  }

  /** A unique alphanumeric token. */
  public static String token() {
    return Long.toString(SEQ.incrementAndGet() * 7919 + System.nanoTime() % 1000, 36).toUpperCase();
  }

  /** A CBG Fire account (PAR01) mortgaged to BDO Home Loans with a PN and loan number. */
  public Spec fire() {
    String t = token();
    return new Spec(
        CBG_CLIENT,
        "PAR01",
        "CBG",
        new RiskItemData(
            null,
            null,
            null,
            null,
            null,
            null,
            new Location(
                t + " Acacia Street",
                "Makati",
                "Metro Manila",
                "DWELLING",
                "CLASS_1",
                List.of(new InsuredItem("Building", new BigDecimal("2000000")))),
            null),
        new Mortgage("BDO_HOME_LOANS", "HL-" + t, List.of("PN-" + t)));
  }

  /** A CBG Motor account (MTR10) without mortgage. */
  public Spec motor() {
    String t = token();
    return new Spec(
        CBG_CLIENT,
        "MTR10",
        "CBG",
        new RiskItemData(
            null,
            new BigDecimal("900000"),
            null,
            new BigDecimal("100000"),
            new BigDecimal("100000"),
            new Vehicle(
                "P" + t, null, "E" + t, "C" + t, "Toyota", "Vios", 2025, "SEDAN", "White", 5),
            null,
            null),
        Mortgage.NONE);
  }

  /** An Other Lines account (CGL01, corporate banking): client confirmation. */
  public Spec liability() {
    return new Spec(
        CORPORATE_CLIENT,
        "CGL01",
        "CORBANK",
        RiskItemData.generic("Warehouse " + token(), new BigDecimal("5000000"), null),
        Mortgage.NONE);
  }

  /** Creates, submits and validates an account: it awaits payment. */
  public Account awaitingPayment(Spec spec) {
    Long clientId = clients.requireByCode(company(), spec.client()).getId();
    AccountDraft draft =
        new AccountDraft(
            clientId,
            spec.product(),
            spec.segment(),
            "EMAIL",
            "INS-MGIC",
            "MKT",
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2027, 10, 1),
            false,
            1,
            "PHP",
            PaymentArrangement.VIA_BDOI,
            spec.mortgage(),
            null,
            List.of(spec.item()),
            null,
            null,
            null);
    Account account = as.run("ao", () -> accounts.createDraft(NewAccount.direct(company(), draft)));
    attach(account.getId(), "IDF", "ao");
    as.run("ao", () -> accounts.submit(account.getId(), null));
    return as.run("proc", () -> accounts.validate(account.getId(), null));
  }

  /** An account ready for placement (payment confirmed). */
  public String ready(Spec spec) {
    String arn = awaitingPayment(spec).getArn();
    as.run("proc", () -> lifecycle.markPaymentConfirmed(arn, "Test payment"));
    return arn;
  }

  /** An account placed with its insurer (slip generated and sent). */
  public String placed(Spec spec) {
    String arn = ready(spec);
    as.run(
        "proc",
        () -> {
          PlacementSlip slip = slips.generate(company(), List.of(arn)).get(0);
          return slips.send(slip.getId(), slips.draft(slip.getId()));
        });
    return arn;
  }

  /** Attaches a small PDF of a document type to an account. */
  public void attach(Long accountId, String documentType, String user) {
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

  /**
   * What to create.
   *
   * @param client client code
   * @param product product
   * @param segment market segment
   * @param item risk item
   * @param mortgage mortgage
   */
  public record Spec(
      String client, String product, String segment, RiskItemData item, Mortgage mortgage) {}
}
