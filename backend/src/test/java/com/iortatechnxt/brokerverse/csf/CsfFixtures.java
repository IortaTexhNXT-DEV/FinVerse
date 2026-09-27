package com.iortatechnxt.brokerverse.csf;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.csf.service.VerificationService.CheckAnswer;
import com.iortatechnxt.brokerverse.csf.service.VerificationService.VerifyRequest;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Unique clients, documents and verifications for the Customer Servicing Facility tests. */
@Component
public class CsfFixtures {

  static final String AGENT = "csfagent";
  static final String SUPERVISOR = "csfsup";
  static final String MANAGEMENT = "csfmgmt";

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private final ClientService clients;
  private final AttachmentService attachments;
  private final DocumentService documents;
  private final DocumentComposer composer;
  private final AsUser as;
  private final TestData data;

  CsfFixtures(
      ClientService clients,
      AttachmentService attachments,
      DocumentService documents,
      DocumentComposer composer,
      AsUser as,
      TestData data) {
    this.clients = clients;
    this.attachments = attachments;
    this.documents = documents;
    this.composer = composer;
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

  /**
   * A prospect with unique identifiers and a registered e-mail, or none.
   *
   * @param withEmail registered e-mail
   * @return client
   */
  Client client(boolean withEmail) {
    String d = digits(9);
    ClientDetails details =
        new ClientDetails(
            ClientType.INDIVIDUAL,
            new PersonName("Csf" + word(6), word(7), null, null, null),
            LocalDate.of(1980, 1 + RANDOM.nextInt(12), 1 + RANDOM.nextInt(28)),
            new Identity(
                "9"
                    + d.substring(0, 2)
                    + "-"
                    + d.substring(2, 5)
                    + "-"
                    + d.substring(5, 8)
                    + "-000",
                "PASSPORT",
                "C" + digits(9)),
            new Contact(
                withEmail ? word(8).toLowerCase(Locale.ROOT) + "@csf-client.ph" : null,
                "0998" + digits(7),
                null,
                "7 Servicing Street",
                "Makati",
                "Metro Manila",
                "1200"),
            "CBG",
            false,
            null);
    return as.run(
        "ao",
        () ->
            clients.create(
                company(),
                details,
                new ClientProfile("FILIPINO", "SINGLE", "Tester", "SALARY", "STANDARD")));
  }

  /**
   * A renewal advice PDF stored on a record and linked to the client, as the Renewal module does.
   *
   * @param owner record the advice belongs to
   * @param clientId client
   * @return the document
   */
  Attachment renewalAdvice(AttachmentTarget owner, Long clientId) {
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                "BDO Insure",
                "Renewal Advice",
                "RA-" + digits(6),
                List.of(new DocumentSpec.Text("Renewal", "Your policy expires soon.")),
                List.of(),
                null));
    return as.run(
        "proc",
        () -> {
          Attachment a =
              attachments.upload(
                  owner, "RA_" + digits(4) + ".pdf", pdf, "Renewal advice", "RENEWAL_ADVICE");
          documents.link(
              a.getId(), List.of(new AttachmentTarget("Client", String.valueOf(clientId))));
          return a;
        });
  }

  /**
   * A verification with the given number of matched checks of four.
   *
   * @param matches checks matched
   * @return request
   */
  static VerifyRequest checks(int matches) {
    List<String> codes = List.of("ADDRESS", "CONTACT_NUMBER", "EMAIL", "INSURED_PROPERTY");
    return new VerifyRequest(
        "HOTLINE",
        java.util.stream.IntStream.range(0, codes.size())
            .mapToObj(i -> new CheckAnswer(codes.get(i), i < matches))
            .toList(),
        null);
  }
}
