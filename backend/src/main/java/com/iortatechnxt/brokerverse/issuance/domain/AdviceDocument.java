package com.iortatechnxt.brokerverse.issuance.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * The rendered Insurance Advice.
 *
 * @param trigger what generated it
 * @param templateVersion template version used (BRNB.004)
 * @param fileName file name
 * @param sha256 checksum
 * @param content PDF bytes
 */
public record AdviceDocument(
    AdviceTrigger trigger, String templateVersion, String fileName, String sha256, byte[] content) {

  /** Defensive copy. */
  public AdviceDocument {
    content = content.clone();
  }

  @Override
  public byte[] content() {
    return content.clone();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof AdviceDocument d
        && trigger == d.trigger
        && Objects.equals(templateVersion, d.templateVersion)
        && Objects.equals(fileName, d.fileName)
        && Objects.equals(sha256, d.sha256)
        && Arrays.equals(content, d.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trigger, templateVersion, fileName, sha256, Arrays.hashCode(content));
  }

  @Override
  public String toString() {
    return fileName + " (" + content.length + " bytes)";
  }
}
