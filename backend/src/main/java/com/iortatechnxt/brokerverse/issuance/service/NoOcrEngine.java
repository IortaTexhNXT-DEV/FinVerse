package com.iortatechnxt.brokerverse.issuance.service;

import java.util.Optional;

/** Default {@link OcrEngine} while no OCR is connected: nothing is read (manual entry). */
public class NoOcrEngine implements OcrEngine {

  @Override
  public Optional<String> read(byte[] content) {
    return Optional.empty();
  }
}
