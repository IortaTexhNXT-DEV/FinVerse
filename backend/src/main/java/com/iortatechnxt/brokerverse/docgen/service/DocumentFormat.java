package com.iortatechnxt.brokerverse.docgen.service;

/**
 * Formats of a business document (client requirement 16): every document is downloaded as PDF or
 * Word; e-mail attachments stay PDF.
 */
public enum DocumentFormat {
  /** PDF, the default and the e-mail attachment. */
  PDF("application/pdf", "pdf"),
  /** Word, the same content for editing. */
  DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

  private final String contentType;
  private final String extension;

  DocumentFormat(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  /**
   * MIME type.
   *
   * @return content type
   */
  public String contentType() {
    return contentType;
  }

  /**
   * File extension without the dot.
   *
   * @return extension
   */
  public String extension() {
    return extension;
  }
}
