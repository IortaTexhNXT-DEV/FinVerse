package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hmo;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.eb.bor.service.BorService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentQuery;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentQuery.DocumentView;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbBorStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeViewService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The EB document register and the Broker on Record (FR-EB-002, 003, 030, 031; wave E1-B): versions
 * per cycle and type, the process tag and transaction rules, department access classes, and the BOR
 * upload, validation, rejection and re-upload.
 */
@IntegrationTest
class EbDocumentsBorIT {

  @Autowired private EbFixtures fx;
  @Autowired private EbDocumentService documents;
  @Autowired private EbDocumentQuery query;
  @Autowired private BorService bor;
  @Autowired private ProgrammeViewService views;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private EbCycle cycle(EbProgramme p) {
    return fx.cycle(p, BusinessType.NEW_BUSINESS, 2030);
  }

  private List<?> upload(EbCycle cycle, String type, String process, UploadedFile... files) {
    return as.run(
        AO,
        () ->
            documents.upload(
                fx.company(),
                cycle.getId(),
                new EbDocumentService.Upload(
                    type, process, EbDocumentSource.CLIENT, "From HR", List.of(files))));
  }

  @Test
  void aNewUploadOfATypeSupersedesTheActiveVersion() {
    EbProgramme p = fx.programme(false, List.of(hmo(null, null)));
    EbCycle cycle = cycle(p);
    upload(cycle, EbDocumentTypes.MASTERLIST, EbDocumentTypes.NB_PLACEMENT, pdf("list-1.pdf"));
    upload(
        cycle,
        EbDocumentTypes.MASTERLIST,
        EbDocumentTypes.NB_PLACEMENT,
        pdf("list-2a.pdf"),
        pdf("list-2b.pdf"));
    List<DocumentView> listed = as.run(AO, () -> query.list(fx.company(), p.getId()));
    assertThat(listed).hasSize(3);
    assertThat(listed)
        .filteredOn(d -> d.versionNo() == 2)
        .extracting(DocumentView::status)
        .containsOnly("ACTIVE");
    assertThat(listed)
        .filteredOn(d -> d.versionNo() == 1)
        .extracting(DocumentView::status)
        .containsExactly("SUPERSEDED");
    DocumentView first = listed.get(0);
    assertThat(first.documentTypeLabel()).isEqualTo("Master list (named)");
    assertThat(first.processLabel()).isEqualTo("New business placement");
    assertThat(first.source()).isEqualTo("CLIENT");
    assertThat(first.cycleNo()).isEqualTo(cycle.getCycleNo());
    assertThat(
            jdbc.queryForObject(
                "select process_tag from doc_attachment_link where attachment_id = ?"
                    + " and entity_type = 'Client'",
                String.class,
                first.attachmentId()))
        .isEqualTo(EbDocumentTypes.NB_PLACEMENT);
  }

