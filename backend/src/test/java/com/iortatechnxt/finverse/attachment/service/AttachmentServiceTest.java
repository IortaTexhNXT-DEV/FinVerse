package com.iortatechnxt.finverse.attachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.finverse.attachment.domain.AllowedFileType;
import com.iortatechnxt.finverse.attachment.domain.AttachmentContent;
import com.iortatechnxt.finverse.attachment.domain.AttachmentContentRepository;
import com.iortatechnxt.finverse.attachment.domain.AttachmentRepository;
import com.iortatechnxt.finverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.finverse.attachment.service.VirusScanner.ScanVerdict;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class AttachmentServiceTest {

  private final AttachmentRepository attachments = mock(AttachmentRepository.class);
  private final AttachmentContentRepository contents = mock(AttachmentContentRepository.class);

  private AttachmentService service(VirusScanner scanner, DataSize max) {
    return new AttachmentService(
        attachments,
        contents,
        List.of(scanner),
        new AttachmentProperties(max),
        mock(AuditTrailService.class),
        new CurrentUser(),
        Clock.systemUTC());
  }

  @Test
  void infectedFilesAreRejectedBeforeStorage() {
    VirusScanner scanner = (name, content) -> new ScanVerdict(false, "EICAR-Test-File");
    AttachmentService service = service(scanner, null);
    byte[] csv = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(
            () -> service.upload(new AttachmentTarget("Party", "C-1"), "data.csv", csv, null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("EICAR");
    verify(attachments, never()).save(any());
  }

  @Test
  void configuredLimitApplies() {
    AttachmentService service = service(new NoOpVirusScanner(), DataSize.ofBytes(4));
    assertThatThrownBy(
            () ->
                service.upload(
                    new AttachmentTarget("Party", "C-1"),
                    "data.csv",
                    "12345".getBytes(StandardCharsets.UTF_8),
                    null))
        .extracting("code")
        .isEqualTo("ATTACHMENT_TOO_LARGE");
  }

  @Test
  void tamperedContentFailsIntegrityCheck() {
    AttachmentService service = service(new NoOpVirusScanner(), null);
    var target = new AttachmentTarget("Party", "C-1");
    byte[] csv = "x\n".getBytes(StandardCharsets.UTF_8);
    when(attachments.save(any())).thenAnswer(i -> i.getArgument(0));
    var saved = service.upload(target, "x.csv", csv, " ");
    assertThat(saved.getDescription()).isNull();
    when(attachments.findById(7L)).thenReturn(Optional.of(saved));
    when(contents.findById(7L))
        .thenReturn(
            Optional.of(new AttachmentContent(7L, "tampered".getBytes(StandardCharsets.UTF_8))));
    assertThatThrownBy(() -> service.download(7L))
        .extracting("code")
        .isEqualTo("ATTACHMENT_INTEGRITY_FAILURE");
  }

  @Test
  void fileNamesAreSanitized() {
    assertThat(AttachmentService.sanitize("../../etc/passwd.pdf")).isEqualTo("passwd.pdf");
    assertThat(AttachmentService.sanitize("a\"b<c>.pdf")).isEqualTo("a_b_c_.pdf");
    assertThat(AttachmentService.sanitize(null)).isEqualTo("file");
    assertThat(AttachmentService.sanitize(".pdf")).isEqualTo("file.pdf");
    assertThat(AttachmentService.sanitize("x".repeat(300) + ".pdf")).hasSize(255).endsWith(".pdf");
    assertThat(AttachmentService.sha256(new byte[0]))
        .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  void fileTypesAreDetectedByExtensionAndSignature() {
    assertThat(AllowedFileType.fromFileName("A.JPEG")).contains(AllowedFileType.JPEG);
    assertThat(AllowedFileType.fromFileName("noextension")).isEmpty();
    assertThat(AllowedFileType.fromFileName("book.xlsx")).contains(AllowedFileType.XLSX);
    assertThat(AllowedFileType.DOCX.matches(new byte[] {'P', 'K', 3, 4, 0})).isTrue();
    assertThat(AllowedFileType.XLSX.matches(new byte[] {'P'})).isFalse();
    assertThat(AllowedFileType.JPEG.matches(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
        .isTrue();
    assertThat(AllowedFileType.CSV.matches(new byte[] {'a', 0, 'b'})).isFalse();
    assertThat(AllowedFileType.allowedExtensions()).contains("pdf", "docx", "csv");
  }
}
