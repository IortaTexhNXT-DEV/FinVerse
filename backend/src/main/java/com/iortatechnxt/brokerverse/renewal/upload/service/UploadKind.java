package com.iortatechnxt.brokerverse.renewal.upload.service;

import java.util.List;

/** The Renewal uploads with an outcome summary, their bulk uploads and Processing Result name. */
public enum UploadKind {
  /** LAMD CBG loans and paid-off lists (FRRN.012.01). */
  LAMD(List.of("RNW_LAMD_CBG_LOANS", "RNW_LAMD_PAID_OFF", "RNW_LAMD_REPORT"), "LAMD"),
  /** BDOFC and BDOSOLD reports (FRRN.012.06). */
  BDOFC(List.of("RNW_BDOFC_SOLD"), "BDOFC-SOLD"),
  /** Insurer disposition files (FRRN.013.01). */
  INSURER(List.of("RNW_INSURER_DISPOSITION", "RNW_INSURER_RESPONSE"), "Insurer Disposition"),
  /** Renewal Update files (FRRN.015.03). */
  UPDATE(List.of("RNW_RENEWAL_UPDATE", "RNW_DISPOSITION_UPLOAD"), "Renewal Update"),
  /** Hold cover response files (FRRN.037.01). */
  HOLD_COVER(List.of("RNW_HOLD_COVER_RESPONSE"), "Hold Cover Response"),
  /** Placement response files (FRRN.030.01). */
  PLACEMENT(List.of("RNW_PLACEMENT_RESPONSE"), "Placement Response"),
  /** E-policy number files (FRRN.033.05). */
  EPOLICY_NUMBERS(List.of("RNW_EPOLICY_NUMBERS"), "E-Policy Numbers");

  private final List<String> handlers;
  private final String resultPrefix;

  UploadKind(List<String> handlers, String resultPrefix) {
    this.handlers = handlers;
    this.resultPrefix = resultPrefix;
  }

  /**
   * The bulk uploads of the kind.
   *
   * @return handler codes
   */
  public List<String> handlers() {
    return handlers;
  }

  /**
   * The start of the Processing Result file name.
   *
   * @return prefix
   */
  public String resultPrefix() {
    return resultPrefix;
  }
}
