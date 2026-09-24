package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.stereotype.Component;

/**
 * Password-protects outbound documents (BRNB.013/035): PDF with AES-256, Excel workbooks with
 * Office agile encryption. Other file types cannot be protected and are refused, so an unprotected
 * copy is never sent by mistake.
 */
@Component
public class DocumentProtector {

  private static final String PDF = "application/pdf";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final DocumentPasswordPolicy passwords;

  /**
   * Creates the protector.
   *
   * @param passwords owner password source for PDFs
   */
  public DocumentProtector(DocumentPasswordPolicy passwords) {
    this.passwords = passwords;
  }

  /**
   * Whether a file can be protected.
   *
   * @param file file
   * @return true for PDF and XLSX
   */
  public boolean canProtect(MessageFile file) {
    return isPdf(file) || isXlsx(file);
  }

  /**
   * Encrypts a file with a password.
   *
   * @param file file
   * @param password password needed to open it
   * @return the protected file (same name and type)
   */
  public MessageFile protect(MessageFile file, String password) {
    try {
      if (isPdf(file)) {
        return new MessageFile(file.fileName(), file.mimeType(), protectPdf(file, password));
      }
      if (isXlsx(file)) {
        return new MessageFile(file.fileName(), file.mimeType(), protectXlsx(file, password));
      }
    } catch (IOException | GeneralSecurityException | InvalidFormatException e) {
      throw new BusinessRuleException(
          "DOCUMENT_PROTECTION_FAILED",
          "Could not protect " + file.fileName() + ": " + e.getMessage(),
          e);
    }
    throw new BusinessRuleException(
        "DOCUMENT_NOT_PROTECTABLE",
        file.fileName() + " cannot be password protected (only PDF and Excel files can)");
  }

  private byte[] protectPdf(MessageFile file, String password) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (PdfReader reader = new PdfReader(file.content());
        PdfStamper stamper = new PdfStamper(reader, out)) {
      stamper.setEncryption(
          password.getBytes(StandardCharsets.UTF_8),
          passwords.newPassword().getBytes(StandardCharsets.UTF_8),
          PdfWriter.ALLOW_PRINTING,
          PdfWriter.ENCRYPTION_AES_256_V3);
    }
    return out.toByteArray();
  }

  private static byte[] protectXlsx(MessageFile file, String password)
      throws IOException, GeneralSecurityException, InvalidFormatException {
    try (POIFSFileSystem fs = new POIFSFileSystem()) {
      Encryptor encryptor = new EncryptionInfo(EncryptionMode.agile).getEncryptor();
      encryptor.confirmPassword(password);
      try (OPCPackage opc = OPCPackage.open(new ByteArrayInputStream(file.content()));
          OutputStream os = encryptor.getDataStream(fs)) {
        opc.save(os);
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      fs.writeFilesystem(out);
      return out.toByteArray();
    }
  }

  private static boolean isPdf(MessageFile file) {
    return PDF.equals(file.mimeType()) || lowerName(file).endsWith(".pdf");
  }

  private static boolean isXlsx(MessageFile file) {
    return XLSX.equals(file.mimeType()) || lowerName(file).endsWith(".xlsx");
  }

  private static String lowerName(MessageFile file) {
    return file.fileName().toLowerCase(Locale.ROOT);
  }
}
