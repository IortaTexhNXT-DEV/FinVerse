package com.iortatechnxt.brokerverse.attachment;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.delete;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditLogRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AttachmentIT {

  private static final byte[] PDF =
      "%PDF-1.4\n1 0 obj << >> endobj\ntrailer\n%%EOF".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

  @Autowired private AttachmentService service;
  @Autowired private AuditLogRepository audit;
  @Autowired private AsUser as;
  @Autowired private MockMvc mvc;

  private static String sha256(byte[] content) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
  }

  @Test
  void uploadListDownloadAndDeleteWithChecksumAndAudit() throws Exception {
    AttachmentTarget target = new AttachmentTarget("JournalBatch", "ATT-1");
    Attachment saved =
        as.run("accountant", () -> service.upload(target, "C:\\docs\\invoice.pdf", PDF, "Invoice"));
    assertThat(saved.getFileName()).isEqualTo("invoice.pdf");
    assertThat(saved.getContentType()).isEqualTo("application/pdf");
    assertThat(saved.getSizeBytes()).isEqualTo(PDF.length);
    assertThat(saved.getSha256()).isEqualTo(sha256(PDF));
    assertThat(service.list(target)).extracting(Attachment::getId).containsExactly(saved.getId());

    var file = as.run("checker", () -> service.download(saved.getId()));
    assertThat(file.content()).isEqualTo(PDF);

    as.run(
        "accountant",
        () -> {
          service.delete(saved.getId());
          return null;
        });
    assertThat(service.list(target)).isEmpty();
    assertThatThrownBy(() -> service.get(saved.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
    var trail =
        audit.search(
            null,
            "Attachment",
            saved.getId().toString(),
            Instant.EPOCH,
            Instant.now().plusSeconds(60),
            PageRequest.of(0, 10));
    assertThat(trail.getContent())
        .extracting(a -> a.getAction().name())
        .contains("CREATE", "EXPORT", "DEACTIVATE");
  }

  @Test
  void rejectsDisallowedTypesMismatchedContentAndOversizedFiles() {
    AttachmentTarget target = new AttachmentTarget("JournalBatch", "ATT-2");
    assertThatThrownBy(() -> service.upload(target, "virus.exe", PDF, null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("pdf");
    assertThatThrownBy(() -> service.upload(target, "scan.pdf", PNG, null))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("ATTACHMENT_CONTENT_MISMATCH");
    assertThatThrownBy(() -> service.upload(target, "empty.pdf", new byte[0], null))
        .extracting("code")
        .isEqualTo("ATTACHMENT_EMPTY");
    assertThatThrownBy(() -> service.requireWithinLimit(10L * 1024 * 1024 + 1))
        .extracting("code")
        .isEqualTo("ATTACHMENT_TOO_LARGE");
    assertThatThrownBy(
            () -> service.upload(new AttachmentTarget("Bad Type!", "1"), "a.pdf", PDF, null))
        .extracting("code")
        .isEqualTo("INVALID_ATTACHMENT_TARGET");
    assertThat(service.maxSizeBytes()).isEqualTo(10L * 1024 * 1024);
  }

  @Test
  @WithUserDetails("accountant")
  void httpUploadAndDownload() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG);
    String body =
        mvc.perform(
                multipart("/api/v1/attachments")
                    .file(file)
                    .param("entityType", "JournalBatch")
                    .param("entityId", "ATT-3")
                    .param("description", "Logo"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contentType").value("image/png"))
            .andExpect(jsonPath("$.sha256").value(sha256(PNG)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
    mvc.perform(get("/api/v1/attachments/" + id + "/content"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "attachment; filename=\"logo.png\"; filename*=UTF-8''logo.png"));
    mvc.perform(get("/api/v1/attachments?entityType=JournalBatch&entityId=ATT-3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fileName").value("logo.png"));
    mvc.perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "notes.txt", "text/plain", PDF))
                .param("entityType", "JournalBatch")
                .param("entityId", "ATT-3"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ATTACHMENT_TYPE_NOT_ALLOWED"));
    mvc.perform(delete("/api/v1/attachments/" + id)).andExpect(status().isNoContent());
  }

  @Test
  @WithUserDetails("auditor")
  void viewersCannotUpload() throws Exception {
    mvc.perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "a.pdf", "application/pdf", PDF))
                .param("entityType", "JournalBatch")
                .param("entityId", "ATT-4"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/attachments/policy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maxSizeBytes").value(10 * 1024 * 1024));
  }
}
