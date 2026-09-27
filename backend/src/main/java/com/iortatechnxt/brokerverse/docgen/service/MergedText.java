package com.iortatechnxt.brokerverse.docgen.service;

/**
 * A template merged with values.
 *
 * @param code template code
 * @param versionNo version used (store it on the generated record)
 * @param title title
 * @param text merged text
 */
public record MergedText(String code, int versionNo, String title, String text) {

  /**
   * Version tag, e.g. {@code QUOTATION_TERMS v2}.
   *
   * @return tag
   */
  public String versionTag() {
    return code + " v" + versionNo;
  }
}
