package com.iortatechnxt.brokerverse.crm;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientOnboardingService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.KycDocumentService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Unique clients for crm tests (tests share one database and run in any order). */
@Component
public class CrmFixtures {

  static final byte[] PDF =
      "%PDF-1.4\n% test KYC document\n%%EOF".getBytes(StandardCharsets.US_ASCII);

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private final ClientService clients;
  private final ClientOnboardingService onboarding;
  private final KycDocumentService kyc;
  private final AsUser as;
  private final TestData data;

  CrmFixtures(
      ClientService clients,
      ClientOnboardingService onboarding,
      KycDocumentService kyc,
      AsUser as,
      TestData data) {
    this.clients = clients;
    this.onboarding = onboarding;
    this.kyc = kyc;
    this.as = as;
    this.data = data;
  }

  Long company() {
    return data.company().getId();
  }

  static String word(int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
    }
    return sb.charAt(0) + sb.substring(1).toLowerCase(Locale.ROOT);
  }

  static String digits(int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(RANDOM.nextInt(10));
    }
    return sb.toString();
  }

  static String tin() {
    String d = digits(9);
    return "9" + d.substring(0, 2) + "-" + d.substring(2, 5) + "-" + d.substring(5, 8) + "-000";
  }

  /** A complete individual with unique identifiers. */
  static ClientDetails person() {
    return new ClientDetails(
        ClientType.INDIVIDUAL,
        new PersonName("Test" + word(6), word(7), null, null, null),
        LocalDate.of(1970 + RANDOM.nextInt(30), 1 + RANDOM.nextInt(12), 1 + RANDOM.nextInt(28)),
        new Identity(tin(), "PASSPORT", "T" + digits(9)),
        new Contact(
            word(8).toLowerCase(Locale.ROOT) + "@test-client.ph",
            "0999" + digits(7),
            null,
            "1 Test Street",
            "Makati",
            "Metro Manila",
            "1200"),
        "CBG",
        false,
        null);
  }

  /** A complete corporate client with unique identifiers. */
  static ClientDetails corporate() {
    return new ClientDetails(
        ClientType.CORPORATE,
        new PersonName(null, null, null, null, word(10) + " Holdings Corp."),
        null,
        new Identity(tin(), "SEC_REG", "CS" + digits(9)),
        new Contact(
            word(8).toLowerCase(Locale.ROOT) + "@test-corp.ph",
            null,
            null,
            "2 Test Avenue",
            "Pasig",
            "Metro Manila",
            "1600"),
        "CORBANK",
        true,
        "CIF-" + digits(6));
  }

  Client prospect(ClientDetails details) {
    return as.run(
        "ao",
        () ->
            clients.create(
                company(),
                details,
                new ClientProfile("FILIPINO", "SINGLE", "Tester", "SALARY", "STANDARD")));
  }

  void uploadChecklist(Client c) {
    boolean corporate = c.getClientType() == ClientType.CORPORATE;
    String[] types =
        corporate
            ? new String[] {"KYC_FORM", "SEC_CERTIFICATE", "GIS", "SECRETARY_CERTIFICATE"}
            : new String[] {"KYC_FORM", "VALID_ID"};
    for (String type : types) {
      as.run("ao", () -> kyc.upload(c.getId(), type, type.toLowerCase(Locale.ROOT) + ".pdf", PDF));
    }
  }

  /** A client taken through the whole onboarding: submitted by ao, verified by mkttl, confirmed. */
  Client confirmed(ClientDetails details) {
    Client p = prospect(details);
    uploadChecklist(p);
    as.run("ao", () -> onboarding.submitKyc(p.getId(), "documents complete"));
    as.run("mkttl", () -> onboarding.verifyKyc(p.getId(), "checked originals"));
    return as.run("ao", () -> onboarding.confirm(p.getId(), null));
  }
}
