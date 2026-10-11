package com.iortatechnxt.brokerverse.renewal.placement;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.renewal.domain.EpolicyReceipt;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.epolicy.service.EpolicyNumbersHandler;
import com.iortatechnxt.brokerverse.renewal.epolicy.service.EpolicyReceipts;
import com.iortatechnxt.brokerverse.renewal.epolicy.service.EpolicySending;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * E-policies of the renewal accounts of BDOI's FRS against the real database (FRRN.033): the
 * summary and ZIP files with their record-level results, the sending through CCM and the upload of
 * e-policy numbers with the duplicate check.
 */
@IntegrationTest
class RenewalEpolicyIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private EpolicyReceipts receipts;
  @Autowired private EpolicySending sending;
  @Autowired private EpolicyNumbersHandler numbers;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DocumentComposer composer;
  @Autowired private AsUser as;

  private RenewalCandidate submitted() {
    RenewalCandidate c = fx.unassignedRetail();
    jdbc.update(
        "update rnw_candidate set stage = 'FOR_PLACEMENT_BOOKING', client_email ="
            + " 'juan.delacruz@client.example.ph' where id = ?",
        c.getId());
    return fx.reload(c);
  }

  private byte[] zip(Map<String, String> files) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream out = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, String> f : files.entrySet()) {
        out.putNextEntry(new ZipEntry(f.getKey()));
        out.write(
            composer.pdf(
                new DocumentSpec("Insurer", "E-Policy", f.getValue(), List.of(), List.of(), null)));
        out.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  private static EpolicyReceipts.Incoming file(String name, String text) {
    return new EpolicyReceipts.Incoming(name, text.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void theEpolicyFilesUpdateTheMatchedAccountAndTheEpolicyIsSentThroughCcm() throws IOException {
    RenewalCandidate c = submitted();
    String policy = "MC-PC-" + c.getId();
    String summary =
        "Sequence Number|Reference Number|Policy Number|PDF File Name|Document Tag\n"
            + "1|"
            + c.getRenewalRef()
            + "|"
            + policy
            + "|"
            + policy
            + ".pdf|Policy\n"
            + "2|RNW-NOT-THERE|MC-PC-0|other.pdf|Policy\n";
    byte[] docs = zip(Map.of(policy + ".pdf", "%PDF-1.4 policy", "other.pdf", "%PDF-1.4 other"));
    EpolicyReceipt r =
        as.run(
            PO,
            () ->
                receipts.receive(
                    fx.company(),
                    "UPLOAD",
                    file("summary.txt", summary),
                    new EpolicyReceipts.Incoming("policies.zip", docs)));
    assertThat(r.getStatus()).isEqualTo(EpolicyReceipt.SUCCESSFUL);
    assertThat(r.getRecords()).isEqualTo(2);
    assertThat(r.getMatched()).isEqualTo(1);
    RenewalCandidate updated = fx.reload(c);
    assertThat(updated.getPlacement().getEpolicyNo()).isEqualTo(policy);
    assertThat(updated.getPlacement().getStatus())
        .isEqualTo(CandidatePlacement.FOR_EPOLICY_SENDING);
    String unmatched =
        jdbc.queryForObject(
            "select l.remarks from rnw_epolicy_line l join rnw_epolicy_receipt r on r.id = l.receipt_id"
                + " where r.receipt_no = ? and l.renewal_ref = 'RNW-NOT-THERE'",
            String.class,
            r.getReceiptNo());
    assertThat(unmatched).isEqualTo("Record not found");

    List<EpolicySending.Account> eligible = as.run(PO, () -> sending.accounts(fx.company()));
    assertThat(eligible)
        .filteredOn(a -> a.renewalRef().equals(c.getRenewalRef()))
        .singleElement()
        .matches(a -> a.problem() == null);
    EpolicySending.Summary sent =
        as.run(PO, () -> sending.send(fx.company(), List.of(c.getRenewalRef()), List.of()));
    assertThat(sent.selected()).isEqualTo(1);
    assertThat(sent.submitted() + sent.pending()).isEqualTo(1);
  }

  @Test
  void theFilesAreRefusedWithoutTheZipOrWithADocumentNotInTheSummary() throws IOException {
    RenewalCandidate c = submitted();
    EpolicyReceipt missing =
        as.run(
            PO,
            () -> receipts.receive(fx.company(), "UPLOAD", file("s.txt", "1|A|B|c.pdf|P"), null));
    assertThat(missing.getStatus()).isEqualTo(EpolicyReceipt.FAILED);
    byte[] docs = zip(Map.of("listed.pdf", "x", "stray.pdf", "y"));
    EpolicyReceipt stray =
        as.run(
            PO,
            () ->
                receipts.receive(
                    fx.company(),
                    "UPLOAD",
                    file("s.txt", "1|" + c.getRenewalRef() + "|P-1|listed.pdf|Policy"),
                    new EpolicyReceipts.Incoming("z.zip", docs)));
    assertThat(stray.getStatus()).isEqualTo(EpolicyReceipt.FAILED);
    assertThat(stray.getRemarks()).contains("stray.pdf");
  }

  @Test
  void anEpolicyNumberAlreadyHeldByAnotherActiveAccountIsRefused() {
    RenewalCandidate first = submitted();
    RenewalCandidate second = submitted();
    BulkContext ctx =
        new BulkContext(fx.company(), "EPN-" + first.getId(), LocalDate.now(), Map.of());
    String no = "EP-" + first.getId();
    BulkRow one =
        new BulkRow(2, Map.of("Reference Number", first.getRenewalRef(), "Policy Number", no));
    assertThat(numbers.validate(one, ctx)).isEmpty();
    as.run(PO, () -> numbers.process(one, ctx));
    assertThat(fx.reload(first).getPlacement().getEpolicyNo()).isEqualTo(no);
    BulkRow dup =
        new BulkRow(3, Map.of("Reference Number", second.getRenewalRef(), "Policy Number", no));
    assertThat(numbers.validate(dup, ctx)).anyMatch(e -> e.startsWith("Duplicate Policy Number"));
    BulkRow unknown =
        new BulkRow(4, Map.of("Reference Number", "RNW-NONE", "Policy Number", "X-1"));
    assertThat(numbers.validate(unknown, ctx)).contains("Reference Number does not exist");
  }
}
