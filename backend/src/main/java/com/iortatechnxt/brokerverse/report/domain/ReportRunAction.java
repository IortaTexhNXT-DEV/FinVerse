package com.iortatechnxt.brokerverse.report.domain;

/** What an archived report run was (CSHID.017/018). */
public enum ReportRunAction {
  /** Run on screen. */
  VIEW,
  /** Exported (downloaded or printed); the file is kept. */
  EXPORT
}
