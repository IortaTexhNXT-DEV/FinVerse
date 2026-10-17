package com.iortatechnxt.brokerverse.bulk.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * A file to upload.
 *
 * @param companyId company
 * @param handlerCode handler
 * @param fileName file name
 * @param content bytes
 * @param parameters handler parameters chosen on screen
 */
public record BulkUpload(
    Long companyId,
    String handlerCode,
    String fileName,
    byte[] content,
    Map<String, String> parameters) {

  /** Defensive copies. */
  public BulkUpload {
    content = content.clone();
    parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
  }

  @Override
  public byte[] content() {
    return content.clone();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BulkUpload u
        && Objects.equals(companyId, u.companyId)
        && handlerCode.equals(u.handlerCode)
        && fileName.equals(u.fileName)
        && Arrays.equals(content, u.content)
        && parameters.equals(u.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(companyId, handlerCode, fileName, Arrays.hashCode(content), parameters);
  }

  @Override
  public String toString() {
    return "BulkUpload[" + handlerCode + ", " + fileName + ", " + content.length + " bytes]";
  }
}
