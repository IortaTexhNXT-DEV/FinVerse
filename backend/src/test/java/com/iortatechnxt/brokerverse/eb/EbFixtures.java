package com.iortatechnxt.brokerverse.eb;

import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbContactRole;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeInput;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Test data of the Employee Benefits waves E1-B and E1-C: programmes of a seed corporate client
 * with unique names, cycles, and the moves of a cycle through stages built by later waves.
 */
@Component
public class EbFixtures {

  /** A seed corporate client (V981). */
  public static final String CLIENT = "CL-2026-000003";

  /** The EB account officer (V1930). */
  public static final String AO = "ebao";

  private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000L);

  private final ProgrammeService programmes;
  private final CycleService cycles;
  private final ClientService clients;
  private final WorkflowService workflow;
  private final AsUser as;
  private final TestData data;

  EbFixtures(
      ProgrammeService programmes,
      CycleService cycles,
      ClientService clients,
      WorkflowService workflow,
      AsUser as,
      TestData data) {
    this.programmes = programmes;
    this.cycles = cycles;
    this.clients = clients;
    this.workflow = workflow;
    this.as = as;
    this.data = data;
  }

  /**
   * A unique token.
   *
   * @return token
   */
  public static String token() {
    return Long.toString(SEQ.incrementAndGet() * 7919 + System.nanoTime() % 1000, 36).toUpperCase();
  }

  /**
   * The seed company.
   *
   * @return company id
   */
  public Long company() {
    return data.company().getId();
  }

  /**
   * The seed corporate client.
   *
   * @return client
   */
  public Client client() {
    return clients.requireByCode(company(), CLIENT);
  }

  /**
   * An HMO line of the incumbent MGIC ending on a date.
   *
   * @param periodTo end of the current period
   * @param arn current ARN, may be null
   * @return line data
   */
  public static EbProgrammeLine.Data hmo(LocalDate periodTo, String arn) {
    return new EbProgrammeLine.Data(
        "HMO",
        null,
        "INS-MGIC",
        "HMO-" + token(),
        arn,
        periodTo == null ? null : periodTo.minusYears(1),
        periodTo,
        120);
  }

  /**
   * An HR contact receiving the renewal advice.
   *
   * @return contact
   */
  public static EbProgrammeContact.Data hr() {
    return new EbProgrammeContact.Data(
        "Hr Head " + token(),
        "hr" + token().toLowerCase() + "@client.example",
        null,
        EbContactRole.HR_HEAD,
        true,
        false);
  }

  /**
   * A programme of the seed client.
   *
   * @param eligible flagged for renewal
   * @param lines benefit lines
   * @return the programme
   */
  public EbProgramme programme(boolean eligible, List<EbProgrammeLine.Data> lines) {
    return programme(eligible, lines, List.of(hr()));
  }

  /**
   * A programme of the seed client.
   *
   * @param eligible flagged for renewal
   * @param lines benefit lines
   * @param contacts contacts
   * @return the programme
   */
  public EbProgramme programme(
      boolean eligible, List<EbProgrammeLine.Data> lines, List<EbProgrammeContact.Data> contacts) {
    Client client = client();
    return as.run(
        AO,
        () ->
            programmes.create(
                company(),
                new ProgrammeInput(
                    client.getId(),
                    "Group benefits " + token(),
                    "BDO",
                    EbFunding.EMPLOYER,
                    null,
                    null,
                    eligible,
                    lines,
                    contacts)));
  }

  /**
   * Opens a cycle.
   *
   * @param programme programme
   * @param type business type
   * @param year policy year
   * @return the cycle
   */
  public EbCycle cycle(EbProgramme programme, BusinessType type, int year) {
    return as.run(
        AO,
        () ->
            cycles.open(
                company(),
                programme.getId(),
                new CycleService.OpenCycle(type, year, LocalDate.of(year, 1, 1))));
  }

  /**
   * Moves a cycle by system actions (the steps of later waves).
   *
   * @param cycle cycle
   * @param actions actions in order
   */
  public void move(EbCycle cycle, String... actions) {
    for (String action : actions) {
      as.run(
          AO,
          () ->
              workflow.systemTransition(
                  EbCodes.ENTITY_CYCLE, cycle.getId().toString(), action, TransitionNote.NONE));
    }
  }

  /**
   * A valid one-page PDF (e-mails protect their attachments, which needs a readable PDF).
   *
   * @param name file name
   * @return file
   */
  public static UploadedFile realPdf(String name) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document doc = new Document();
    PdfWriter.getInstance(doc, out);
    doc.open();
    doc.add(new Paragraph(name + " " + token()));
    doc.close();
    return new UploadedFile(name, out.toByteArray());
  }

  /**
   * A PDF file.
   *
   * @param name file name
   * @return file
   */
  public static UploadedFile pdf(String name) {
    return new UploadedFile(name, ("%PDF-1.4 " + token()).getBytes(StandardCharsets.US_ASCII));
  }

  /**
   * A motor account draft that rates (the seed catalog has no EB product yet).
   *
   * @param clientId client
   * @return draft
   */
  public static AccountDraft motorDraft(Long clientId) {
    String id = token();
    RiskItemData vehicle =
        new RiskItemData(
            null,
            new BigDecimal("1000000"),
            null,
            new BigDecimal("100000"),
            new BigDecimal("100000"),
            new Vehicle(
                "P" + id, null, "E" + id, "C" + id, "Toyota", "Vios", 2025, "SEDAN", "White", 5),
            null,
            null);
    return new AccountDraft(
        clientId,
        "MTR10",
        "CBG",
        "EMAIL",
        "INS-MGIC",
        "MKT",
        LocalDate.of(2026, 10, 1),
        LocalDate.of(2027, 10, 1),
        false,
        1,
        "PHP",
        PaymentArrangement.VIA_BDOI,
        Mortgage.NONE,
        null,
        List.of(vehicle),
        null,
        null,
        null);
  }
}
