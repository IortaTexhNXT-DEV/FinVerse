package com.iortatechnxt.brokerverse.issuance.service;

import java.util.Optional;

/**
 * Port: reads the text of a scanned or printed document (BRIDSP-02; OCR parked, SP SQ03 and Q24).
 * The default {@code NoOcrEngine} reads nothing, so the user enters the fields by hand; an OCR
 * adapter replaces the bean without changing the review screens.
 */
public interface OcrEngine {

  /**
   * Reads the text of a document without a text layer.
   *
   * @param content document bytes
   * @return the text, empty when the document cannot be read
   */
  Optional<String> read(byte[] content);
}
