package com.iortatechnxt.brokerverse.attachment.service;

import org.springframework.stereotype.Component;

/**
 * Placeholder of the in-application scan: accepts every file. It never stands in for a scan: the
 * {@link MalwareScanPolicy} decides at start-up where files are scanned (by the file store, by a
 * real scanner bean, or not at all outside production) and refuses a production start in which this
 * placeholder would be the only protection.
 */
@Component
public class NoOpVirusScanner implements VirusScanner {

  @Override
  public ScanVerdict scan(String fileName, byte[] content) {
    return ScanVerdict.ok();
  }
}
