package com.iortatechnxt.brokerverse.booking;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Booking test data: accounts brought to POLICY_ISSUED through the real account services (direct
 * booking with an e-policy), for confirmed demo clients that have a sub-ledger party (V981).
 */
@Component
public class BookingFixtures {

  /** A booking date in an open period and in the past. */
  public static final LocalDate BOOKED_ON = LocalDate.of(2026, 9, 15);

  /** Start of the test accounts' cover. */
  public static final LocalDate FROM = LocalDate.of(2026, 10, 1);

  /** End of the test accounts' cover. */
  public static final LocalDate TO = LocalDate.of(2027, 10, 1);

  /** Demo client with a party (CBG). */
  public static final String CLIENT = "CL-2026-000001";

  private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000L);

  private final AccountService accounts;
  private final AccountLifecycleService lifecycle;
  private final AccountQueryService queries;
  private final ClientService clients;
  private final DocumentService documents;
  private final AsUser as;
  private final TestData data;

  BookingFixtures(
      AccountService accounts,
      AccountLifecycleService lifecycle,
      AccountQueryService queries,
      ClientService clients,
      DocumentService documents,
      AsUser as,
      TestData data) {
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.queries = queries;
    this.clients = clients;
    this.documents = documents;
    this.as = as;
    this.data = data;
  }

  /** The demo company. */
  public Long company() {
    return data.company().getId();
  }

  /** A unique token. */
  public static String token() {
    return Long.toString(SEQ.incrementAndGet() * 7919 + System.nanoTime() % 1000, 36).toUpperCase();
  }

  /** An issued MTR10 CBG motor account paid via BDOI. */
  public Account motor() {
    return issued(spec("MTR10", "CBG", PaymentArrangement.VIA_BDOI));
  }

  /** An issued MTR10 CBG motor account paid directly to the insurer (BRNB.114). */
  public Account directPaymentMotor() {
    return issued(spec("MTR10", "CBG", PaymentArrangement.DIRECT_TO_INSURER));
  }

  /** Account specification with the defaults. */
  public static Spec spec(String product, String segment, PaymentArrangement arrangement) {
    return new Spec(product, segment, arrangement, FROM, TO, 1);
  }

  /** An issued multi-year PAR01 property account starting in the past (BRNB.112). */
  public Account multiYear(LocalDate from, int years) {
    return issued(
        new Spec("PAR01", "CBG", PaymentArrangement.VIA_BDOI, from, from.plusYears(years), years));
  }

  /** Brings an account to POLICY_ISSUED by direct booking (BRNB.111) with its policy numbers. */
  public Account issued(Spec spec) {
    Long clientId = clients.requireByCode(company(), CLIENT).getId();
    String id = token();
    RiskItemData item =
        "PAR01".equals(spec.product()) ? location(id + " Booking Test St") : vehicle(id);
    AccountDraft draft =
        new AccountDraft(
            clientId,
            spec.product(),
            spec.segment(),
            "EMAIL",
            "INS-MGIC",
            "MKT",
            spec.from(),
            spec.to(),
            spec.years() > 1,
            spec.years(),
            "PHP",
            spec.arrangement(),
            Mortgage.NONE,
            null,
            List.of(item),
            null,
            null,
            null);
    Account account = as.run("ao", () -> accounts.createDraft(NewAccount.direct(company(), draft)));
    attach(account.getId(), "IDF");
    attach(account.getId(), "EPOLICY");
    as.run("ao", () -> accounts.submit(account.getId(), null));
    as.run("proc", () -> accounts.directBooking(account.getId(), null));
    List<String> policies = new ArrayList<>();
    for (int y = 1; y <= spec.years(); y++) {
      policies.add("POL-" + id + "-" + y);
    }
    as.run(
        "proc", () -> lifecycle.recordPolicy(account.getArn(), policies, LocalDate.of(2026, 9, 1)));
    return queries.get(account.getId());
  }

  private void attach(Long accountId, String documentType) {
    as.run(
        "ao",
        () ->
            documents.upload(
                new AttachmentTarget("Account", String.valueOf(accountId)),
                List.of(
                    new UploadedFile(
                        "doc.pdf", "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions(documentType, false, null, null)));
  }

  private static RiskItemData vehicle(String id) {
    return new RiskItemData(
        null,
        new BigDecimal("1100000"),
        null,
        new BigDecimal("100000"),
        new BigDecimal("100000"),
        new Vehicle(
            "P" + id, null, "E" + id, "C" + id, "Toyota", "Vios", 2025, "SEDAN", "White", 5),
        null,
        null);
  }

  private static RiskItemData location(String address) {
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
            List.of(new InsuredItem("Building", new BigDecimal("4000000")))),
        null);
  }

  /**
   * What account to issue.
   *
   * @param product product
   * @param segment market segment
   * @param arrangement payment arrangement
   * @param from period start
   * @param to period end
   * @param years term in years
   */
  public record Spec(
      String product,
      String segment,
      PaymentArrangement arrangement,
      LocalDate from,
      LocalDate to,
      int years) {}
}
