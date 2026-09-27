package com.iortatechnxt.brokerverse.issuance.service;

/**
 * What to extract (BRIDSP-02; SUBMITTED_POLICIES_DESIGN section 9, issuance row): the document kind
 * selects the patterns and the field set.
 *
 * @param kind document kind ({@code EPOLICY} or {@code SUBMITTED_POLICY})
 * @param content document bytes (PDF)
 * @param insurerCode insurer whose patterns are tried first, may be null
 */
public record ExtractionRequest(String kind, byte[] content, String insurerCode) {

  /** Defensive copy of the content. */
  public ExtractionRequest {
    content = content == null ? new byte[0] : content.clone();
  }

  /**
   * The document bytes.
   *
   * @return a copy of the content
   */
  @Override
  public byte[] content() {
    return content.clone();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExtractionRequest r
        && java.util.Objects.equals(kind, r.kind)
        && java.util.Arrays.equals(content, r.content)
        && java.util.Objects.equals(insurerCode, r.insurerCode);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(kind, java.util.Arrays.hashCode(content), insurerCode);
  }

  @Override
  public String toString() {
    return "ExtractionRequest[kind=" + kind + ", bytes=" + content.length + "]";
  }
}
