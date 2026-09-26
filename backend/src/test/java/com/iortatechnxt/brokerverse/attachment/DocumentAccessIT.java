package com.iortatechnxt.brokerverse.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.domain.DocumentAccess;
import com.iortatechnxt.brokerverse.attachment.service.DocumentAccessPolicy;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Document access classes (BRID-025; cross-BRD work item P3, V1031) and process tags: a type with
 * access rows is listed and downloaded only by its departments; types without rows keep today's
 * behaviour; refusals are audited.
 */
@IntegrationTest
class DocumentAccessIT {

  private static final byte[] PDF = "%PDF-1.4 access".getBytes(StandardCharsets.US_ASCII);
  private static final String UTILIZATION = "EB_UTILIZATION";
  private static final String SOA = "EB_SOA";
  private static final String BILLING = "EB_DIRECT_BILLING";

  @Autowired private DocumentService documents;
  @Autowired private DocumentAccessPolicy policy;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private static String id() {
    return Long.toString(System.nanoTime() % 1_000_000_000L);
  }

  /** Runs as an EB user holding the given permissions (the EB SIT/UAT users come with wave E2). */
  private static <T> T asHolder(String user, Supplier<T> action, String... permissions) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user, null, Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private Attachment upload(AttachmentTarget target, String type, String processTag) {
    return as.run(
            "ao",
            () ->
                documents.upload(
                    target,
                    List.of(new UploadedFile(type.toLowerCase(Locale.ROOT) + ".pdf", PDF)),
                    new UploadOptions(type, false, null, null, processTag)))
        .get(0);
  }

  private List<String> types(String user, AttachmentTarget target, String... permissions) {
    return asHolder(
        user,
        () -> documents.list(target).stream().map(Attachment::getDocumentType).toList(),
        permissions);
  }

  @Test
  void documentsAreListedAndDownloadedByTheirDepartmentsOnly() {
    AttachmentTarget change = new AttachmentTarget("EbMemberChange", id());
    Attachment utilization = upload(change, UTILIZATION, "ENDORSEMENT");
    Attachment soa = upload(change, SOA, null);
    Attachment billing = upload(change, BILLING, "ENDORSEMENT");
    Attachment idf = upload(change, "IDF", null);
    assertThat(billing.getProcessTag()).isEqualTo("ENDORSEMENT");

    assertThat(types("ebao.test", change, "ATTACHMENT_VIEW", "EB_MARKET"))
        .containsExactlyInAnyOrder(UTILIZATION, BILLING, "IDF");
    assertThat(types("ebproc.test", change, "ATTACHMENT_VIEW", "EB_PROCESS"))
        .containsExactlyInAnyOrder(SOA, BILLING, "IDF");
    assertThat(types("ebcoll.test", change, "ATTACHMENT_VIEW", "EB_COLLECT"))
        .containsExactlyInAnyOrder(SOA, BILLING, "IDF");
    assertThat(types("auditor.test", change, "ATTACHMENT_VIEW", "AUDIT_VIEW"))
        .containsExactlyInAnyOrder(UTILIZATION, SOA, BILLING, "IDF");
    assertThat(as.run("ao", () -> documents.list(change)))
        .extracting(Attachment::getId)
        .containsExactly(idf.getId());
    assertThat(documents.list(change)).hasSize(4);
    assertThat(documents.documentTypesOf(change)).contains(UTILIZATION, SOA);

    assertThat(
            asHolder(
                    "ebcoll.test",
                    () -> documents.download(billing.getId()),
                    "ATTACHMENT_VIEW",
                    "EB_COLLECT")
                .content())
        .isEqualTo(PDF);
    assertThatThrownBy(
            () ->
                asHolder(
                    "ebcoll.test",
                    () -> documents.download(utilization.getId()),
                    "ATTACHMENT_VIEW",
                    "EB_COLLECT"))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                asHolder(
                    "ebcoll.test",
                    () -> documents.zip(List.of(billing.getId(), utilization.getId())),
                    "ATTACHMENT_VIEW",
                    "EB_COLLECT"))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where entity_type = 'Attachment' and entity_id = ?"
                    + " and action = 'REJECT' and username = 'ebcoll.test'",
                Long.class,
                String.valueOf(utilization.getId())))
        .isEqualTo(2L);
    assertThat(policy.mayView(soa)).isTrue();
  }

  @Test
  void linksCarryTheProcessTagAndSharedTypesHaveTheirRows() {
    AttachmentTarget cycle = new AttachmentTarget("EbCycle", id());
    AttachmentTarget account = new AttachmentTarget("Account", id());
    Attachment advice = upload(cycle, "RENEWAL_ADVICE", "RENEWAL_PLACEMENT");
    as.run("ao", () -> documents.link(advice.getId(), List.of(account), "RENEWAL_PLACEMENT"));
    assertThat(
            jdbc.queryForObject(
                "select process_tag from doc_attachment_link where attachment_id = ?",
                String.class,
                advice.getId()))
        .isEqualTo("RENEWAL_PLACEMENT");
    assertThat(types("csf.test", account, "ATTACHMENT_VIEW", "CSF_VIEW"))
        .containsExactly("RENEWAL_ADVICE");
    assertThat(types("claims.test", account, "ATTACHMENT_VIEW", "BCL_VIEW")).isEmpty();
    assertThat(policy.rows())
        .extracting(DocumentAccess::getDocumentType, DocumentAccess::getPermission)
        .contains(
            tuple("CLAIM_REPORT", "BCL_VIEW"),
            tuple("RENEWAL_ADVICE", "CSF_VIEW"),
            tuple(SOA, "EB_COLLECT"));
  }
}
