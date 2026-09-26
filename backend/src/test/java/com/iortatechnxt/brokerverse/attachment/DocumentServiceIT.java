package com.iortatechnxt.brokerverse.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Document types, multi-file upload, nominated names, links and ZIP (BRNB.026/055/056). */
@IntegrationTest
class DocumentServiceIT {

  private static final byte[] PDF = "%PDF-1.4 seed".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] ODS = {'P', 'K', 3, 4, 1, 2, 3};

  @Autowired private DocumentService documents;
  @Autowired private AsUser as;

  private static String id() {
    return Long.toString(System.nanoTime() % 1_000_000_000L);
  }

  @Test
  void filesAreTypedNamedLinkedAndZipped() throws IOException {
    AttachmentTarget account = new AttachmentTarget("Account", id());
    AttachmentTarget other = new AttachmentTarget("Account", id() + "9");
    List<Attachment> saved =
        as.run(
            "ao",
            () ->
                documents.upload(
                    account,
                    List.of(
                        new UploadedFile("scan one.pdf", PDF), new UploadedFile("list.ods", ODS)),
                    new UploadOptions("IDF", true, "ARN-2026-000123", "Signed IDF")));
    assertThat(saved)
        .extracting(Attachment::getFileName)
        .containsExactly("ARN-2026-000123_IDF_1.pdf", "ARN-2026-000123_IDF_2.ods");
    assertThat(saved).allMatch(a -> "IDF".equals(a.getDocumentType()));
    List<Attachment> more =
        as.run(
            "ao",
            () ->
                documents.upload(
                    account,
                    List.of(new UploadedFile("policy.pdf", PDF)),
                    new UploadOptions("IDF", true, "ARN-2026-000123", null)));
    assertThat(more.get(0).getFileName()).isEqualTo("ARN-2026-000123_IDF_3.pdf");
    List<Attachment> kept =
        as.run(
            "ao",
            () ->
                documents.upload(
                    account,
                    List.of(new UploadedFile("original name.pdf", PDF)),
                    new UploadOptions(null, false, null, null)));
    assertThat(kept.get(0).getFileName()).isEqualTo("original name.pdf");
    assertThat(documents.documentTypesOf(account)).containsExactly("IDF");

    Long shared = saved.get(0).getId();
    as.run("ao", () -> documents.link(shared, List.of(other, account)));
    as.run("ao", () -> documents.link(shared, List.of(other)));
    assertThat(documents.list(other)).extracting(Attachment::getId).containsExactly(shared);
    assertThat(documents.documentTypesOf(other)).containsExactly("IDF");
    as.run(
        "ao",
        () -> {
          documents.remove(shared, other);
          return null;
        });
    assertThat(documents.list(other)).isEmpty();
    assertThat(documents.list(account)).hasSize(4);

    byte[] zip =
        as.run(
            "ao",
            () ->
                documents.zip(
                    List.of(
                        saved.get(0).getId(),
                        saved.get(1).getId(),
                        kept.get(0).getId(),
                        kept.get(0).getId())));
    List<String> names = new ArrayList<>();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
      for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
        names.add(e.getName());
      }
    }
    assertThat(names)
        .containsExactly(
            "ARN-2026-000123_IDF_1.pdf",
            "ARN-2026-000123_IDF_2.ods",
            "original name.pdf",
            "original name (1).pdf");

    as.run(
        "ao",
        () -> {
          documents.remove(kept.get(0).getId(), account);
          return null;
        });
    assertThat(documents.list(account)).hasSize(3);
  }

  @Test
  void uploadRulesAreEnforced() {
    AttachmentTarget account = new AttachmentTarget("Account", id());
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        documents.upload(
                            account,
                            List.of(new UploadedFile("a.pdf", PDF)),
                            new UploadOptions("NOT_A_TYPE", false, null, null))))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        documents.upload(
                            account,
                            List.of(new UploadedFile("a.pdf", PDF)),
                            new UploadOptions("IDF", true, " ", null))))
        .extracting("code")
        .isEqualTo("DOCUMENT_REFERENCE_REQUIRED");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        documents.upload(
                            account, List.of(), new UploadOptions(null, false, null, null))))
        .extracting("code")
        .isEqualTo("DOCUMENT_FILE_COUNT");
    assertThatThrownBy(() -> as.run("ao", () -> documents.zip(List.of())))
        .extracting("code")
        .isEqualTo("DOCUMENT_FILE_COUNT");
    assertThatThrownBy(
            () ->
                as.run(
                    "ao",
                    () ->
                        documents.upload(
                            account,
                            List.of(new UploadedFile("fake.xls", PDF)),
                            new UploadOptions(null, false, null, null))))
        .extracting("code")
        .isEqualTo("ATTACHMENT_CONTENT_MISMATCH");
  }
}
