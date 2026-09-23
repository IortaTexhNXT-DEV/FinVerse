package com.iortatechnxt.finverse.attachment.service;

import org.springframework.stereotype.Component;

/** Default scanner: accepts every file. Replace or complement with a real scanner bean. */
@Component
public class NoOpVirusScanner implements VirusScanner {

  @Override
  public ScanVerdict scan(String fileName, byte[] content) {
    return ScanVerdict.ok();
  }
}
