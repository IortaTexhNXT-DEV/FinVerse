package com.iortatechnxt.finverse.report.render;

/** Supported export formats. */
public enum ExportFormat {
  PDF("application/pdf", "pdf"),
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
  CSV("text/csv", "csv");

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