  @Test
  void uploadsNeedTheirProcessTypeAndCycleAndRespectAccessClasses() {
    EbProgramme p = fx.programme(false, List.of(hmo(null, null)));
    EbCycle cycle = cycle(p);
    assertThatThrownBy(() -> upload(cycle, EbDocumentTypes.TOR, " ", pdf("tor.pdf")))
        .extracting("code")
        .isEqualTo("EB_PROCESS_REQUIRED");
    assertThatThrownBy(() -> upload(cycle, null, EbDocumentTypes.PROPOSAL_PROCESS, pdf("tor.pdf")))
        .extracting("code")
        .isEqualTo("EB_DOCUMENT_TYPE_REQUIRED");
    assertThatThrownBy(
            () -> upload(cycle, "KYC_ID", EbDocumentTypes.PROPOSAL_PROCESS, pdf("a.pdf")))
        .extracting("code")
        .isEqualTo("EB_DOCUMENT_TYPE_INVALID");
    assertThatThrownBy(
            () -> upload(cycle, EbDocumentTypes.BOR, EbDocumentTypes.NB_PLACEMENT, pdf("b.pdf")))
        .extracting("code")
        .isEqualTo("EB_BOR_ON_BOR_TAB");
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        documents.upload(
                            fx.company(),
                            null,
                            new EbDocumentService.Upload(
                                EbDocumentTypes.TOR,
                                "PROPOSAL",
                                null,
                                null,
                                List.of(pdf("t.pdf"))))))
        .extracting("code")
        .isEqualTo("EB_DOCUMENT_TRANSACTION_REQUIRED");
    assertThatThrownBy(
            () ->
                upload(
                    cycle,
                    EbDocumentTypes.TOR,
                    EbDocumentTypes.PROPOSAL_PROCESS,
                    new UploadedFile("tor.exe", "MZ".getBytes(StandardCharsets.US_ASCII))))
        .extracting("code")
        .isEqualTo("ATTACHMENT_TYPE_NOT_ALLOWED");

    upload(cycle, EbDocumentTypes.UTILIZATION, EbDocumentTypes.PROPOSAL_PROCESS, pdf("util.pdf"));
    upload(cycle, EbDocumentTypes.DIRECT_BILLING, EbDocumentTypes.ENDORSEMENT, pdf("billing.pdf"));
    assertThat(as.run(AO, () -> query.list(fx.company(), p.getId())))
        .extracting(DocumentView::documentType)
        .contains(EbDocumentTypes.UTILIZATION, EbDocumentTypes.DIRECT_BILLING);
    assertThat(as.run("ebcoll", () -> query.list(fx.company(), p.getId())))
        .extracting(DocumentView::documentType)
        .containsExactly(EbDocumentTypes.DIRECT_BILLING);
  }

  @Test
  void theBorIsUploadedRejectedReuploadedAndValidated() {
    EbProgramme p = fx.programme(false, List.of(hmo(null, null)));
    EbCycle cycle = cycle(p);
    Long company = fx.company();
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        bor.upload(
                            company,
                            cycle.getId(),
                            new UploadedFile(
                                "bor.csv", "a,b".getBytes(StandardCharsets.US_ASCII)))))
        .extracting("code")
        .isEqualTo("ATTACHMENT_TYPE_NOT_ALLOWED");
    EbBor first = as.run(AO, () -> bor.upload(company, cycle.getId(), pdf("bor.pdf")));
    assertThat(first.getVersionNo()).isEqualTo(1);
    assertThat(first.getStatus()).isEqualTo(EbBorStatus.UPLOADED);
    assertThatThrownBy(() -> as.run(AO, () -> bor.upload(company, cycle.getId(), pdf("bor2.pdf"))))
        .extracting("code")
        .isEqualTo("EB_BOR_PENDING");
    assertThatThrownBy(() -> as.run("ebproc", () -> bor.reject(company, first.getId(), " ")))
        .extracting("code")
        .isEqualTo("EB_BOR_REASON_REQUIRED");
    as.run("ebproc", () -> bor.reject(company, first.getId(), "Not signed"));
    assertThatThrownBy(
            () ->
                as.run(
                    "ebproc",
                    () ->
                        bor.validate(
                            company,
                            first.getId(),
                            new EbBor.Checklist(
                                true,
                                true,
                                true,
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2027, 1, 1)))))
        .extracting("code")
        .isEqualTo("EB_BOR_ALREADY_DECIDED");

    EbBor second = as.run(AO, () -> bor.upload(company, cycle.getId(), pdf("bor-signed.pdf")));
    assertThat(second.getVersionNo()).isEqualTo(2);
    assertThatThrownBy(
            () ->
                as.run(
                    "ebproc",
                    () ->
                        bor.validate(
                            company,
                            second.getId(),
                            new EbBor.Checklist(
                                true,
                                false,
                                true,
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2027, 1, 1)))))
        .extracting("code")
        .isEqualTo("EB_BOR_CHECKLIST_INCOMPLETE");
    assertThatThrownBy(
            () ->
                as.run(
                    "ebproc",
                    () ->
                        bor.validate(
                            company,
                            second.getId(),
                            new EbBor.Checklist(
                                true,
                                true,
                                true,
                                LocalDate.of(2027, 1, 1),
                                LocalDate.of(2026, 1, 1)))))
        .extracting("code")
        .isEqualTo("EB_BOR_VALIDITY_INVALID");
    EbBor validated =
        as.run(
            "ebproc",
            () ->
                bor.validate(
                    company,
                    second.getId(),
                    new EbBor.Checklist(
                        true, true, true, LocalDate.of(2026, 1, 1), LocalDate.of(2099, 1, 1))));
    assertThat(validated.getStatus()).isEqualTo(EbBorStatus.VALIDATED);
    assertThat(validated.getDecidedBy()).isEqualTo("ebproc");
    assertThat(bor.ofProgramme(company, p.getId()))
        .extracting(EbBor::getStatus)
        .containsExactly(EbBorStatus.VALIDATED, EbBorStatus.REJECTED);
    assertThat(views.view(company, p.getId()).cycles().get(0).borStatus()).isEqualTo("VALIDATED");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = ? and entity_id = ?",
                Long.class,
                AO,
                cycle.getId().toString()))
        .isEqualTo(2L);
    assertThat(
            jdbc.queryForObject(
                "select status from eb_document where attachment_id = ?",
                String.class,
                first.getAttachmentId()))
        .isEqualTo("REJECTED");
  }
}
