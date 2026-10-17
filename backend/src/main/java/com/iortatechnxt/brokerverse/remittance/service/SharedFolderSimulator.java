package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.remittance.service.port.SharedFolderGateway;
import org.springframework.stereotype.Component;

/**
 * The shared-drive simulator of SIT and UAT (IF-RMT-04): it accepts a file with content and gives
 * back the path it would be written to; a folder ending with {@code .invalid} is answered as
 * unreachable, so the failure path (alert, re-send) can be walked through. It keeps no copy.
 */
@Component
public class SharedFolderSimulator implements SharedFolderGateway {

  private static final String UNREACHABLE = ".invalid";

  @Override
  public boolean live() {
    return false;
  }

  @Override
  public Copy copy(String folder, String fileName, byte[] content) {
    if (folder.endsWith(UNREACHABLE)) {
      return new Copy(false, "Shared folder " + folder + " cannot be reached");
    }
    if (content == null || content.length == 0) {
      return new Copy(false, "The file " + fileName + " is empty");
    }
    return new Copy(true, folder + "/" + fileName);
  }
}
