package com.iortatechnxt.brokerverse.report.render;

/**
 * Supported export formats: PDF (print), Excel, CSV and, for BRNB.037, OpenDocument spreadsheet
 * (ODS) and XML. Word (DOCX) is offered for reports that are documents or schedules ({@link
 * com.iortatechnxt.brokerverse.report.core.ReportMetadata#documentStyle()}, client requirement 16).
 */
public enum ExportFormat {
  PDF("application/pdf", "pdf"),
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
  CSV("text/csv", "csv"),
  ODS("application/vnd.oasis.opendocument.spreadsheet", "ods"),
  XML("application/xml", "xml"),
  DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

  private final String contentType;
  private final String extension;

  ExportFormat(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  public String contentType() {
    return contentType;
  }

  public String extension() {
    return extension;
  }
}
