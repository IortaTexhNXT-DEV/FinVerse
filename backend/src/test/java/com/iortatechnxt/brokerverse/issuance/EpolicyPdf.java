package com.iortatechnxt.brokerverse.issuance;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;

/** Builds e-policy PDFs with a text layer for the extraction tests. */
final class EpolicyPdf {

  private EpolicyPdf() {}

  static byte[] of(String... lines) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document()) {
      PdfWriter.getInstance(doc, out);
      doc.open();
      for (String line : lines) {
        doc.add(new Paragraph(line));
      }
    }
    return out.toByteArray();
  }

  static byte[] policy(String arn, String... policyNumbers) {
    String[] lines = new String[policyNumbers.length + 3];
    lines[0] = "MABUHAY GENERAL INSURANCE CORP. - E-POLICY";
    lines[1] = "Account Reference Number: " + arn;
    lines[2] = "Period of insurance: 2026-10-01 to 2027-10-01    Total premium: PHP 12,345.67";
    for (int i = 0; i < policyNumbers.length; i++) {
      lines[i + 3] = "Policy No: " + policyNumbers[i];
    }
    return of(lines);
  }
}
