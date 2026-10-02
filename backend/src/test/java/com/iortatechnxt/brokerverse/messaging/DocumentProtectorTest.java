package com.iortatechnxt.brokerverse.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.service.DocumentProtector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/** Word (DOCX) protection of outbound files (BRID-007; plain JUnit). */
class DocumentProtectorTest {

  private static final String DOCX_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

  private final DocumentProtector protector = new DocumentProtector(() -> "owner-secret");

  private static byte[] docx(String text) throws IOException {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      doc.createParagraph().createRun().setText(text);
      doc.write(out);
      return out.toByteArray();
    }
  }

  @Test
  void aWordDocumentIsEncryptedAndOpensWithItsPassword()
      throws IOException, GeneralSecurityException {
    MessageFile tor = new MessageFile("TOR.docx", DOCX_TYPE, docx("Terms of Reference"));
    assertThat(protector.canProtect(tor)).isTrue();

    MessageFile protectedTor = protector.protect(tor, "s3cret!");

    assertThat(protectedTor.fileName()).isEqualTo("TOR.docx");
    try (POIFSFileSystem fs =
        new POIFSFileSystem(new ByteArrayInputStream(protectedTor.content()))) {
      Decryptor decryptor = Decryptor.getInstance(new EncryptionInfo(fs));
      assertThat(decryptor.verifyPassword("wrong")).isFalse();
      assertThat(decryptor.verifyPassword("s3cret!")).isTrue();
      try (InputStream data = decryptor.getDataStream(fs);
          XWPFDocument opened = new XWPFDocument(data)) {
        assertThat(opened.getParagraphs().get(0).getText()).isEqualTo("Terms of Reference");
      }
    }
  }

  @Test
  void aFileByNameIsRecognisedAndOtherTypesAreRefused() {
    assertThat(
            protector.canProtect(
                new MessageFile("x.DOCX", "application/octet-stream", new byte[0])))
        .isTrue();
    MessageFile csv =
        new MessageFile("masterlist.csv", "text/csv", "a,b".getBytes(StandardCharsets.UTF_8));
    assertThat(protector.canProtect(csv)).isFalse();
    assertThatThrownBy(() -> protector.protect(csv, "pw"))
        .extracting("code", "message")
        .containsExactly(
            "DOCUMENT_NOT_PROTECTABLE",
            "masterlist.csv cannot be password protected. Send it as PDF, Excel or Word");
  }
}
